plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

dependencies {
    intellijPlatform {
        // 目标平台：IDEA 社区版 2024.2（兼容 2024.2 及以上的 IDEA）
        intellijIdeaCommunity("2024.2")
    }
}

// 本机可用 JDK 26 编译，但产出 Java 17 字节码，保证插件能装进低版本 IDEA
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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
