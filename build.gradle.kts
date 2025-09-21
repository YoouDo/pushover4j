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
    id("org.sonarqube") version "6.3.1.5724"
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
    testRuntimeOnly(libs.slf4j.simple)
    testImplementation(libs.jupiter.api)
    testRuntimeOnly(libs.jupiter.engine)
    testRuntimeOnly(libs.junit.platform)

    testImplementation(libs.wiremock.jre8)
    testImplementation(libs.json.path)
    testImplementation(libs.hamcrest)
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

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
    }
}
