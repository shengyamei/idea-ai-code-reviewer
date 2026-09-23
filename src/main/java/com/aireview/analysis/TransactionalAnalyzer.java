package com.aireview.analysis;

import com.aireview.model.IssueModel;
import com.aireview.model.Severity;
import com.aireview.model.Source;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.InheritanceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 框架感知分析器实现：@Transactional 事务失效检测。
 * 校赛 v1 规则：方法声明 throws 受检异常，且注解未配置 rollbackFor / rollbackForClassName。
 * 实现人：左晶晶；调用方：盛雅梅（融合裁决）。
 */
public class TransactionalAnalyzer implements FrameworkAnalyzer {

    private static final String TX_FQN = "org.springframework.transaction.annotation.Transactional";
    private static final String RULE_ID = "TransactionRollbackFor";

    private static final Logger LOG = Logger.getInstance(TransactionalAnalyzer.class);

    @Override
    public List<IssueModel> analyze(PsiFile file) {
        // PSI 访问必须处于读动作内；这里自包含包装，保证从 EDT 或后台线程调用都安全（联调约定）
        return ReadAction.compute(() -> doAnalyze(file));
    }

    private List<IssueModel> doAnalyze(PsiFile file) {
        List<IssueModel> issues = new ArrayList<>();
        if (!(file instanceof PsiJavaFile)) {
            return issues;
        }
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null) {
            return issues;
        }
        file.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitMethod(PsiMethod method) {
                super.visitMethod(method);
                PsiAnnotation annotation = findTransactional(method);
                if (annotation == null || hasRollbackFor(annotation)) {
                    return;
                }
                List<String> checkedExceptions = checkedExceptionsOf(method);
                if (checkedExceptions.isEmpty()) {
                    return;
                }
                issues.add(buildIssue(file, method, annotation, checkedExceptions, document));
            }
        });
        return issues;
    }

    /**
     * 优先按解析结果匹配 Spring 注解；无法解析时（演示工程不带 Spring 依赖）降级为短名匹配。
     * 注意：注解无法解析时 getQualifiedName() 返回短名而非 null，不能依赖它为 null 来判断未解析。
     */
    private PsiAnnotation findTransactional(PsiMethod method) {
        for (PsiAnnotation candidate : method.getModifierList().getAnnotations()) {
            PsiJavaCodeReferenceElement nameReference = candidate.getNameReferenceElement();
            if (nameReference == null) {
                continue;
            }
            String referenceText = nameReference.getText();
            if (!"Transactional".equals(referenceText) && !referenceText.endsWith(".Transactional")) {
                continue;
            }
            PsiElement resolved = nameReference.resolve();
            if (resolved == null) {
                // 无法解析：降级为短名匹配
                return candidate;
            }
            if (resolved instanceof PsiClass && TX_FQN.equals(((PsiClass) resolved).getQualifiedName())) {
                return candidate;
            }
        }
        return null;
    }

    private boolean hasRollbackFor(PsiAnnotation annotation) {
        return annotation.findDeclaredAttributeValue("rollbackFor") != null
                || annotation.findDeclaredAttributeValue("rollbackForClassName") != null;
    }

    /**
     * 只保留受检异常：Throwable 子类且非 RuntimeException / Error。
     * 类型无法解析（工程未关联 JDK / 索引未就绪）时按短名启发式兑底，保证解析环境不完整时仍能检出。
     */
    private List<String> checkedExceptionsOf(PsiMethod method) {
        List<String> names = new ArrayList<>();
        for (PsiClassType type : method.getThrowsList().getReferencedTypes()) {
            PsiClass resolved = type.resolve();
            if (resolved == null) {
                String presentable = type.getPresentableText();
                if (isLikelyUnchecked(presentable)) {
                    continue;
                }
                LOG.info("事务分析：异常类型 " + presentable + " 未能解析，按受检异常兑底处理（工程解析环境可能不完整）");
                names.add(presentable);
                continue;
            }
            if (!InheritanceUtil.isInheritor(resolved, CommonClassNames.JAVA_LANG_THROWABLE)) {
                continue;
            }
            if (InheritanceUtil.isInheritor(resolved, CommonClassNames.JAVA_LANG_RUNTIME_EXCEPTION)
                    || InheritanceUtil.isInheritor(resolved, CommonClassNames.JAVA_LANG_ERROR)) {
                continue;
            }
            names.add(type.getPresentableText());
        }
        return names;
    }

    /** 解析失败时的短名启发式：命中 RuntimeException/Error 家族常见命名则不按受检异常处理。 */
    private static boolean isLikelyUnchecked(String shortName) {
        if (shortName.endsWith("RuntimeException") || shortName.endsWith("Error")) {
            return true;
        }
        return switch (shortName) {
            case "RuntimeException", "Error", "IllegalArgumentException", "IllegalStateException",
                 "NullPointerException", "UnsupportedOperationException", "IndexOutOfBoundsException",
                 "ArithmeticException", "ClassCastException", "NumberFormatException",
                 "ConcurrentModificationException", "SecurityException" -> true;
            default -> false;
        };
    }

    private IssueModel buildIssue(PsiFile file, PsiMethod method, PsiAnnotation annotation,
                                  List<String> checkedExceptions, Document document) {
        int line = document.getLineNumber(annotation.getTextOffset()) + 1;

        IssueModel issue = new IssueModel();
        issue.setId(UUID.randomUUID().toString());
        issue.setSource(Source.RULE);
        issue.setRuleId(RULE_ID);
        issue.setSeverity(Severity.CRITICAL);
        issue.setFilePath(file.getVirtualFile() != null ? file.getVirtualFile().getPath() : file.getName());
        issue.setStartLine(line);
        issue.setEndLine(line);
        issue.setMessage("事务方法声明了受检异常但未配置 rollbackFor，异常发生时事务不会回滚");
        issue.setEvidence("方法 " + method.getName() + "() 声明 throws " + String.join("、", checkedExceptions)
                + "；@Transactional 未配置 rollbackFor/rollbackForClassName，Spring 默认仅对 RuntimeException 与 Error 回滚");
        issue.setFixable(true);
        return issue;
    }
}
