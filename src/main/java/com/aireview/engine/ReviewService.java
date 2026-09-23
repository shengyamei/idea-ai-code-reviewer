package com.aireview.engine;

import com.aireview.analysis.FrameworkAnalyzer;
import com.aireview.model.IssueModel;
import com.aireview.model.Severity;
import com.aireview.model.Source;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.lang.rule.RulePriority;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;

/**
 * C2模块：ReviewService 评审融合服务
 * 功能：汇聚多源扫描结果、去重、排序，对外提供统一评审入口
 * 调用：PMD规则扫描、FrameworkAnalyzer框架分析、LLM语义评审
 */
public class ReviewService {

    private static final Logger LOG = Logger.getInstance(ReviewService.class);

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

        LOG.info("引擎结果：框架 " + frameworkIssues.size() + " 条，PMD " + pmdIssues.size()
                + " 条，LLM " + llmIssues.size() + " 条");

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
     * PMD静态规则扫描（E1），转换为IssueModel。
     * 校赛启用规则：UnusedLocalVariable（未使用变量）、EmptyCatchBlock（空 catch 块）。
     * PMD 为纯文本分析、不依赖 IDE 环境；扫描失败时返回空列表，不影响其他引擎。
     */
    private List<IssueModel> runPmdScan(String filePath, String sourceCode) {
        List<IssueModel> list = new ArrayList<>();
        try {
            PMDConfiguration configuration = new PMDConfiguration();
            // IDEA 插件类加载器对 JDK 资源（java/lang/*.class）不可见，PMD 类型系统初始化会失败，
            // 必须显式指定平台类加载器用于解析 JDK 符号（单测环境为常规类加载器，不会暴露此问题）
            configuration.setClassLoader(ClassLoader.getPlatformClassLoader());
            configuration.addRuleSet("category/java/bestpractices.xml/UnusedLocalVariable");
            configuration.addRuleSet("category/java/errorprone.xml/EmptyCatchBlock");
            try (PmdAnalysis analysis = PmdAnalysis.create(configuration)) {
                analysis.files().addSourceFile(FileId.fromPathLikeString(filePath), sourceCode);
                Report report = analysis.performAnalysisAndCollectReport();
                for (RuleViolation violation : report.getViolations()) {
                    list.add(toIssueModel(violation, filePath));
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("PMD 扫描失败，本次跳过规则扫描：" + e.getMessage(), e);
        }
        return list;
    }

    /** PMD 违规项 → 统一评审模型。 */
    private static IssueModel toIssueModel(RuleViolation violation, String filePath) {
        IssueModel issue = new IssueModel();
        issue.setId(UUID.randomUUID().toString());
        issue.setSource(Source.RULE);
        issue.setRuleId(violation.getRule().getName());
        issue.setSeverity(mapSeverity(violation.getRule().getPriority()));
        issue.setFilePath(filePath);
        issue.setStartLine(violation.getBeginLine());
        issue.setEndLine(violation.getEndLine());
        issue.setMessage(violation.getDescription());
        issue.setEvidence("PMD 规则 " + violation.getRule().getName() + "：" + violation.getDescription());
        issue.setFixable(false);
        return issue;
    }

    /** PMD 优先级（1 为最高）映射为评审级别。 */
    private static Severity mapSeverity(RulePriority priority) {
        int level = priority.getPriority();
        if (level <= 2) {
            return Severity.CRITICAL;
        }
        if (level == 3) {
            return Severity.MAJOR;
        }
        if (level == 4) {
            return Severity.MINOR;
        }
        return Severity.INFO;
    }

    /**
     * LLM大模型评审（E2），调用API并解析JSON结果。
     * 当前为离线占位：待配置大模型 API Key 后启用（依赖 jackson 已就绪）。
     */
    private List<IssueModel> runLlmReview(String sourceCode, String filePath) {
        List<IssueModel> list = new ArrayList<>();
        // 预留：接入大模型 API 后在此发起请求，将返回 JSON 解析为 IssueModel（Source.LLM）
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
