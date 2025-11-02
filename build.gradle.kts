plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("maven-publish")
    id("org.jetbrains.kotlinx.kover") version "0.9.2"
}
val projectVersion = project.findProperty("version") as String? ?: System.getenv("version") as String

group = "dev.sayaya"
version = projectVersion

repositories {
    gradlePluginPortal()
    mavenCentral()
}
dependencies {
    implementation("org.docstr.gwt:org.docstr.gwt.gradle.plugin:$projectVersion")
    implementation("io.ktor:ktor-server-core:3.3.1")
    implementation("io.ktor:ktor-server-netty:3.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    // 테스트 구현에 필요한 의존성
    testImplementation("io.kotest:kotest-runner-junit5:6.0.4")
}
gradlePlugin {
    plugins {
        register("gwtPlugin") {
            id = "${project.group}.gwt"
            implementationClass = "${project.group}.gwt.GwtPlugin"
            version = project.version
        }
        register("gwtLombokPlugin") {
            id = "${project.group}.gwt.lombok"
            implementationClass = "${project.group}.gwt.GwtLombokPlugin"
            version = project.version
        }
        register("gwtTestPlugin") {
            id = "${project.group}.gwt.test"
            implementationClass = "${project.group}.gwt.GwtTestPlugin"
            version = project.version
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