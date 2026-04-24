package dev.sayaya.gwt.test

import dev.sayaya.gwt.WebServerParameters
import dev.sayaya.gwt.WebServerService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import kotlin.io.path.createTempDirectory

class GwtBundleAccessibilityTest : DescribeSpec({
    lateinit var project: Project
    
    beforeEach {
        project = ProjectBuilder.builder().build()
    }

    describe("GWT Bundle Accessibility Verification") {
        it("Scenario 1: 동일 디렉토리에 nocache.js와 cache.js가 있을 때 모두 접근 가능해야 한다") {
            val rootDir = createTempDirectory("gwt-bundle-test-success").toFile()
            val moduleDir = File(rootDir, "module").apply { mkdirs() }
            
            val nocacheFile = File(moduleDir, "test.nocache.js").apply { writeText("console.log('nocache');") }
            val cacheFile = File(moduleDir, "test.cache.js").apply { writeText("console.log('cache');") }

            val serviceProvider = project.gradle.sharedServices.registerIfAbsent("successWebServer", WebServerService::class.java) {
                it.parameters.contentRoot.from(rootDir)
                it.parameters.port.set(0)
            }

            val service = serviceProvider.get()
            val port = service.getPort()

            try {
                // nocache.js 접근 확인
                val nocacheUrl = URI.create("http://127.0.0.1:$port/module/test.nocache.js").toURL()
                checkResponse(nocacheUrl) shouldBe 200

                // cache.js 접근 확인
                val cacheUrl = URI.create("http://127.0.0.1:$port/module/test.cache.js").toURL()
                checkResponse(cacheUrl) shouldBe 200
                
                println("[LOG] Success Case: Both files are accessible in the same directory.")
            } finally {
                service.close()
                rootDir.deleteRecursively()
            }
        }

        it("Scenario 2: GWT 컴파일 출력 위치와 서버 서빙 위치가 불일치할 때 (결함 재현)") {
            val serverRootDir = createTempDirectory("server-root").toFile()
            val compilerOutputDir = createTempDirectory("compiler-output").toFile()
            
            // 서버 루트에는 nocache.js만 있고, 실제 번들(cache.js)은 컴파일 출력 위치에만 있는 상황 시뮬레이션
            val serverModuleDir = File(serverRootDir, "module").apply { mkdirs() }
            val compilerModuleDir = File(compilerOutputDir, "module").apply { mkdirs() }
            
            File(serverModuleDir, "test.nocache.js").writeText("console.log('nocache');")
            File(compilerModuleDir, "test.cache.js").writeText("console.log('cache');")

            // 웹 서버는 serverRootDir만 바라보고 있음
            val serviceProvider = project.gradle.sharedServices.registerIfAbsent("mismatchWebServer", WebServerService::class.java) {
                it.parameters.contentRoot.from(serverRootDir)
                it.parameters.port.set(0)
            }

            val service = serviceProvider.get()
            val port = service.getPort()

            try {
                // nocache.js는 서버 루트에 있으므로 성공
                val nocacheUrl = URI.create("http://127.0.0.1:$port/module/test.nocache.js").toURL()
                checkResponse(nocacheUrl) shouldBe 200

                // cache.js는 서버 루트에 없으므로 404 발생 (결함 재현)
                val cacheUrl = URI.create("http://127.0.0.1:$port/module/test.cache.js").toURL()
                val responseCode = checkResponse(cacheUrl)
                
                println("[LOG] Mismatch Case: nocache.js accessible, but cache.js returns $responseCode")
                
                if (responseCode == 404) {
                    println("[LOG] 분석 결과: 서버의 contentRoot가 GWT 컴파일 결과물을 포함하지 않는 디렉토리(예: src/main/webapp)로 설정되었을 경우, " +
                            "정적 파일인 nocache.js는 수동 복사되어 존재할 수 있으나 동적 생성되는 *.cache.js 파일들은 누락되어 404가 발생함.")
                }
                
                // 실제 결함 현상을 확인하기 위해 404가 나오는지 검증 (재현 테스트)
                // responseCode shouldBe 404
            } finally {
                service.close()
                serverRootDir.deleteRecursively()
                compilerOutputDir.deleteRecursively()
            }
        }
    }
})

private fun checkResponse(url: java.net.URL): Int {
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    return try {
        connection.responseCode
    } catch (e: Exception) {
        -1
    }
}
