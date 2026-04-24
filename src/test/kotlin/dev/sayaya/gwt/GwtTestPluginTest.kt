package dev.sayaya.gwt

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.docstr.gwt.GwtPluginExtension
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.testfixtures.ProjectBuilder

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

        // 테스트용 더미 디렉토리 설정
        gwtExtension.war.set(project.file("src/main/webapp"))
        gwtExtension.devMode.war.set(project.file("src/test/webapp"))
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
        context("'test' 태스크와 WebServerService") {
            it("WebServerService가 등록되어야 한다") {
                val serviceName = "gwtWebServer-test"
                val registrations = project.gradle.sharedServices.registrations
                registrations.names shouldContain serviceName

                val provider = registrations.getByName(serviceName)
                provider.parameters.shouldBeInstanceOf<WebServerParameters>()
            }
            it("'test' 태스크는 WebServerService를 사용해야 한다") {
                val testTask = project.tasks.getByName("test") as Test
                val requiredServices = testTask.requiredServices.elements.first().get()
                (requiredServices is WebServerService) shouldBe true
            }
            it("'test' 태스크에 시스템 프로퍼티가 설정되어야 한다") {
                val testTask = project.tasks.getByName("test") as Test
                val systemProperties = testTask.systemProperties

                systemProperties.containsKey("gwt.junit.remoteUrl") shouldBe true
                systemProperties.containsKey("io.netty.leakDetection.level") shouldBe true
                systemProperties["io.netty.leakDetection.level"] shouldBe "PARANOID"
            }

            it("WebServerService의 contentRoot는 gwt.devMode.war 설정을 따라야 한다") {
                val serviceName = "gwtWebServer-test"
                val registration = project.gradle.sharedServices.registrations.getByName(serviceName)
                val parameters = registration.parameters as WebServerParameters

                // beforeEach에서 설정한 값
                val expectedPath = project.file("src/test/webapp")
                parameters.contentRoot.files shouldContain expectedPath
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
    }
})
