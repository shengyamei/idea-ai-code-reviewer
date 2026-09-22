import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

dependencies {
    intellijPlatform {
        // 目标平台：IDEA 社区版 2024.2（兼容 2024.2 及以上的 IDEA）
        intellijIdeaCommunity("2024.2")
        // Java 插件（PSI：PsiClass/PsiMethod/PsiAnnotation 等），对应 plugin.xml 中的 com.intellij.modules.java
        bundledPlugin("com.intellij.java")
        // 测试框架：平台测试框架 + Java 插件测试框架（提供 LightJavaCodeInsightFixtureTestCase）
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    // 单元测试：平台测试框架基于 JUnit 4，且平台类引用了 opentest4j
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
    // 样本集期望结果 JSON 解析（仅测试用，不进插件产物）
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
}

// 编译使用本机 JDK 26（不下载其他 JDK），产出 Java 17 字节码，保证插件能装进低版本 IDEA
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

tasks.withType<JavaCompile> {
    options.release.set(17)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
        }
    }
}
