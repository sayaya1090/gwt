plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.20"
    id("maven-publish")
}
group = "net.sayaya"
version = "1.1.19"
repositories {
    mavenCentral()
}
dependencies {
    implementation("org.wisepersist:gwt-gradle-plugin:1.1.19")
    implementation("io.ktor:ktor-server-core:2.3.9")
    implementation("io.ktor:ktor-server-netty:2.3.9")
    implementation("io.kotest:kotest-runner-junit5:5.8.1")
}
gradlePlugin {
    plugins {
        create("simplePlugin") {
            id = "net.sayaya.gwt"
            implementationClass = "net.sayaya.gwt.GwtPlugin"
            version = "1.1.19"
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
                    username = project.findProperty("github_username") as String
                    password = project.findProperty("github_password") as String
                }
            }
        }
    }
}

subprojects {
    group = "net.sayaya"
    version = "1.0"
    repositories {
        mavenCentral()
    }
}