plugins {
    kotlin("jvm")
    id("maven-publish")
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
    implementation(kotlin("stdlib"))

    implementation("com.google.code.gson:gson:2.13.2")
}

tasks {
    test {
        useJUnitPlatform()
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "gwt-test"
        }
    }
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
