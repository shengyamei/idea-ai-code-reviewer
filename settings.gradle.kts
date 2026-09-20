import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories {
        // 国内镜像优先，解决外网下载超时问题
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/public")
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.jetbrains.intellij.platform") version "2.16.0"
    }
}

rootProject.name = "idea-ai-code-reviewer"

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.16.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        // 国内镜像优先，解决外网下载超时问题
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/jetbrains")
        mavenCentral()

        intellijPlatform {
            defaultRepositories()
        }
    }
}
