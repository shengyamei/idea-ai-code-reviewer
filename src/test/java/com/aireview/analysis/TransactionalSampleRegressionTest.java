package com.aireview.analysis;

import com.aireview.model.IssueModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 样本集回归测试（P3 交付物的一部分，兼作全队联调验证物料）：
 * 扫描 src/test/resources/samples 下的 SXX_*.java 样本，
 * 逐个调用 TransactionalAnalyzer 并与同名 .expected.json 比对，收集全部差异后统一报告。
 * 新增样本只需按约定命名成对放置，无需修改本类。
 */
public class TransactionalSampleRegressionTest extends FrameworkAnalyzerTestCase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void testAllSamples() throws Exception {
        URL samplesUrl = getClass().getResource("/samples");
        assertNotNull("资源目录 /samples 不存在（请先执行 processTestResources）", samplesUrl);
        File sampleDir = new File(samplesUrl.toURI());
        File[] javaFiles = sampleDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull("样本目录不可读：" + sampleDir, javaFiles);
        assertTrue("样本目录为空：" + sampleDir, javaFiles.length > 0);
        Arrays.sort(javaFiles, Comparator.comparing(File::getName));

        List<String> failures = new ArrayList<>();
        for (File javaFile : javaFiles) {
            verifySample(javaFile, failures);
        }
        if (!failures.isEmpty()) {
            fail("样本回归失败 " + failures.size() + " 处：\n" + String.join("\n", failures));
        }
    }

    /** 校验单个样本：数量与逐条字段都必须匹配期望 JSON。 */
    private void verifySample(File javaFile, List<String> failures) throws Exception {
        String sampleName = javaFile.getName();
        File expectedFile = new File(javaFile.getParentFile(), sampleName.replace(".java", ".expected.json"));
        if (!expectedFile.isFile()) {
            failures.add(sampleName + "：缺少期望文件 " + expectedFile.getName());
            return;
        }
        String source = Files.readString(javaFile.toPath(), StandardCharsets.UTF_8);
        JsonNode expectedIssues = MAPPER.readTree(
                Files.readString(expectedFile.toPath(), StandardCharsets.UTF_8)).path("expectedIssues");

        List<IssueModel> actual;
        try {
            actual = new TransactionalAnalyzer().analyze(myFixture.configureByText(sampleName, source));
        } catch (Throwable t) {
            failures.add(sampleName + "：分析过程异常 " + t);
            return;
        }

        if (actual.size() != expectedIssues.size()) {
            StringBuilder actualSummary = new StringBuilder();
            for (IssueModel issue : actual) {
                actualSummary.append(issue.getRuleId()).append('@').append(issue.getStartLine()).append(' ');
            }
            failures.add(sampleName + "：期望 " + expectedIssues.size() + " 条，实际 " + actual.size()
                    + " 条 [" + actualSummary.toString().trim() + "]");
            return;
        }
        for (int i = 0; i < expectedIssues.size(); i++) {
            String mismatch = firstMismatch(expectedIssues.get(i), actual.get(i));
            if (mismatch != null) {
                failures.add(sampleName + " 第 " + (i + 1) + " 条：" + mismatch);
            }
        }
    }

    /** 返回第一条不一致的字段描述；全部一致返回 null。 */
    private static String firstMismatch(JsonNode expected, IssueModel actual) {
        if (!expected.path("ruleId").asText().equals(actual.getRuleId())) {
            return "ruleId 期望 " + expected.path("ruleId").asText() + "，实际 " + actual.getRuleId();
        }
        String severity = actual.getSeverity() == null ? "null" : actual.getSeverity().name();
        if (!expected.path("severity").asText().equals(severity)) {
            return "severity 期望 " + expected.path("severity").asText() + "，实际 " + severity;
        }
        if (expected.path("startLine").asInt() != actual.getStartLine()) {
            return "startLine 期望 " + expected.path("startLine").asInt() + "，实际 " + actual.getStartLine();
        }
        if (expected.path("endLine").asInt() != actual.getEndLine()) {
            return "endLine 期望 " + expected.path("endLine").asInt() + "，实际 " + actual.getEndLine();
        }
        return null;
    }
}
