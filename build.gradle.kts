
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
    id("org.sonarqube") version "5.0.0.4638"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    mavenLocal()
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

idea {
    module {
        inheritOutputDirs = false
        outputDir = file("${layout.buildDirectory}buildDir/classes/java/main")
        testOutputDir = file("${layout.buildDirectory}/classes/java/test")
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

val junitVersion: String by project

dependencies {
    api("org.slf4j:slf4j-api:2.0.12")
    // testing
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.12")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation("com.github.tomakehurst:wiremock-jre8:3.0.1")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit Jupiter test framework
            useJUnitJupiter(junitVersion)
        }
    }
}


