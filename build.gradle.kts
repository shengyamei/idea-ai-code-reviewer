import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
    id("jacoco")
}

group = "com.aireview"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // 目标平台：IDEA 社区版 2024.2（兼容 2024.2 及以上的 IDEA）
        intellijIdeaCommunity("2024.2")
        // Java 语言支持：PsiJavaFile / PsiMethod / PsiClass 等 Java PSI 类来自该内置插件
        bundledPlugin("com.intellij.java")
        // 测试框架：平台测试框架 + Java 插件测试框架（提供 LightJavaCodeInsightFixtureTestCase）
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    // 项目依赖：PMD + Jackson
    implementation("net.sourceforge.pmd:pmd-java:7.4.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    // 单元测试：JUnit5（业务测试）+ JUnit4（平台测试框架的 fixture 测试基于 JUnit4，由 vintage 引擎执行）
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    // Gradle 9 起不再自动注入 JUnit Platform 启动器，需显式声明
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.1")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

// 各开发机 JDK 版本不一（17 / 21 / 26）：直接采用当前 Gradle JVM 作为编译工具链（必然存在，无需下载，
// 避免平台插件默认 toolchain 21 在未安装 21 的机器上构建失败），配合 release = 17 保证全组字节码一致
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(JavaVersion.current().majorVersion)
    }
}

// 显式指定 UTF-8：避免中文 Windows 命令行构建时按 GBK 读取源文件而失败
tasks.withType<JavaCompile> {
    options.release.set(17)
    options.encoding = "UTF-8"
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
        }
    }
}

// JUnit Platform 统一执行：vintage 引擎负责跑 JUnit4 风格的平台 fixture 测试
tasks.test {
    useJUnitPlatform()
}

// jacoco 覆盖率，若后续报内部类错误，可以注释掉这一整块
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}
