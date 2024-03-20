plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.23"
}
repositories {
    mavenCentral()
}
dependencies {
    implementation("org.wisepersist:gwt-gradle-plugin:1.1.19")
    implementation("io.ktor:ktor-server-core:2.3.9")
    implementation("io.ktor:ktor-server-netty:2.3.9")
    implementation("ch.qos.logback:logback-classic:1.5.3")
}
gradlePlugin {
    plugins {
        create("simplePlugin") {
            id = "net.sayaya.gwt"
            implementationClass = "net.sayaya.GwtPlugin"
            version = "1.1.19"
        }
    }
}