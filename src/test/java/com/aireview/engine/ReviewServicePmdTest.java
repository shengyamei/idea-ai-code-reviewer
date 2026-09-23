package com.aireview.engine;

import com.aireview.analysis.FrameworkAnalyzerTestCase;
import com.aireview.analysis.TransactionalAnalyzer;
import com.aireview.model.IssueModel;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiFile;

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

    private static boolean containsRule(List<IssueModel> issues, String ruleId) {
        return issues.stream().anyMatch(issue -> ruleId.equals(issue.getRuleId()));
    }
}
