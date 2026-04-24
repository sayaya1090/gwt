package dev.sayaya.gwt

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import kotlin.io.path.createTempDirectory

class GwtHttpAccessibilityTest : DescribeSpec({
    lateinit var project: Project
    beforeEach {
        project = ProjectBuilder.builder().build()
    }
    describe("WebServerService Accessibility") {
        it("실제 WebServerService를 구동하고 HTTP GET 요청에 200 OK 응답을 반환해야 한다") {
            val tempDir = createTempDirectory("gwt-accessibility-test").toFile()
            File(tempDir, "test.html").writeText("<html><body>Success</body></html>")

            val serviceProvider = project.gradle.sharedServices.registerIfAbsent("accessibilityWebServer", WebServerService::class.java) {
                parameters.contentRoot.from(tempDir)
                parameters.port.set(0)
            }

            val service = serviceProvider.get()
            val port = service.getPort()

            try {
                val url = URI.create("http://127.0.0.1:$port/test.html").toURL()
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                
                val responseCode = connection.responseCode
                val content = connection.inputStream.bufferedReader().use { it.readText() }

                responseCode shouldBe 200
                content shouldBe "<html><body>Success</body></html>"
                
                println("Accessibility Test Success: HTTP $responseCode at $url")
            } finally {
                service.close()
                tempDir.deleteRecursively()
            }
        }
    }
})
