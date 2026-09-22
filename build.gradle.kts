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
    }
    // 项目依赖：PMD + Jackson
    implementation("net.sourceforge.pmd:pmd-java:7.4.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    // JUnit5 单元测试
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
}

// 统一使用 JDK 17（LTS）：全组标配、沙箱 IDEA 2024.2 原生支持，直接产出 17 字节码
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType<JavaCompile> {
    options.release.set(17)
    // 显式指定 UTF-8：避免中文 Windows 命令行构建时按 GBK 读取源文件而失败
    options.encoding = "UTF-8"
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
        }
    }
}

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
