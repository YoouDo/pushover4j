buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    `java-library`
    `maven-publish`
    signing
    jacoco
    idea
    alias(libs.plugins.sonarqube)
}

group = "de.kleinkop"
version = "1.0.0"

repositories {
    mavenCentral()
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withJavadocJar()
    withSourcesJar()
}

idea {
    module {
        inheritOutputDirs = false
        outputDir = layout.buildDirectory.dir("classes/java/main").get().asFile
        testOutputDir = layout.buildDirectory.dir("classes/java/test").get().asFile
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

dependencies {
    api(libs.slf4j.api)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.jsr310)

    // testing
    testImplementation(libs.jupiter.api)
    testImplementation(libs.wiremock.jre8)
    testImplementation(libs.json.path)
    testImplementation(libs.hamcrest)
    testImplementation(libs.awaitility)

    testRuntimeOnly(libs.slf4j.simple)
    testRuntimeOnly(libs.jupiter.engine)
    testRuntimeOnly(libs.junit.platform)
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit Jupiter test framework
            useJUnitJupiter(libs.versions.junit)
        }
    }
}

signing {
    val key = System.getenv("SIGNING_KEY") ?: return@signing
    val password = System.getenv("SIGNING_PASSWORD") ?: return@signing
    val publishing: PublishingExtension by project

    useInMemoryPgpKeys(key, password)
    sign(publishing.publications["mavenJava"])
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
    }
}
