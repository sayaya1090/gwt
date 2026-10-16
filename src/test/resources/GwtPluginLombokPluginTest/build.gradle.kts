plugins {
    kotlin("jvm") version "2.4.21"
    id("dev.sayaya.gwt.lombok")
}

repositories {
    mavenCentral()
}

version = "1.0.0"
group = "com.example"

dependencies {
    implementation("org.projectlombok:lombok:1.18.48")
    annotationProcessor("org.projectlombok:lombok:1.18.48")
    testImplementation("io.kotest:kotest-runner-junit5:6.2.6")
}
tasks {
    gwt {
        gwtVersion = "2.13.1"
        modules = listOf("com.example.App")
        war = file("src/main/webapp")
        sourceLevel = "auto"
    }
    test {
        useJUnitPlatform()
        failOnNoDiscoveredTests = false
    }
}