package com.aireview.ui;

import com.aireview.engine.MockReviewServiceImpl;
import com.aireview.engine.ReviewService;
import com.aireview.model.IssueModel;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 编辑器右键菜单"智能代码评审"入口。
 * 当前阶段：调用 MockReviewServiceImpl 提供结果；盛雅梅的 ReviewServiceImpl 就绪后改一行即可接入。
 */
public class ReviewAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(ReviewAction.class);

    /** 工具窗口 ID，必须与 plugin.xml 中注册的一致。 */
    private static final String TOOL_WINDOW_ID = "代码评审";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        if (project == null || file == null) {
            return;
        }

        // TODO 接入盛雅梅的真实引擎后，把 MockReviewServiceImpl 换成她的 ReviewServiceImpl 即可
        ReviewService service = new MockReviewServiceImpl();
        List<IssueModel> issues = service.review(file);

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow == null) {
            Messages.showErrorDialog("未找到评审结果窗口", "智能代码评审");
            return;
        }
        toolWindow.show();

        // 工具窗口首次打开时内容是异步创建的，延迟到事件队列末尾再填充数据
        ApplicationManager.getApplication().invokeLater(() -> applyIssues(toolWindow, issues));

        LOG.info("触发评审: " + file.getName() + "，模拟结果 " + issues.size() + " 条");
    }

    /** 在工具窗口里找到评审面板并填充数据（不按索引取内容，避免索引越界异常）。 */
    private static void applyIssues(ToolWindow toolWindow, List<IssueModel> issues) {
        for (Content content : toolWindow.getContentManager().getContents()) {
            if (content.getComponent() instanceof ReviewPanel panel) {
                panel.updateIssues(issues);
                return;
            }
        }
        LOG.warn("未找到评审结果面板，数据未填充");
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 仅在打开了 Java 文件时显示菜单项
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabledAndVisible(file instanceof PsiJavaFile);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // 声明 update() 在后台线程执行：新平台推荐做法，避免弃用警告
        return ActionUpdateThread.BGT;
    }
}
