plugins {
    kotlin("jvm") version "1.9.23"
}
repositories {
    mavenCentral()
}
dependencies {
    implementation("io.kotest:kotest-runner-junit5:5.8.1")
    implementation("org.slf4j:slf4j-simple:2.0.12")
}
tasks {
    gwtTest {
        modules = listOf("net.sayaya.blah.Test", "net.sayaya.blah.Index")
        launcherDir = file("build/resources/test/static")
        extraJvmArgs = listOf("-javaagent:${lombok}=ECJ")
        webserverPort = 8080
        port = 8081
        src += files(File("src/test/java"))
    }
}