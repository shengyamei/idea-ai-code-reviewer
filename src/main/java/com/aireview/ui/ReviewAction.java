package com.aireview.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;

/**
 * 编辑器右键菜单"智能代码评审"入口。
 * 当前阶段：拿到 PSI 文件并弹窗验证；后续在此接入 ReviewService（盛雅梅）并驱动结果面板。
 */
public class ReviewAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(ReviewAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        if (file == null) {
            Messages.showWarningDialog("没有获取到当前文件", "智能代码评审");
            return;
        }

        String path = file.getVirtualFile() != null ? file.getVirtualFile().getPath() : file.getName();
        LOG.info("触发评审: " + path);

        // TODO G4：替换为 ReviewService.review(file) 调用，把结果渲染到工具窗口面板
        Messages.showInfoMessage("已触发评审：" + file.getName(), "智能代码评审");
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 仅在打开了 Java 文件时显示菜单项
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabledAndVisible(file instanceof PsiJavaFile);
    }
}
