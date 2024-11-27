plugins {
    java
    kotlin("jvm")
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "org.varamadon"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.0.0.202409031743-r")
    implementation("com.google.guava:guava:33.3.1-jre")
    implementation("io.ktor:ktor-server-core:2.3.0")
    implementation("io.ktor:ktor-server-netty:2.3.0")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")
    implementation(project(":auto-refactor-shared"))
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        bundledPlugin("com.intellij.java")
        instrumentationTools()
    }
}

configurations.all {
    exclude("org.slf4j")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks {
    runIde {
        val projectLocalPath: String? by project
        val repositoryUrl: String? by project
        val username: String? by project
        val accessToken: String? by project
        args = listOfNotNull(
            "autorefactor",
            projectLocalPath,
            repositoryUrl,
            username,
            accessToken
        )
        jvmArgs = listOf(
            "-Djava.awt.headless=true", "-Djdk.module.illegalAccess.silent=true",
            "--add-exports", "java.base/jdk.internal.vm=ALL-UNNAMED",
            "-Didea.is.internal=false"
        )
        maxHeapSize = "12g"
    }
    register("runAutorefactor") {
        dependsOn(runIde)
    }
}

