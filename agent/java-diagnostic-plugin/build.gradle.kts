import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.24"
  id("org.jetbrains.intellij.platform") version "2.2.0"
}

group = "com.microsoft.azure.agent.plugin"
//version = "1.0-SNAPSHOT"
version = providers.gradleProperty("projectVersion").get() // Plugin version
val ideaVersion = providers.gradleProperty("platformVersion").get()

val javaVersion = 17

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(javaVersion)
  }
  sourceCompatibility = JavaVersion.toVersion(javaVersion)
  targetCompatibility = JavaVersion.toVersion(javaVersion)
}



dependencies {
  intellijPlatform {
//    create(IntelliJPlatformType.IntellijIdeaCommunity, "platformVersion")
    bundledPlugins(listOf("com.intellij.java"))
    intellijIdeaCommunity(ideaVersion)

//    bundledPlugin("java")

    pluginVerifier()
  }
  implementation("io.kubernetes:client-java:19.0.1")
}

intellijPlatform {
  pluginConfiguration {
    ideaVersion {
        sinceBuild = providers.gradleProperty("pluginSinceBuild")
        untilBuild = provider { null }
    }

    productDescriptor {
        eap = true
    }
  }

  publishing {
    token = providers.gradleProperty("jetBrainsToken")
    channels = providers.gradleProperty("jetBrainsChannel").map { listOf(it) }
  }

  pluginVerification {
    ides {
      ide(IntelliJPlatformType.IntellijIdeaCommunity, ideaVersion)
    }
    freeArgs = listOf(
      "-mute",
      "TemplateWordInPluginId,TemplateWordInPluginName"
    )
  }
}

tasks {
  // Set the JVM compatibility versions
//  withType<JavaCompile> {
//    sourceCompatibility = "17"
//    targetCompatibility = "17"
//  }
//  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//    kotlinOptions.jvmTarget = "17"
//  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}
