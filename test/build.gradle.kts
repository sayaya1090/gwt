plugins {
    kotlin("jvm")
    id("maven-publish")
}
dependencies {
    implementation("io.kotest:kotest-runner-junit5:5.8.1")
    implementation("org.seleniumhq.selenium:selenium-java:4.18.1")
    implementation("org.slf4j:slf4j-simple:2.0.12")
}
tasks {
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
        publications {
            register("maven", MavenPublication::class) {
                groupId = "net.sayaya"
                artifactId = "gwt-test"
                version = "1.1"
                from(project.components["java"])
            }
        }
    }
}