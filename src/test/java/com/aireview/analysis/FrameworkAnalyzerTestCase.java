package com.aireview.analysis;

import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

/**
 * 框架分析器测试基类：light fixture 默认不带 JDK（projectSdk 为 null），
 * 会导致 throws 类型解析失败、受检异常判定全部跳过；
 * 这里挂上测试 JVM 的真实 JDK，使解析行为与真实 IDE 一致。
 */
public abstract class FrameworkAnalyzerTestCase extends LightJavaCodeInsightFixtureTestCase {

    private static final LightProjectDescriptor REAL_JDK_DESCRIPTOR = new LightProjectDescriptor() {
        @Override
        public Sdk getSdk() {
            return JavaSdk.getInstance().createJdk("test-real-jdk", System.getProperty("java.home"));
        }
    };

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return REAL_JDK_DESCRIPTOR;
    }
}
