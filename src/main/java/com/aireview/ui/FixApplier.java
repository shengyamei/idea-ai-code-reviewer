package com.aireview.ui;

import com.aireview.model.IssueModel;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * 一键修复：Diff 预览 → 确认 → 把 fixPatch 写入文件。
 * fixPatch 约定：替换 [startLine, endLine] 区间内容的"修复后文本"。
 */
public final class FixApplier {

    private FixApplier() {
    }

    public static void previewAndApply(@NotNull Project project, @NotNull IssueModel issue) {
        if (!issue.isFixable() || issue.getFixPatch() == null || issue.getFixPatch().isEmpty()) {
            Messages.showInfoMessage(project, "该问题暂无可用修复", "智能代码评审");
            return;
        }
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(issue.getFilePath());
        if (file == null) {
            Messages.showWarningDialog(project, "找不到目标文件", "智能代码评审");
            return;
        }
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            Messages.showWarningDialog(project, "无法读取文件内容", "智能代码评审");
            return;
        }

        // 行号严格限制在文档范围内（1 起），避免越界
        int lineCount = document.getLineCount();
        int startLine = Math.min(Math.max(issue.getStartLine(), 1), lineCount);
        int endLine = Math.min(Math.max(issue.getEndLine(), startLine), lineCount);
        int startOffset = document.getLineStartOffset(startLine - 1);
        int endOffset = document.getLineEndOffset(endLine - 1);

        // Diff 预览：左侧当前代码，右侧修复后代码（模态窗口，关闭后再确认）
        String currentText = document.getText(new TextRange(startOffset, endOffset));
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        SimpleDiffRequest request = new SimpleDiffRequest(
                "修复预览 - " + issue.getMessage(),
                contentFactory.create(currentText),
                contentFactory.create(issue.getFixPatch()),
                "当前代码", "修复后");
        DiffManager.getInstance().showDiff(project, request, DiffDialogHints.MODAL);

        int answer = Messages.showYesNoDialog(project,
                "是否应用该修复？", "智能代码评审", Messages.getQuestionIcon());
        if (answer != Messages.YES) {
            return;
        }

        // 在写命令中修改文档（IntelliJ 平台规范：改动文件必须包在 WriteCommandAction 里）
        WriteCommandAction.runWriteCommandAction(project,
                () -> document.replaceString(startOffset, endOffset, issue.getFixPatch()));

        // 跳转到修改位置
        new OpenFileDescriptor(project, file, startOffset).navigate(true);
    }
}
