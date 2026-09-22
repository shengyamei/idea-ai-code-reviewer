package com.aireview.engine;

import com.aireview.analysis.FrameworkAnalyzerTestCase;
import com.aireview.analysis.TransactionalAnalyzer;
import com.aireview.model.IssueModel;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiFile;

import java.util.List;

/**
 * 融合层修复补丁生成测试：验证"应用修复"拿到的是真实 rollbackFor 补丁（而非占位文本），
 * 且补丁行与源文件的"整行替换"语义一致。实现人：葛佩娴（集成阶段）。
 */
public class ReviewServicePatchTest extends FrameworkAnalyzerTestCase {

    /** 无参注解：补全为 @Transactional(rollbackFor = Exception.class)，保留缩进。 */
    public void testPatchWithoutAttributes() {
        String code = "import org.springframework.transaction.annotation.Transactional;\n" +
                "\n" +
                "class Foo {\n" +
                "    @Transactional\n" +
                "    public void bar() throws java.io.IOException {\n" +
                "    }\n" +
                "}\n";
        PsiFile file = myFixture.configureByText("Foo.java", code);
        List<IssueModel> issues = ReadAction.compute(() -> new ReviewService(new TransactionalAnalyzer()).review(file));

        assertEquals(1, issues.size());
        IssueModel issue = issues.get(0);
        assertTrue(issue.isFixable());
        assertEquals("    @Transactional(rollbackFor = Exception.class)", issue.getFixPatch());
    }

    /** 带参数注解：在已有参数前插入 rollbackFor，原有参数保留。 */
    public void testPatchWithExistingAttributes() {
        String code = "import org.springframework.transaction.annotation.Transactional;\n" +
                "\n" +
                "class Foo {\n" +
                "    @Transactional(timeout = 30, readOnly = false)\n" +
                "    public void bar() throws java.io.IOException {\n" +
                "    }\n" +
                "}\n";
        PsiFile file = myFixture.configureByText("Foo2.java", code);
        List<IssueModel> issues = ReadAction.compute(() -> new ReviewService(new TransactionalAnalyzer()).review(file));

        assertEquals(1, issues.size());
        assertEquals("    @Transactional(rollbackFor = Exception.class, timeout = 30, readOnly = false)",
                issues.get(0).getFixPatch());
    }
}
