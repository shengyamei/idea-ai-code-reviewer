plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

dependencies {
    intellijPlatform {
        // 目标平台：IDEA 社区版 2024.2（兼容 2024.2 及以上的 IDEA）
        intellijIdeaCommunity("2024.2")
        // Java 语言支持：PsiJavaFile / PsiMethod / PsiClass 等 Java PSI 类来自该内置插件
        bundledPlugin("com.intellij.java")
    }
}

// 统一使用 JDK 17（LTS）：全组标配、沙箱 IDEA 2024.2 原生支持，直接产出 17 字节码
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
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
