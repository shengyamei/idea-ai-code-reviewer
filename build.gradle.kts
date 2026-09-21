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
