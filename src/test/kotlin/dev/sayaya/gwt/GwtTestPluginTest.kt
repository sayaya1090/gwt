package dev.sayaya.gwt

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.docstr.gwt.GwtPluginExtension
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.testfixtures.ProjectBuilder

class GwtTestPluginTest : DescribeSpec({
    lateinit var project: Project

    beforeEach {
        project = ProjectBuilder.builder().build()
        // GwtTestPlugin은 'java'와 'org.docstr.gwt'를 모두 필요로 하므로, 먼저 적용합니다.
        project.pluginManager.apply("java")
        project.pluginManager.apply(GwtTestPlugin::class.java)
    }

    describe("GwtTestPlugin 적용 시") {
        it("는 'org.docstr.gwt' 플러그인을 적용해야 한다") {
            project.plugins.hasPlugin("org.docstr.gwt") shouldBe true
        }

        it("는 모든 JavaCompile 태스크의 인코딩을 UTF-8로 설정해야 한다") {
            val javaCompileTask = project.tasks.getByName("compileJava") as JavaCompile
            javaCompileTask.options.encoding shouldBe "UTF-8"
        }

        it("는 모든 설정에서 'jakarta.servlet:jakarta.servlet-api'를 제외해야 한다") {
            val config = project.configurations.getByName("implementation")
            val rule = config.excludeRules.firstOrNull()
            rule?.group shouldBe "jakarta.servlet"
            rule?.module shouldBe "jakarta.servlet-api"
        }

        context("'gwtTestCompile' 태스크") {
            it("는 GwtTestCompileTask 타입으로 등록되어야 한다") {
                project.tasks.getByName("gwtTestCompile").shouldBeInstanceOf<GwtTestCompileTask>()
            }

            it("는 'processTestResources' 태스크에 의존해야 한다") {
                val task = project.tasks.getByName("gwtTestCompile")
                val dependencyNames = task.taskDependencies.getDependencies(task).map { it.name }
                dependencyNames shouldContain "processTestResources"
            }

            it("는 modules 프로퍼티가 gwt.devMode.modules 값으로 설정되어야 한다") {
                val testModuleName = "com.example.TestModule"
                val gwtExtension = project.extensions.getByType(GwtPluginExtension::class.java)
                gwtExtension.devMode.modules.set(listOf(testModuleName))

                // 태스크가 등록될 때 설정 액션이 실행되므로, 다시 조회해야 최신 설정이 반영됩니다.
                val task = project.tasks.getByName("gwtTestCompile") as GwtTestCompileTask
                task.modules.get() shouldContain testModuleName
            }
        }

        context("'test' 태스크") {
            it("는 'gwtTestCompile' 태스크에 의존해야 한다") {
                val task = project.tasks.getByName("test")
                val dependencyNames = task.taskDependencies.getDependencies(task).map { it.name }
                dependencyNames shouldContain "gwtTestCompile"
            }
        }
        context("'war' 플러그인과 함께 적용 시") {
            it("'war' 태스크는 'gwtCompile' 태스크에 의존해야 한다") {
                // 'war' 플러그인을 적용합니다.
                project.pluginManager.apply("war")

                // !!! 중요: gwtCompile 태스크가 생성되도록 GWT 모듈을 설정합니다.
                val gwtExtension = project.extensions.getByType(GwtPluginExtension::class.java)
                gwtExtension.modules.set(listOf("com.example.App"))

                val warTask = project.tasks.getByName("war")
                val dependencyNames = warTask.taskDependencies.getDependencies(warTask).map { it.name }
                dependencyNames shouldContain "gwtCompile"
            }
        }
    }
})