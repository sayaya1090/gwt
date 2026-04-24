plugins {
    kotlin("jvm")
    id("maven-publish")
    signing
    id("org.jetbrains.kotlinx.kover")
    id("com.vanniktech.maven.publish") version "0.36.0"
}

repositories {
    mavenCentral()
}

dependencies {
    // Playwright
    api("com.microsoft.playwright:playwright:1.59.0")

    // Kotest
    api("io.kotest:kotest-runner-junit5:6.1.10")
    api("io.kotest:kotest-assertions-core:6.1.10")

    // Kotlin
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api("com.google.code.gson:gson:2.13.2")

    // Gradle Testing
    api(gradleTestKit())

    // Access to WebServerService for reproduction tests
    testImplementation(project(":"))
}

tasks {
    test {
        useJUnitPlatform()
        // 표준 출력 및 표준 에러를 로그에 표시
        testLogging {
            showStandardStreams = true
            events("passed", "skipped", "failed", "standardOut", "standardError")
            showExceptions = true
            showStackTraces = true
            showCauses = true
        }
        // Java 17+에서 ProjectBuilder 사용을 위한 모듈 열기 설정

        jvmArgs(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/java.net=ALL-UNNAMED"
        )
    }
}

signing {
    val signingKey = project.findProperty("signing.secretKey") as String? ?: System.getenv("GPG_PRIVATE_KEY")
    val signingPassword = project.findProperty("signing.passphrase") as String? ?: System.getenv("GPG_PASSWORD")
    useInMemoryPgpKeys(signingKey, signingPassword)
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
        description.set("Kotest and Playwright integration library for GWT testing with console log verification")
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