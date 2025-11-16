plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version "2.2.0"
    id("maven-publish")
    id("com.gradle.plugin-publish") version "2.0.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.2"
}

allprojects {
    group = "dev.sayaya"
    version = "2.2.7"

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
}
dependencies {
    implementation("org.docstr.gwt:org.docstr.gwt.gradle.plugin:$version")
    implementation("io.ktor:ktor-server-core:3.3.1")
    implementation("io.ktor:ktor-server-netty:3.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    // 테스트 구현에 필요한 의존성
    testImplementation("io.kotest:kotest-runner-junit5:6.0.4")
    kover(project(":test"))
}
gradlePlugin {
    website = "https://github.com/sayaya1090/gwt"
    vcsUrl = "https://github.com/sayaya1090/gwt.git"

    plugins {
        register("gwtPlugin") {
            id = "${project.group}.gwt"
            implementationClass = "${project.group}.gwt.GwtPlugin"
            version = project.version
            displayName = "GWT Gradle Plugin"
            description = "Extends org.docstr.gwt plugin with automatic Lombok support, built-in web server management for tests, and HTML host file generation for GWT development"
            tags = listOf("gwt", "java", "web", "lombok", "test", "selenium")
        }
        register("gwtLombokPlugin") {
            id = "${project.group}.gwt.lombok"
            implementationClass = "${project.group}.gwt.GwtLombokPlugin"
            version = project.version
            displayName = "GWT Lombok Plugin"
            description = "Automatically configures Lombok Java Agent for GWT compilation, enabling seamless use of Lombok annotations in GWT projects"
            tags = listOf("gwt", "java", "lombok", "annotation-processor")
        }
        register("gwtTestPlugin") {
            id = "${project.group}.gwt.test"
            implementationClass = "${project.group}.gwt.GwtTestPlugin"
            version = project.version
            displayName = "GWT Test Plugin"
            description = "Simplifies GWT testing with automatic web server lifecycle management, HTML test file generation, and kotest+selenium integration"
            tags = listOf("gwt", "java", "test", "selenium", "testing", "kotest")
        }
    }
}
tasks {
    test {
        useJUnitPlatform()
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
}
kover {
    reports {
        verify {
            rule {
                disabled = false
                bound {
                    minValue = 80
                }
            }
        }
    }
}