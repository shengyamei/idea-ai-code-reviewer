package com.aireview.ui;

import com.aireview.model.IssueModel;
import com.aireview.model.Severity;
import com.aireview.model.Source;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 评审结果面板：展示 IssueModel 列表，双击某行跳转到对应代码行。
 */
public class ReviewPanel extends JPanel {

    private static final Logger LOG = Logger.getInstance(ReviewPanel.class);

    private static final String[] COLUMNS = {"来源", "级别", "行号", "问题描述"};

    private final Project project;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<IssueModel> issues = new ArrayList<>();

    public ReviewPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;

        this.tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        this.table = new JTable(tableModel);
        // 最后一列（问题描述）吃掉剩余宽度，各列不再平均分配
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(420);
        // 问题描述列自动换行
        table.getColumnModel().getColumn(3).setCellRenderer(new WrapCellRenderer());

        // 双击跳转（单击容易误触）
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    navigateTo(table.getSelectedRow());
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        // 底部操作按钮区
        JButton jumpButton = new JButton("跳转到代码");
        jumpButton.addActionListener(e -> navigateTo(table.getSelectedRow()));

        JButton fixButton = new JButton("应用修复");
        fixButton.addActionListener(e -> fixSelected(table.getSelectedRow()));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.add(jumpButton);
        buttons.add(fixButton);
        add(buttons, BorderLayout.SOUTH);
    }

    /** 用最新评审结果替换表格内容。 */
    public void updateIssues(@NotNull List<IssueModel> newIssues) {
        issues.clear();
        issues.addAll(newIssues);
        tableModel.setRowCount(0);
        for (IssueModel issue : newIssues) {
            tableModel.addRow(new Object[]{
                    sourceText(issue.getSource()),
                    severityText(issue.getSeverity()),
                    issue.getStartLine(),
                    issue.getMessage()
            });
        }
        adjustRowHeights();
        // 首次填充时表格可能尚未完成布局（列宽不可靠），延后一帧按最终列宽再校正一次行高
        SwingUtilities.invokeLater(this::adjustRowHeights);
    }

    /** 来源显示为中文（演示更直观）。 */
    private static String sourceText(Source source) {
        if (source == null) {
            return "";
        }
        return switch (source) {
            case RULE -> "规则引擎";
            case LLM -> "大模型";
        };
    }

    /** 级别显示为中文（演示更直观）。 */
    private static String severityText(Severity severity) {
        if (severity == null) {
            return "";
        }
        return switch (severity) {
            case CRITICAL -> "严重";
            case MAJOR -> "警告";
            case MINOR -> "建议";
            case INFO -> "提示";
        };
    }

    /** 按内容自动调整每行高度（配合描述列换行）。 */
    private void adjustRowHeights() {
        table.setRowHeight(22);
        for (int row = 0; row < table.getRowCount(); row++) {
            int height = table.getRowHeight();
            for (int column = 0; column < table.getColumnCount(); column++) {
                Component comp = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
                // 先按当前列宽设置组件尺寸：JTextArea 只有拿到实际宽度才能算出换行后的真实高度
                comp.setSize(table.getColumnModel().getColumn(column).getWidth(), Short.MAX_VALUE);
                height = Math.max(height, comp.getPreferredSize().height + 6);
            }
            table.setRowHeight(row, height);
        }
    }

    /** 对选中的问题执行一键修复（Diff 预览 → 确认 → 应用）。 */
    private void fixSelected(int row) {
        if (row < 0 || row >= issues.size()) {
            Messages.showInfoMessage(project, "请先在列表里选中一条问题", "智能代码评审");
            return;
        }
        FixApplier.previewAndApply(project, issues.get(row));
    }

    /** 跳转核心：把某一行的缺陷定位到编辑器对应位置并高亮。 */
    private void navigateTo(int row) {
        if (row < 0 || row >= issues.size()) {
            return;
        }
        IssueModel issue = issues.get(row);
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(issue.getFilePath());
        if (file == null) {
            LOG.warn("找不到文件: " + issue.getFilePath());
            return;
        }
        // 行号严格限制在 [1, 文档实际行数] 内，避免超范围导致跳转异常
        // 新版 IDEA 平台要求文档模型访问必须处于读动作内（本方法由 EDT 的双击/按钮触发，同样适用）
        Integer maxLine = ReadAction.compute(() -> {
            Document document = FileDocumentManager.getInstance().getDocument(file);
            return document != null ? document.getLineCount() : Integer.MAX_VALUE;
        });
        int line = Math.min(Math.max(issue.getStartLine(), 1), maxLine);
        // OpenFileDescriptor 行号从 0 开始，IssueModel 行号从 1 开始，这里减 1
        new OpenFileDescriptor(project, file, line - 1, 0).navigate(true);
    }

    /** 描述列换行渲染器：让长文本自动折行显示。 */
    private static class WrapCellRenderer extends JTextArea implements TableCellRenderer {
        WrapCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            setText(value != null ? value.toString() : "");
            setFont(table.getFont());
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            return this;
        }
    }
}
