plugins {
    kotlin("jvm") version "2.2.21"
    id("dev.sayaya.gwt.lombok")
}

repositories {
    mavenCentral()
}

version = "1.0.0"
group = "com.example"

dependencies {
    implementation("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    testImplementation("io.kotest:kotest-runner-junit5:6.0.4")
}
tasks {
    gwt {
        gwtVersion = "2.12.2"
        modules = listOf("com.example.App")
        war = file("src/main/webapp")
        sourceLevel = "auto"
    }
    test {
        useJUnitPlatform()
        failOnNoDiscoveredTests = false
    }
}