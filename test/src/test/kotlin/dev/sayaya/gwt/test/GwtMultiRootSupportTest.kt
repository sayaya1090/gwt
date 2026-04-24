package dev.sayaya.gwt.test

import dev.sayaya.gwt.WebServerService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import kotlin.io.path.createTempDirectory

class GwtMultiRootSupportTest : DescribeSpec({
    lateinit var project: Project
    
    beforeEach {
        project = ProjectBuilder.builder().build()
    }

    describe("WebServerService Multi-Root Support") {
        it("서로 다른 두 디렉토리를 contentRoot에 추가했을 때, 두 곳의 파일에 모두 접근 가능해야 한다") {
            val dir1 = createTempDirectory("multi-root-1").toFile()
            val dir2 = createTempDirectory("multi-root-2").toFile()
            
            val file1 = File(dir1, "file1.txt").apply { writeText("Content from Dir 1") }
            val file2 = File(dir2, "file2.txt").apply { writeText("Content from Dir 2") }

            val serviceProvider = project.gradle.sharedServices.registerIfAbsent("multiRootWebServer", WebServerService::class.java) {
                // 다중 루트 설정
                it.parameters.contentRoot.from(dir1)
                it.parameters.contentRoot.from(dir2)
                it.parameters.port.set(0)
            }

            val service = serviceProvider.get()
            val port = service.getPort()

            try {
                // 첫 번째 디렉토리 파일 접근 확인
                val url1 = URI.create("http://127.0.0.1:$port/file1.txt").toURL()
                val conn1 = url1.openConnection() as HttpURLConnection
                conn1.responseCode shouldBe 200
                conn1.inputStream.bufferedReader().use { it.readText() } shouldBe "Content from Dir 1"

                // 두 번째 디렉토리 파일 접근 확인
                val url2 = URI.create("http://127.0.0.1:$port/file2.txt").toURL()
                val conn2 = url2.openConnection() as HttpURLConnection
                conn2.responseCode shouldBe 200
                conn2.inputStream.bufferedReader().use { it.readText() } shouldBe "Content from Dir 2"
                
                println("[LOG] Multi-root verification success: Both files from different roots are accessible.")
            } finally {
                service.close()
                dir1.deleteRecursively()
                dir2.deleteRecursively()
            }
        }
    }
})
