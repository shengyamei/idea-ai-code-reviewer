package com.aireview.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * 右侧"代码评审"工具窗口：装载 ReviewPanel。
 * 面板初始为空，由 ReviewAction 触发评审后填充数据。
 */
public class ReviewToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ReviewPanel panel = new ReviewPanel(project);
        Content content = ContentFactory.getInstance().createContent(panel, "评审结果", false);
        toolWindow.getContentManager().addContent(content);
    }
}
