package dev.sayaya.gwt

import dev.sayaya.gwt
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.docstr.gwt.GwtPluginExtension
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.testfixtures.ProjectBuilder
import java.net.ConnectException
import java.net.ServerSocket
import java.net.URL
import kotlin.io.path.createTempDirectory

class GwtTestPluginTest : DescribeSpec({
    lateinit var project: Project
    val appModuleName = "com.example.App"
    val testModuleName = "com.example.TestModule"

    beforeEach {
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply(GwtTestPlugin::class.java)

        // gwtCompile 태스크가 생성되도록 GWT 모듈을 설정합니다.
        val gwtExtension = project.extensions.getByType(GwtPluginExtension::class.java)
        gwtExtension.modules.set(listOf(appModuleName))
        gwtExtension.devMode.modules.set(listOf(testModuleName))
    }

    describe("GwtTestPlugin 적용 시") {
        it("java 플러그인을 적용해야 한다") {
            project.plugins.hasPlugin("java") shouldBe true
        }
        it("는 'org.docstr.gwt' 플러그인을 적용해야 한다") {
            project.plugins.hasPlugin("org.docstr.gwt") shouldBe true
        }

        it("는 모든 JavaCompile 태스크의 인코딩을 UTF-8로 설정해야 한다") {
            val javaCompileTask = project.tasks.getByName("compileJava") as JavaCompile
            javaCompileTask.options.encoding shouldBe "UTF-8"
        }
        it("'compileJava' 태스크는 'processTestResources' 태스크에 의존해야 한다") {
            val task = project.tasks.getByName("compileJava")
            val dependencyNames = task.taskDependencies.getDependencies(task).map { it.name }
            dependencyNames shouldContain "processTestResources"
        }

        context("'gwtTestCompile' 태스크") {
            it("는 GwtTestCompileTask 타입으로 등록되어야 한다") {
                project.tasks.getByName("gwtTestCompile").shouldBeInstanceOf<GwtTestCompileTask>()
            }

            it("는 modules 프로퍼티가 gwt.devMode.modules 값으로 설정되어야 한다") {
                // 태스크가 등록될 때 설정 액션이 실행되므로, 다시 조회해야 최신 설정이 반영됩니다.
                val task = project.tasks.getByName("gwtTestCompile") as GwtTestCompileTask
                task.modules.get() shouldContain testModuleName
            }
        }
        context("'gwtDevMode' 태스크") {
            it("는 'gwtGenerateTestHtml' 태스크에 의존해야 한다") {
                val task = project.tasks.getByName("gwtDevMode")
                val dependencyNames = task.taskDependencies.getDependencies(task).map { it.name }
                dependencyNames shouldContain "gwtGenerateTestHtml"
            }
        }
        context("'test' 태스크") {
            it("는 'openWebServer' 태스크에 의존해야 한다") {
                val task = project.tasks.getByName("test")
                val dependencyNames = task.taskDependencies.getDependencies(task).map { it.name }
                dependencyNames shouldContain "openWebServer"
            }
            it("는 'closeWebServer' 태스크로 종료되어야 한다") {
                val task = project.tasks.getByName("test")
                val finalizerNames = task.finalizedBy.getDependencies(task).map { it.name }
                finalizerNames shouldContain "closeWebServer"
            }
        }
        context("'war' 플러그인과 함께 적용 시") {
            it("'war' 태스크는 'test' 태스크에 의존해야 한다") {
                // 'war' 플러그인을 적용합니다.
                project.pluginManager.apply("war")

                val warTask = project.tasks.getByName("war")
                val dependencyNames = warTask.taskDependencies.getDependencies(warTask).map { it.name }
                dependencyNames shouldContain "test"
            }
        }

        context("'openWebServer' 태스크") {
            it("는 webserverPort의 기본값으로 8080을 사용해야 한다") {
                project.tasks.getByName("test")
                val task = project.tasks.getByName("openWebServer") as WebServerTask
                task.webserverPort.get() shouldBe 8080
            }

            it("는 test.gwt.webPort에 설정된 값을 사용해야 한다") {
                val testTask = project.tasks.getByName("test") as Test
                testTask.gwt {
                    webPort.set(9999)
                }
                val task = project.tasks.getByName("openWebServer") as WebServerTask
                task.webserverPort.get() shouldBe 9999
            }
            it("의 webserverPath는 gwt.war 값을 사용해야 한다") {
                // 준비 (Arrange)
                val expectedWarDir = project.file("src/main/webapp")
                val gwtExtension = project.extensions.getByType(GwtPluginExtension::class.java)
                gwtExtension.war.set(expectedWarDir)

                // 실행 (Act)
                val task = project.tasks.getByName("openWebServer") as WebServerTask

                // 검증 (Assert)
                task.webserverPath.get() shouldBe expectedWarDir
            }
        }
        context("'closeWebServer' 태스크") {
            it("는 'openWebServer' 태스크로 시작된 웹서버를 종료해야 한다") {
                // Arrange
                val openWebServerTask = project.tasks.getByName("openWebServer") as WebServerTask
                val closeWebServerTask = project.tasks.getByName("closeWebServer")

                val tempDir = createTempDirectory("webserver-test-close").toFile()
                val port = ServerSocket(0).use { it.localPort }

                openWebServerTask.webserverPort.set(port)
                openWebServerTask.webserverPath.set(tempDir)

                try {
                    // Act 1: Start the server
                    openWebServerTask.exec()
                    openWebServerTask.isRunning() shouldBe true

                    // Act 2: Execute the closeWebServer task's actions
                    closeWebServerTask.actions.forEach { action -> action.execute(closeWebServerTask) }
                    // Assert: Server is stopped, and port is released
                    openWebServerTask.isRunning() shouldBe false
                } finally {
                    // Clean up in case of test failure before close
                    if (openWebServerTask.isRunning()) {
                        openWebServerTask.close()
                    }
                    tempDir.deleteRecursively()
                }
            }
        }
    }
})
