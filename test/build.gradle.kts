plugins {
    kotlin("jvm")
    id("maven-publish")
    signing
    id("org.jetbrains.kotlinx.kover")
    id("com.vanniktech.maven.publish") version "0.35.0"
}

repositories {
    mavenCentral()
}

dependencies {
    // Selenium WebDriver
    api("org.seleniumhq.selenium:selenium-java:4.27.0")
    api("org.seleniumhq.selenium:selenium-chrome-driver:4.27.0")

    // Kotest
    api("io.kotest:kotest-runner-junit5:6.0.4")
    api("io.kotest:kotest-assertions-core:6.0.4")

    // Kotlin
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api("com.google.code.gson:gson:2.13.2")
}

tasks {
    test {
        useJUnitPlatform()
    }
}

signing {
    val signingKey = System.getenv("GPG_PRIVATE_KEY")
    val signingPassword = System.getenv("GPG_PASSWORD")
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    } else {
        useGpgCmd()
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/sayaya1090/maven")
            credentials {
                username = project.findProperty("github_username") as String? ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("github_password") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "gwt-test", version.toString())

    pom {
        name.set("GWT Test Library")
        description.set("Kotest and Selenium integration library for GWT testing with automatic ChromeDriver setup and console log verification")
        url.set("https://github.com/sayaya1090/gwt")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("sayaya1090")
                name.set("sayaya")
                email.set("sayaya1090@gmail.com")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/sayaya1090/gwt.git")
            developerConnection.set("scm:git:ssh://github.com/sayaya1090/gwt.git")
            url.set("https://github.com/sayaya1090/gwt")
        }
    }
}