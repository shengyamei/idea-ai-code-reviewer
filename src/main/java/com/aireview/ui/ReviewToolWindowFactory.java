package com.aireview.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;

/**
 * 右侧"代码评审"工具窗口：展示评审结果列表。
 * 当前阶段用假数据验证展示效果；后续由 ReviewAction 触发的真实结果填充。
 */
public class ReviewToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 列顺序与 IssueModel 展示需求一致：来源 / 级别 / 行号 / 描述
        String[] columns = {"来源", "级别", "行号", "问题描述"};
        Object[][] rows = {
                {"RULE", "MAJOR", "12", "空 catch 块：异常被吞掉"},
                {"LLM", "CRITICAL", "27", "@Transactional 方法内异常被捕获，事务不会回滚"},
                {"RULE", "MINOR", "45", "局部变量命名不规范"},
        };

        DefaultTableModel model = new DefaultTableModel(rows, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);

        Content content = ContentFactory.getInstance().createContent(panel, "评审结果", false);
        toolWindow.getContentManager().addContent(content);
    }
}
