plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version "2.2.21"
    id("maven-publish")
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