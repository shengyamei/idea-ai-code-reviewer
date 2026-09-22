package com.aireview.engine;

import com.aireview.model.IssueModel;
import com.aireview.model.Severity;
import com.aireview.model.Source;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ReviewService 的模拟实现：返回固定 3 条结果，供界面联调与演示。
 * 盛雅梅的 ReviewServiceImpl 就绪后替换（调用方只需改 new 的实现类）。
 * 注意：真实实现（PMD + 大模型）耗时较长，接入时应改为后台线程执行。
 */
public class MockReviewServiceImpl implements ReviewService {

    @Override
    public List<IssueModel> review(PsiFile file) {
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
