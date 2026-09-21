package com.aireview.ui;

import com.aireview.model.IssueModel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
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

        JTable table = new JTable(tableModel);
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
    }

    /** 用最新评审结果替换表格内容。 */
    public void updateIssues(@NotNull List<IssueModel> newIssues) {
        issues.clear();
        issues.addAll(newIssues);
        tableModel.setRowCount(0);
        for (IssueModel issue : newIssues) {
            tableModel.addRow(new Object[]{
                    issue.getSource(),
                    issue.getSeverity(),
                    issue.getStartLine(),
                    issue.getMessage()
            });
        }
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
        Document document = FileDocumentManager.getInstance().getDocument(file);
        int maxLine = document != null ? document.getLineCount() : Integer.MAX_VALUE;
        int line = Math.min(Math.max(issue.getStartLine(), 1), maxLine);
        // OpenFileDescriptor 行号从 0 开始，IssueModel 行号从 1 开始，这里减 1
        new OpenFileDescriptor(project, file, line - 1, 0).navigate(true);
    }
}
