package com.aireview.ui;

import com.aireview.model.IssueModel;
import com.aireview.model.Severity;
import com.aireview.model.Source;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 编辑器右键菜单"智能代码评审"入口。
 * 当前阶段：打开并填充结果面板（模拟数据）；接入 ReviewService 后替换 buildMockIssues。
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

        // TODO 接入盛雅梅的引擎后，这一行替换为：reviewService.review(file)
        List<IssueModel> issues = buildMockIssues(file);

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

    /** 生成模拟评审结果（行号 1/10/20，便于验证点击跳转）；引擎接入后整体删除。 */
    private static List<IssueModel> buildMockIssues(PsiFile file) {
        String path = file.getVirtualFile() != null ? file.getVirtualFile().getPath() : "";

        List<IssueModel> list = new ArrayList<>();
        list.add(mock(path, Source.RULE, "EmptyCatchBlock", Severity.MAJOR,
                1, "空 catch 块：异常被吞掉"));
        // 事务这条配一个"可修复"补丁（演示一键修复）
        IssueModel fixable = mock(path, Source.LLM, null, Severity.CRITICAL,
                10, "@Transactional 方法内异常被捕获，事务不会回滚");
        String patch = buildSafePatch(file, fixable.getStartLine());
        fixable.setFixable(patch != null);
        fixable.setFixPatch(patch);
        list.add(fixable);
        list.add(mock(path, Source.RULE, "VariableNaming", Severity.MINOR,
                20, "变量命名不规范"));
        return list;
    }

    /**
     * 构造演示用补丁：保留目标行原内容、行尾追加一条修复注释（不破坏代码结构）。
     * 拿不到文件内容时返回 null，该项会提示"暂无可用修复"，避免误伤代码。
     */
    private static String buildSafePatch(PsiFile file, int line) {
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null || document.getLineCount() == 0) {
            return null;
        }
        int safeLine = Math.min(Math.max(line, 1), document.getLineCount());
        String original = document.getText(new TextRange(
                document.getLineStartOffset(safeLine - 1),
                document.getLineEndOffset(safeLine - 1)));
        return original + "  // [智能修复] 不要在 @Transactional 方法内捕获异常，应向上抛出以触发事务回滚";
    }

    private static IssueModel mock(String path, Source source, String ruleId,
                                   Severity severity, int line, String message) {
        IssueModel m = new IssueModel();
        m.setId(UUID.randomUUID().toString());
        m.setSource(source);
        m.setRuleId(ruleId);
        m.setSeverity(severity);
        m.setFilePath(path);
        m.setStartLine(line);
        m.setEndLine(line);
        m.setMessage(message);
        m.setEvidence("模拟数据（引擎就绪后由 ReviewService 提供）");
        return m;
    }
}
