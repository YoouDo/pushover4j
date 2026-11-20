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

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])

            pom {
                name.set("pushover client")
                description.set("A Java client for Pushover messaging")
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
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:YoouDo/Pushover4j.git")
                    developerConnection.set("scm:git:git@github.com:YoouDo/Pushover4j.git")
                    url.set("git@github.com:YoouDo/Pushover4j.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("OSSRH_USER") ?: return@credentials
                password = System.getenv("OSSRH_PASSWORD") ?: return@credentials
            }
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
