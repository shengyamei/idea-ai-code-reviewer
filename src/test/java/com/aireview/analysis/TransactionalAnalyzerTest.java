package com.aireview.analysis;

import com.aireview.model.IssueModel;
import com.aireview.model.Severity;
import com.aireview.model.Source;

import java.util.List;

/**
 * 事务失效分析器冒烟测试：验证 P1 原型（定位 @Transactional 方法）与 P2 规则 v1 的基本判定。
 * 完整样本集回归（10 个样本 + 期望 JSON）见后续测试，本类只保底验证框架链路可用。
 * 实现人：左晶晶。
 */
public class TransactionalAnalyzerTest extends FrameworkAnalyzerTestCase {

    /** 正例：@Transactional 方法声明受检异常且未配置 rollbackFor，应报告 1 条 CRITICAL。 */
    public void testCheckedExceptionWithoutRollbackFor() {
        String code = "import org.springframework.transaction.annotation.Transactional;\n" +
                "\n" +
                "class Foo {\n" +
                "    @Transactional\n" +
                "    public void bar() throws java.io.IOException {\n" +
                "    }\n" +
                "}\n";
        List<IssueModel> issues = new TransactionalAnalyzer().analyze(myFixture.configureByText("Foo.java", code));

        assertEquals(1, issues.size());
        IssueModel issue = issues.get(0);
        assertEquals(Source.RULE, issue.getSource());
        assertEquals("TransactionRollbackFor", issue.getRuleId());
        assertEquals(Severity.CRITICAL, issue.getSeverity());
        assertEquals(4, issue.getStartLine());
        assertEquals(4, issue.getEndLine());
        assertTrue(issue.isFixable());
    }

    /** 反例：已配置 rollbackFor，不应报告。 */
    public void testWithRollbackForNotReported() {
        String code = "import org.springframework.transaction.annotation.Transactional;\n" +
                "\n" +
                "class Foo {\n" +
                "    @Transactional(rollbackFor = Exception.class)\n" +
                "    public void bar() throws java.io.IOException {\n" +
                "    }\n" +
                "}\n";
        List<IssueModel> issues = new TransactionalAnalyzer().analyze(myFixture.configureByText("Bar.java", code));

        assertEmpty(issues);
    }

    /** 同一文件内两个方法均命中：验证遍历不遗漏、不重复。 */
    public void testTwoTransactionalMethodsBothReported() {
        String code = "import org.springframework.transaction.annotation.Transactional;\n" +
                "\n" +
                "class Foo {\n" +
                "    @Transactional\n" +
                "    public void a() throws java.io.IOException {\n" +
                "    }\n" +
                "\n" +
                "    @Transactional\n" +
                "    public void b() throws java.sql.SQLException {\n" +
                "    }\n" +
                "}\n";
        List<IssueModel> issues = new TransactionalAnalyzer().analyze(myFixture.configureByText("Multi.java", code));

        assertEquals(2, issues.size());
        assertEquals(4, issues.get(0).getStartLine());
        assertEquals(8, issues.get(1).getStartLine());
    }
}
