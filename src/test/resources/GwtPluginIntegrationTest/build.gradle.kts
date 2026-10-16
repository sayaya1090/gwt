plugins {
    kotlin("jvm") version "2.4.21"
    id("dev.sayaya.gwt")
}

repositories {
    mavenCentral()
}

version = "1.0.0"
group = "com.example"

dependencies {
    implementation("com.google.elemental2:elemental2-dom:1.3.2")

    implementation("org.projectlombok:lombok:1.18.48")
    annotationProcessor("org.projectlombok:lombok:1.18.48")

    testImplementation("io.kotest:kotest-runner-junit5:6.2.6")
    testImplementation("com.microsoft.playwright:playwright:1.63.0")
}

tasks {
    gwt {
        gwtVersion = "2.13.1"
        modules = listOf("com.example.App")
        war = file("src/main/webapp")
        sourceLevel = "auto"
        devMode {
            modules = listOf("com.example.Test")
            war = file("src/test/webapp")
        }
    }
    test {
        useJUnitPlatform()
    }
}