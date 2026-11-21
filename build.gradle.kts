import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    `java-library`
    `maven-publish`
    signing
    jacoco
    idea
    id("com.vanniktech.maven.publish") version "0.37.0"
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
        languageVersion = JavaLanguageVersion.of(21)
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
    testImplementation(libs.wiremock)
    testImplementation(libs.json.path)
    testImplementation(libs.hamcrest)
    testImplementation(libs.awaitility)

    testRuntimeOnly(libs.slf4j.simple)
    testRuntimeOnly(libs.jupiter.engine)
    testRuntimeOnly(libs.junit.platform)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

mavenPublishing {
    coordinates(project.group.toString(), "pushover4j", project.version.toString())

    configure(
        JavaLibrary(
            javadocJar = JavadocJar.None(),
            sourcesJar = SourcesJar.Sources(),
        )
    )

    pom {
        name.set("pushover client")
        description.set("A Java client for Pushover messaging")
        inceptionYear.set("2026")
        url.set("https://github.com/YoouDo/Pushover4j")
        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("YoouDo")
                name.set("Udo Kleinkop")
                email.set("udo@kleinkop.de")
                url.set("https://github.com/YoouDo")
            }
        }
        scm {
            connection.set("scm:git:git@github.com:YoouDo/Pushover4j.git")
            developerConnection.set("scm:git:git@github.com:YoouDo/Pushover4j.git")
            url.set("https://github.com/YoouDo/Pushover4j")
        }
    }

    publishToMavenCentral()
    signAllPublications()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
    }
}
