package dev.sayaya.gwt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.net.BindException
import java.net.ConnectException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import kotlin.io.path.createTempDirectory

class WebServerServiceTest : DescribeSpec({
    lateinit var project: Project
    beforeEach {
        project = ProjectBuilder.builder().build()
    }
    describe("WebServerService") {
        it("웹 서버를 시작하고 정적 파일을 올바르게 제공해야 한다") {
            // 임시 디렉토리 및 테스트 파일 생성
            val tempDir = createTempDirectory("webserver-service-test").toFile()
            File(tempDir, "index.html").apply {
                writeText("Hello from WebServerService!")
            }

            // 빌드 서비스 등록
            val serviceProvider = project.gradle.sharedServices.registerIfAbsent("testWebServer", WebServerService::class.java) {
                parameters.contentRoot.from(tempDir)
                parameters.port.set(0) // 랜덤 포트
            }

            // 서비스 인스턴스화 (이 시점에 서버 시작)
            val service = serviceProvider.get()

            try {
                // 포트가 할당되었는지 확인
                val port = service.getPort()
                port shouldNotBe -1
                port shouldNotBe 0

                // HTTP 요청을 보내 파일 내용을 가져옵니다.
                val response = URI.create("http://localhost:$port/index.html").toURL().readText()
                response shouldBe "Hello from WebServerService!"

            } finally {
                service.close()
                tempDir.deleteRecursively()
            }
        }

        it("유효하지 않은 포트로 서버를 시작하면 예외를 던져야 한다") {
            val tempDir = createTempDirectory("webserver-service-test").toFile()

            val serviceProvider = project.gradle.sharedServices.registerIfAbsent("invalidPortServer", WebServerService::class.java) {
                parameters.contentRoot.from(tempDir)
                parameters.port.set(-1) // 유효하지 않은 포트
            }

            try {
                // 서비스 인스턴스 생성 시 init 블록에서 startServer()가 호출되므로 여기서 예외 발생
                shouldThrow<Exception> {
                    serviceProvider.get()
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }

        it("이미 사용 중인 포트로 서버를 시작하면 예외를 던져야 한다") {
            // 포트를 먼저 점유
            val socket = ServerSocket(28080, 50, InetAddress.getByName("127.0.0.1"))
            val occupiedPort = socket.localPort
            val tempDir = createTempDirectory("webserver-service-test").toFile()

            val serviceProvider = project.gradle.sharedServices.registerIfAbsent("occupiedPortServer", WebServerService::class.java) {
                parameters.contentRoot.from(tempDir)
                parameters.port.set(occupiedPort)
            }

            try {
                // 포트가 이미 사용 중이므로 예외가 발생해야 함
                val exception = shouldThrow<Exception> {
                    serviceProvider.get()
                }

                // BindException 또는 그 원인이 BindException인지 확인
                val isBindException = generateSequence(exception as Throwable) { it.cause }
                    .any { it is BindException || it.message?.contains("Address already in use") == true }

                isBindException shouldBe true

                // 원래 소켓은 여전히 열려있어야 함
                socket.isClosed shouldBe false

            } finally {
                socket.close()
                tempDir.deleteRecursively()
            }
        }

        it("서빙 루트 밖의 파일은 경로 조작(.., %2e%2e)으로도 제공하지 않아야 한다") {
            val base = createTempDirectory("webserver-traversal-test").toFile()
            val root = File(base, "www").apply { mkdirs() }
            File(root, "ok.txt").writeText("PUBLIC")
            File(base, "secret.txt").writeText("TOP-SECRET")

            val service = project.gradle.sharedServices.registerIfAbsent("traversalServer", WebServerService::class.java) {
                parameters.contentRoot.from(root)
                parameters.port.set(0)
            }.get()

            fun rawGet(path: String): String = java.net.Socket("127.0.0.1", service.getPort()).use { socket ->
                socket.soTimeout = 5000
                socket.getOutputStream().write("GET $path HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n".toByteArray())
                socket.getInputStream().readBytes().toString(Charsets.UTF_8)
            }

            try {
                rawGet("/ok.txt") shouldContain "PUBLIC"
                listOf("/../secret.txt", "/%2e%2e/secret.txt", "/sub/../../secret.txt", "/..%2fsecret.txt").forEach { path ->
                    withClue(path) { rawGet(path) shouldNotContain "TOP-SECRET" }
                }
            } finally {
                service.close()
                base.deleteRecursively()
            }
        }

        listOf("미지정" to null, "0" to 0).forEach { (label, requested) ->
            it("포트가 $label 이면 실제로 바인딩된 포트를 반환하고 접속할 수 있어야 한다") {
                val tempDir = createTempDirectory("webserver-port-test").toFile()
                File(tempDir, "index.html").writeText("PORT-OK")
                val service = project.gradle.sharedServices.registerIfAbsent("portServer-$label", WebServerService::class.java) {
                    parameters.contentRoot.from(tempDir)
                    if (requested != null) parameters.port.set(requested)
                }.get()
                try {
                    val port = service.getPort()
                    (port > 0) shouldBe true
                    URI.create("http://127.0.0.1:$port/index.html").toURL().readText() shouldBe "PORT-OK"
                } finally {
                    service.close()
                    tempDir.deleteRecursively()
                }
            }
        }

        context("close()") {
            it("실행 중인 서버를 정상적으로 종료해야 한다") {
                val tempDir = createTempDirectory("webserver-service-test").toFile()
                File(tempDir, "index.html").writeText("test")

                val serviceProvider = project.gradle.sharedServices.registerIfAbsent("closingServer", WebServerService::class.java) {
                    parameters.contentRoot.from(tempDir)
                    parameters.port.set(0)
                }

                val service = serviceProvider.get()
                val port = service.getPort()

                // 서버 종료
                service.close()

                // 잠시 대기 후 연결 시도 시 실패해야 함
                Thread.sleep(500)
                shouldThrow<ConnectException> {
                    URI.create("http://localhost:$port/index.html").toURL().openStream()
                }

                tempDir.deleteRecursively()
            }

            it("여러 번 호출해도 안전해야 한다") {
                val tempDir = createTempDirectory("webserver-service-test").toFile()

                val serviceProvider = project.gradle.sharedServices.registerIfAbsent("multiCloseServer", WebServerService::class.java) {
                    parameters.contentRoot.from(tempDir)
                    parameters.port.set(0)
                }

                val service = serviceProvider.get()

                try {
                    // 여러 번 close() 호출
                    service.close()
                    service.close()
                    service.close()

                    // 예외가 발생하지 않아야 함
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }
    }
})