package dev.sayaya.gwt

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.docstr.gwt.GwtPluginExtension
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

class GwtTestCompileTaskTest : DescribeSpec({
    lateinit var project: Project
    lateinit var gwtExtension: GwtPluginExtension
    lateinit var baseExtraDir: File
    lateinit var devModeExtraDir: File


    beforeEach {
        project = ProjectBuilder.builder().withName("test-project").build()
        project.plugins.apply(GwtTestPlugin::class.java)
        gwtExtension = project.extensions.getByType(GwtPluginExtension::class.java)

        // `modules`가 비어있으면 예외가 발생하므로, 모든 테스트에서 기본값을 설정합니다.
        gwtExtension.modules.set(listOf("com.example.Dummy"))

        // 테스트에 사용할 가상 디렉토리를 생성합니다.
        baseExtraDir = project.file("src/base/extra")
        devModeExtraDir = project.file("src/dev/extra")
    }

    describe("GwtTestCompileTask의 extraSourceDirs 프로퍼티 설정") {
        context("gwt.devMode.extraSourceDirs와 gwt.extraSourceDirs가 모두 설정된 경우") {
            it("devMode의 extraSourceDirs를 우선적으로 사용해야 한다") {
                // 준비 (Arrange)
                gwtExtension.extraSourceDirs.from(baseExtraDir)
                gwtExtension.devMode.extraSourceDirs.from(devModeExtraDir)

                // 실행 (Act)
                val task = project.tasks.getByName("gwtTestCompile") as GwtTestCompileTask

                // 검증 (Assert)
                task.extraSourceDirs.files shouldContainExactly setOf(devModeExtraDir)
            }
        }

        context("gwt.extraSourceDirs만 설정된 경우") {
            it("base의 extraSourceDirs를 사용해야 한다") {
                // 준비 (Arrange)
                gwtExtension.extraSourceDirs.from(baseExtraDir)
                // gwt.devMode.extraSourceDirs는 설정하지 않음

                // 실행 (Act)
                val task = project.tasks.getByName("gwtTestCompile") as GwtTestCompileTask

                // 검증 (Assert)
                task.extraSourceDirs.files shouldContainExactly setOf(baseExtraDir)
            }
        }

        context("gwt.devMode.extraSourceDirs와 gwt.extraSourceDirs가 모두 설정되지 않은 경우") {
            it("extraSourceDirs는 비어있어야 한다") {
                // 준비 (Arrange)
                // 아무것도 설정하지 않음

                // 실행 (Act)
                val task = project.tasks.getByName("gwtTestCompile") as GwtTestCompileTask

                // 검증 (Assert)
                task.extraSourceDirs.files.shouldBeEmpty()
            }
        }
    }
    describe("GwtTestCompileTask의 sourceLevel 프로퍼티 설정") {
        context("gwt.devMode.sourceLevel과 gwt.sourceLevel이 모두 설정된 경우") {
            it("devMode의 sourceLevel을 우선적으로 사용해야 한다") {
                // 준비 (Arrange)
                gwtExtension.sourceLevel.set("8")
                gwtExtension.devMode.sourceLevel.set("11")

                // 실행 (Act)
                val task = project.tasks.getByName("gwtTestCompile") as GwtTestCompileTask

                // 검증 (Assert)
                task.sourceLevel.get() shouldBe "11"
            }
        }

        context("gwt.sourceLevel만 설정된 경우") {
            it("base의 sourceLevel을 사용해야 한다") {
                // 준비 (Arrange)
                gwtExtension.sourceLevel.set("8")
                // devMode.sourceLevel은 설정하지 않음

                // 실행 (Act)
                val task = project.tasks.getByName("gwtTestCompile") as GwtTestCompileTask

                // 검증 (Assert)
                task.sourceLevel.get() shouldBe "8"
            }
        }

        context("gwt.devMode.sourceLevel과 gwt.sourceLevel이 모두 설정되지 않은 경우") {
            it("sourceLevel 프로퍼티에 값이 없어야 한다 (not present)") {
                // 준비 (Arrange)
                // 아무것도 설정하지 않음

                // 실행 (Act)
                val task = project.tasks.getByName("gwtTestCompile") as GwtTestCompileTask

                // 검증 (Assert)
                task.sourceLevel.isPresent.shouldBeFalse()
            }
        }
    }
})