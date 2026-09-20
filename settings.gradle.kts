import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
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
        mavenCentral()

        intellijPlatform {
            defaultRepositories()
        }
    }
}
