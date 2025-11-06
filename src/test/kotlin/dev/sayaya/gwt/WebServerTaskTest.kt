package dev.sayaya.gwt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.net.BindException
import java.net.ConnectException
import java.net.ServerSocket
import java.net.URI
import kotlin.io.path.createTempDirectory

class WebServerTaskTest : DescribeSpec({
    describe("WebServerTask") {
        it("웹 서버를 시작하고 정적 파일을 올바르게 제공해야 한다") {
            val project: Project = ProjectBuilder.builder().build()
            val task = project.tasks.register("webServer", WebServerTask::class.java).get()

            // 임시 디렉토리 및 테스트 파일 생성
            val tempDir = createTempDirectory("webserver-test").toFile()
            File(tempDir, "index.html").apply {
                writeText("Hello from WebServerTask!")
            }

            // 사용 가능한 포트 동적 할당
            val port = ServerSocket(0).use { it.localPort }

            // 태스크의 입력 프로퍼티 설정
            task.webserverPort.set(port)
            task.webserverPath.set(tempDir)

            try {
                // exec()는 서버가 준비될 때까지 대기합니다
                task.exec()

                // 서버가 정상적으로 시작되었는지 확인
                task.isRunning() shouldBe true

                // HTTP 요청을 보내 파일 내용을 가져옵니다.
                val response = URI.create("http://localhost:$port/index.html").toURL().readText()
                response shouldBe "Hello from WebServerTask!"

            } finally {
                task.close()
                tempDir.deleteRecursively()
            }
        }

        it("유효하지 않은 포트로 서버를 시작하면 예외를 던져야 한다") {
            val project: Project = ProjectBuilder.builder().build()
            val task = project.tasks.register("webServer", WebServerTask::class.java).get()

            // 유효하지 않은 포트 (음수)
            task.webserverPort.set(-1)
            val tempDir = createTempDirectory("webserver-test").toFile()
            task.webserverPath.set(tempDir)

            try {
                // exec() 호출 시 예외가 발생해야 함
                shouldThrow<Exception> {
                    task.exec()
                }

                // 서버가 시작되지 않았음을 확인
                task.isRunning() shouldBe false
            } finally {
                tempDir.deleteRecursively()
            }
        }
        it("이미 사용 중인 포트로 서버를 시작하면 예외를 던져야 한다") {
            val project: Project = ProjectBuilder.builder().build()

            // 포트를 먼저 점유
            val socket = ServerSocket(0)
            val occupiedPort = socket.localPort

            val task = project.tasks.register("webServer", WebServerTask::class.java).get()
            val tempDir = createTempDirectory("webserver-test").toFile()
            File(tempDir, "index.html").writeText("test")

            task.webserverPort.set(occupiedPort)
            task.webserverPath.set(tempDir)

            try {
                // 포트가 이미 사용 중이므로 예외가 발생해야 함
                val exception = shouldThrow<Throwable> {
                    task.exec()
                }

                // BindException 또는 그 원인이 BindException인지 확인
                val isBindException = exception is BindException ||
                        exception.cause is BindException ||
                        exception.message?.contains("Address already in use") == true
                isBindException shouldBe true

                // 서버가 시작되지 않았음을 확인
                task.isRunning() shouldBe false

                // 원래 소켓은 여전히 열려있어야 함
                socket.isClosed shouldBe false

            } finally {
                socket.close()
                task.close()
                tempDir.deleteRecursively()
            }
        }
        context("isRunning()") {
            it("exec() 호출 전에는 false를 반환해야 한다") {
                val project: Project = ProjectBuilder.builder().build()
                val task = project.tasks.register("webServer", WebServerTask::class.java).get()

                val port = ServerSocket(0).use { it.localPort }
                task.webserverPort.set(port)
                task.webserverPath.set(createTempDirectory("webserver-test").toFile())

                // exec() 호출 전
                task.isRunning() shouldBe false
            }
            it("서버가 성공적으로 시작되면 true를 반환해야 한다") {
                val project: Project = ProjectBuilder.builder().build()
                val task = project.tasks.register("webServer", WebServerTask::class.java).get()

                val tempDir = createTempDirectory("webserver-test").toFile()
                val port = ServerSocket(0).use { it.localPort }
                task.webserverPort.set(port)
                task.webserverPath.set(tempDir)

                try {
                    task.exec()

                    // 서버 시작 후
                    task.isRunning() shouldBe true
                } finally {
                    task.close()
                    tempDir.deleteRecursively()
                }
            }

            it("서버 시작 실패 시 false를 반환해야 한다") {
                val project: Project = ProjectBuilder.builder().build()
                val task = project.tasks.register("webServer", WebServerTask::class.java).get()

                task.webserverPort.set(-1)
                task.webserverPath.set(createTempDirectory("webserver-test").toFile())

                try {
                    shouldThrow<Exception> {
                        task.exec()
                    }
                } catch (e: Exception) {
                    // 예외 무시
                }

                // 서버 시작 실패 후
                task.isRunning() shouldBe false
            }
        }

        context("close()") {
            it("실행 중인 서버를 정상적으로 종료해야 한다") {
                val project: Project = ProjectBuilder.builder().build()
                val task = project.tasks.register("webServer", WebServerTask::class.java).get()

                val tempDir = createTempDirectory("webserver-test").toFile()
                val port = ServerSocket(0).use { it.localPort }
                task.webserverPort.set(port)
                task.webserverPath.set(tempDir)

                try {
                    task.exec()
                    task.isRunning() shouldBe true

                    // 서버 종료
                    task.close()

                    // 잠시 대기 후 연결 시도 시 실패해야 함
                    Thread.sleep(500)
                    shouldThrow<ConnectException> {
                        URI.create("http://localhost:$port/index.html").toURL().openStream()
                    }
                } finally {
                    tempDir.deleteRecursively()
                }
            }
            it("exec()를 호출하지 않은 상태에서 close()를 호출해도 예외가 발생하지 않아야 한다") {
                val project: Project = ProjectBuilder.builder().build()
                val task = project.tasks.register("webServer", WebServerTask::class.java).get()

                val port = ServerSocket(0).use { it.localPort }
                task.webserverPort.set(port)
                task.webserverPath.set(createTempDirectory("webserver-test").toFile())

                // exec() 호출 없이 close() 호출
                // 예외가 발생하지 않아야 함
                task.close()

                task.isRunning() shouldBe false
            }
            it("여러 번 호출해도 안전해야 한다") {
                val project: Project = ProjectBuilder.builder().build()
                val task = project.tasks.register("webServer", WebServerTask::class.java).get()

                val tempDir = createTempDirectory("webserver-test").toFile()
                val port = ServerSocket(0).use { it.localPort }
                task.webserverPort.set(port)
                task.webserverPath.set(tempDir)

                try {
                    task.exec()
                    task.isRunning() shouldBe true

                    // 여러 번 close() 호출
                    task.close()
                    task.close()
                    task.close()

                    // 예외가 발생하지 않아야 함
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }
    }
})
