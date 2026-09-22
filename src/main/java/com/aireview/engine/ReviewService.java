package com.aireview.engine;

import com.aireview.analysis.FrameworkAnalyzer;
import com.aireview.model.IssueModel;
import com.intellij.psi.PsiFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * C2模块：ReviewService 评审融合服务
 * 功能：汇聚多源扫描结果、去重、排序，对外提供统一评审入口
 * 调用：PMD规则扫描、FrameworkAnalyzer框架分析、LLM语义评审
 */
public class ReviewService {

    private final FrameworkAnalyzer frameworkAnalyzer;

    public ReviewService(FrameworkAnalyzer frameworkAnalyzer) {
        this.frameworkAnalyzer = frameworkAnalyzer;
    }

    /**
     * 对外唯一入口，UI模块调用此方法
     * @param psiFile 当前打开的PSI文件
     * @return 合并、去重、排序后的缺陷列表
     */
    public List<IssueModel> review(PsiFile psiFile) {
        List<IssueModel> totalIssues = new ArrayList<>();
        String fileText = psiFile.getText();
        String filePath = psiFile.getVirtualFile().getPath();

        // 1. 框架规则分析（左晶晶）
        List<IssueModel> frameworkIssues = frameworkAnalyzer.analyze(psiFile);
        totalIssues.addAll(frameworkIssues);

        // 2. PMD静态规则扫描（E1）
        List<IssueModel> pmdIssues = runPmdScan(filePath, fileText);
        totalIssues.addAll(pmdIssues);

        // 3. LLM语义评审（E2）
        List<IssueModel> llmIssues = runLlmReview(fileText, filePath);
        totalIssues.addAll(llmIssues);

        // 4. 去重 + 按严重级别排序（枚举声明顺序 CRITICAL→INFO，即严重度降序）
        List<IssueModel> mergedList = new ArrayList<>(deduplicate(totalIssues));
        mergedList.sort(Comparator.comparing(IssueModel::getSeverity));

        // 5. 生成修复补丁（E4）
        for(IssueModel issue : mergedList){
            generatePatch(issue, fileText);
        }
        return mergedList;
    }

    /**
     * PMD规则扫描，转换为IssueModel
     */
    private List<IssueModel> runPmdScan(String filePath, String sourceCode) {
        List<IssueModel> list = new ArrayList<>();
        // 这里后续接入PMD，先保留占位，可扩展
        return list;
    }

    /**
     * LLM大模型评审，调用API并解析JSON结果
     */
    private List<IssueModel> runLlmReview(String sourceCode, String filePath) {
        List<IssueModel> list = new ArrayList<>();
        // LLM http调用、json解析占位，后续实现
        return list;
    }

    /**
     * 简单去重：文件路径+起始行+消息作为唯一键
     */
    private List<IssueModel> deduplicate(List<IssueModel> rawList) {
        return rawList.stream()
                .collect(Collectors.toMap(
                        item -> item.getFilePath() + "|" + item.getStartLine() + "|" + item.getMessage(),
                        item -> item,
                        (oldItem, newItem) -> oldItem
                ))
                .values()
                .stream()
                .toList();
    }

    /**
     * 生成修复补丁，填充fixPatch字段。
     * 事务规则：在 @Transactional 注解上补 rollbackFor = Exception.class（复用该行原文、仅替换注解片段，
     * 与"整行替换"语义一致）；注解跨行或行内圆括号不配平时不生成补丁，避免误伤代码。
     */
    private void generatePatch(IssueModel issue, String sourceCode) {
        if (!issue.isFixable()) {
            return;
        }
        if ("TransactionRollbackFor".equals(issue.getRuleId())) {
            String[] lines = sourceCode.split("\n", -1);
            int index = issue.getStartLine() - 1;
            if (index < 0 || index >= lines.length) {
                return;
            }
            String line = lines[index];
            if (!isBracketBalanced(line)) {
                return;
            }
            if (line.contains("@Transactional(")) {
                issue.setFixPatch(line.replace("@Transactional(", "@Transactional(rollbackFor = Exception.class, "));
            } else if (line.contains("@Transactional")) {
                issue.setFixPatch(line.replace("@Transactional", "@Transactional(rollbackFor = Exception.class)"));
            }
        }
    }

    /** 行内圆括号是否配平（跨行注解不生成补丁）。 */
    private static boolean isBracketBalanced(String line) {
        int depth = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
        }
        return depth == 0;
    }
}
