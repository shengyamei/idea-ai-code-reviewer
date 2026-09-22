package com.aireview.ui;

import com.aireview.analysis.TransactionalAnalyzer;
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
 * 调用真实融合层 ReviewService：框架规则分析（TransactionalAnalyzer）+ PMD + LLM 汇总。
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

        // 真实融合层：注入框架分析器（PMD/LLM 在 ReviewService 内部，暂为占位）
        ReviewService service = new ReviewService(new TransactionalAnalyzer());
        List<IssueModel> issues = service.review(file);

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow == null) {
            Messages.showErrorDialog("未找到评审结果窗口", "智能代码评审");
            return;
        }
        toolWindow.show();

        // 工具窗口首次打开时内容是异步创建的，延迟到事件队列末尾再填充数据
        ApplicationManager.getApplication().invokeLater(() -> applyIssues(toolWindow, issues));

        LOG.info("触发评审: " + file.getName() + "，结果 " + issues.size() + " 条");
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
