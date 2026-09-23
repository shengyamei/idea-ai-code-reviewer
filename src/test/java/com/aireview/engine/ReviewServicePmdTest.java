package com.aireview.engine;

import com.aireview.analysis.FrameworkAnalyzerTestCase;
import com.aireview.analysis.TransactionalAnalyzer;
import com.aireview.model.IssueModel;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiFile;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * PMD 规则扫描链路测试：验证未使用变量、空 catch 块能被检出并转换为统一模型。
 * 实现人：葛佩娴（集成阶段）。
 */
public class ReviewServicePmdTest extends FrameworkAnalyzerTestCase {

    public void testPmdDetectsUnusedVariableAndEmptyCatch() {
        String code = "import java.io.IOException;\n" +
                "class Foo {\n" +
                "    public String createOrder(String orderId) throws IOException {\n" +
                "        String traceId = orderId + \"-x\";\n" +
                "        return orderId;\n" +
                "    }\n" +
                "\n" +
                "    public void warmup() {\n" +
                "        try {\n" +
                "            ping();\n" +
                "        } catch (IOException e) {\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    void ping() throws IOException {\n" +
                "    }\n" +
                "}\n";
        PsiFile file = myFixture.configureByText("Foo.java", code);
        List<IssueModel> issues = ReadAction.compute(() -> new ReviewService(new TransactionalAnalyzer()).review(file));

        assertTrue(containsRule(issues, "UnusedLocalVariable"));
        assertTrue(containsRule(issues, "EmptyCatchBlock"));
    }

    /** 演示样本整文件：应产出 3 条缺陷（事务 1 + PMD 2），且三条均有可应用的修复补丁，锁定演示故事线。 */
    public void testDemoFileReportsThreeDefects() throws Exception {
        String source = Files.readString(Path.of("demo", "DemoOrderService.java"), StandardCharsets.UTF_8);
        PsiFile file = myFixture.configureByText("DemoOrderService.java", source);
        List<IssueModel> issues = ReadAction.compute(() -> new ReviewService(new TransactionalAnalyzer()).review(file));

        assertEquals(3, issues.size());
        assertTrue(containsRule(issues, "TransactionRollbackFor"));
        assertTrue(containsRule(issues, "UnusedLocalVariable"));
        assertTrue(containsRule(issues, "EmptyCatchBlock"));

        // 三条缺陷均应标记为可修复且有修复补丁（锁定"应用修复"演示链路）
        for (IssueModel issue : issues) {
            assertTrue(issue.getRuleId() + " 应标记为可修复", issue.isFixable());
            assertNotNull(issue.getRuleId() + " 应有修复补丁", issue.getFixPatch());
        }
        // 未使用变量：空补丁 = 删除整行（行尾换行由 FixApplier 连带删除）
        assertEquals("", findByRule(issues, "UnusedLocalVariable").getFixPatch());
        // 空 catch：块内补充包装抛出语句，重新评审即可通过
        assertEquals("        } catch (IOException e) {\n"
                        + "            throw new IllegalStateException(\"捕获到未处理异常\", e);\n"
                        + "        }",
                findByRule(issues, "EmptyCatchBlock").getFixPatch());
        // 事务：注解行补上 rollbackFor
        assertEquals("    @Transactional(rollbackFor = Exception.class)",
                findByRule(issues, "TransactionRollbackFor").getFixPatch());
    }

    /** 演示样本二：用户服务模块，4 条缺陷（事务 1 + 未使用变量 2 + 空 catch 1）+ 2 个合规反例。 */
    public void testSecondDemoFileReportsFourDefects() throws Exception {
        String source = Files.readString(Path.of("demo", "DemoUserService.java"), StandardCharsets.UTF_8);
        PsiFile file = myFixture.configureByText("DemoUserService.java", source);
        List<IssueModel> issues = ReadAction.compute(() -> new ReviewService(new TransactionalAnalyzer()).review(file));

        assertEquals(4, issues.size());
        assertEquals(1, countRule(issues, "TransactionRollbackFor"));
        assertEquals(2, countRule(issues, "UnusedLocalVariable"));
        assertEquals(1, countRule(issues, "EmptyCatchBlock"));
        // 合规反例不报告：archive 已配置 rollbackFor（28 行）、markFailed 仅抛运行时异常（33 行）
        assertFalse(containsLine(issues, 28));
        assertFalse(containsLine(issues, 33));

        // 事务补丁：注解已有参数时仍能正确插入 rollbackFor
        assertEquals("    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)",
                findByRule(issues, "TransactionRollbackFor").getFixPatch());
        // 4 条缺陷均应可修复
        for (IssueModel issue : issues) {
            assertTrue(issue.getRuleId() + " 应标记为可修复", issue.isFixable());
            assertNotNull(issue.getRuleId() + " 应有修复补丁", issue.getFixPatch());
        }
    }

    /** 健壮性保障：文件存在语法残缺（类/方法未闭合）时，评审不崩溃，且可解析部分照常产出结果。 */
    public void testBrokenSyntaxFileDoesNotCrashReview() throws Exception {
        String broken = "class Broken {\n"
                + "    @Transactional\n"
                + "    public void run() throws java.io.IOException {\n"
                + "        doWork();\n";
        PsiFile file = myFixture.configureByText("Broken.java", broken);
        List<IssueModel> issues = ReadAction.compute(() -> new ReviewService(new TransactionalAnalyzer()).review(file));
        // PSI 错误恢复下仍能解析出 @Transactional 与受检异常声明，事务检查照常命中
        assertNotNull(issues);
        assertTrue(containsRule(issues, "TransactionRollbackFor"));
    }

    /** 现场演示保障：模拟"现场编辑制造缺陷"——删掉 archive 的 rollbackFor 参数后，应立即多报 1 条事务。 */
    public void testLiveEditRemovingRollbackForTriggersNewIssue() throws Exception {
        String source = Files.readString(Path.of("demo", "DemoUserService.java"), StandardCharsets.UTF_8);
        String edited = source.replace("@Transactional(rollbackFor = Exception.class)", "@Transactional");
        assertFalse("样本二应包含可删除的 rollbackFor 参数", source.equals(edited));
        PsiFile file = myFixture.configureByText("DemoUserService.java", edited);
        List<IssueModel> issues = ReadAction.compute(() -> new ReviewService(new TransactionalAnalyzer()).review(file));

        // 原 4 条 + archive（28 行）新增 1 条事务
        assertEquals(5, issues.size());
        assertEquals(2, countRule(issues, "TransactionRollbackFor"));
        assertTrue(containsLine(issues, 28));
    }

    /**
     * PMD 类型系统初始化依赖 classLoader.getResourceAsStream("java/lang/*.class")。
     * IDEA 插件类加载器对 JDK 资源不可见（真机环境中曾导致初始化 NPE），
     * 因此 runPmdScan 显式指定平台类加载器——这里锁定该事实：平台 CL 必须可解析 JDK 基础类。
     */
    public void testPlatformClassLoaderResolvesJdkClassResources() {
        assertNotNull(ClassLoader.getPlatformClassLoader().getResourceAsStream("java/lang/Boolean.class"));
        assertNotNull(ClassLoader.getPlatformClassLoader().getResourceAsStream("java/lang/Object.class"));

        // 模拟 IDEA 插件类加载器：对资源请求完全不委托父加载器
        ClassLoader isolated = new ClassLoader(null) {
            @Override
            public URL getResource(String name) {
                return null;
            }
        };
        assertNull(isolated.getResourceAsStream("java/lang/Boolean.class"));
    }

    private static boolean containsRule(List<IssueModel> issues, String ruleId) {
        return issues.stream().anyMatch(issue -> ruleId.equals(issue.getRuleId()));
    }

    private static IssueModel findByRule(List<IssueModel> issues, String ruleId) {
        return issues.stream().filter(issue -> ruleId.equals(issue.getRuleId())).findFirst().orElse(null);
    }

    private static int countRule(List<IssueModel> issues, String ruleId) {
        return (int) issues.stream().filter(issue -> ruleId.equals(issue.getRuleId())).count();
    }

    private static boolean containsLine(List<IssueModel> issues, int line) {
        return issues.stream().anyMatch(issue -> issue.getStartLine() == line);
    }
}
