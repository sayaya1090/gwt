package dev.sayaya.gwt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.docstr.gwt.GwtPluginExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
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
    describe("ensureTestHtmlFileForModule (HTML 파일 생성)") {
        lateinit var warDir: File
        lateinit var srcDir: File

        beforeEach {
            // 이 테스트 그룹을 위한 디렉토리 설정
            warDir = project.file("build/war")
            srcDir = project.file("src/main/java")
            srcDir.mkdirs()
            gwtExtension.war.set(warDir)

            // 소스셋이 srcDir을 인식하도록 설정
            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            sourceSets.getByName("main").java.srcDir(srcDir)
        }

        context("HTML 파일이 존재하지 않을 때") {
            it("모듈 이름으로 HTML 파일을 생성해야 한다") {
                // 준비
                val moduleName = "com.example.App"
                val modulePath = moduleName.replace('.', '/')
                val xmlFile = File(srcDir, "$modulePath.gwt.xml")
                xmlFile.parentFile.mkdirs()
                xmlFile.writeText("<module><inherits name='com.google.gwt.user.User'/></module>")

                gwtExtension.modules.set(listOf(moduleName))
                val task = project.tasks.getByName("gwtTestCompile") as GwtTestCompileTask
                val expectedHtmlFile = File(warDir, "com.example.App.html")
                expectedHtmlFile.exists().shouldBeFalse()

                // 실행
                task.exec()

                // 검증
                expectedHtmlFile.exists().shouldBeTrue()
                val content = expectedHtmlFile.readText()
                content shouldContain "<title>com.example.App Test</title>"
                content shouldContain """<script type="text/javascript" src="com.example.App/com.example.App.nocache.js"></script>"""
            }

            it("'rename-to' 속성 값으로 HTML 파일을 생성해야 한다") {
                // 준비
                val moduleName = "com.example.App"
                val renamedModule = "RenamedApp"
                val modulePath = moduleName.replace('.', '/')
                val xmlFile = File(srcDir, "$modulePath.gwt.xml")
                xmlFile.parentFile.mkdirs()
                xmlFile.writeText("<module rename-to='$renamedModule'><inherits name='com.google.gwt.user.User'/></module>")

                gwtExtension.modules.set(listOf(moduleName))
                val task = project.tasks.getByName("gwtTestCompile") as GwtTestCompileTask
                val expectedHtmlFile = File(warDir, "$renamedModule.html")
                expectedHtmlFile.exists().shouldBeFalse()

                // 실행
                task.exec()

                // 검증
                expectedHtmlFile.exists().shouldBeTrue()
                val content = expectedHtmlFile.readText()
                content shouldContain "<title>$renamedModule Test</title>"
                content shouldContain """<script type="text/javascript" src="$renamedModule/$renamedModule.nocache.js"></script>"""
            }
        }

        context("HTML 파일이 이미 존재할 때") {
            it("기존 파일을 덮어쓰지 않아야 한다") {
                // 준비
                val moduleName = "com.example.App"
                val modulePath = moduleName.replace('.', '/')
                val xmlFile = File(srcDir, "$modulePath.gwt.xml")
                xmlFile.parentFile.mkdirs()
                xmlFile.writeText("<module><inherits name='com.google.gwt.user.User'/></module>")

                gwtExtension.modules.set(listOf(moduleName))
                val task = project.tasks.getByName("gwtTestCompile") as GwtTestCompileTask
                val existingHtmlFile = File(warDir, "com.example.App.html")
                existingHtmlFile.parentFile.mkdirs()
                val originalContent = "<!-- This is a pre-existing file -->"
                existingHtmlFile.writeText(originalContent)

                // 실행
                task.exec()

                // 검증
                existingHtmlFile.readText() shouldBe originalContent
            }
        }
        context("파일 생성 중 IO 오류가 발생할 때") {
            it("GradleException을 던져야 한다") {
                // 준비
                val moduleName = "com.example.App"
                val modulePath = moduleName.replace('.', '/')
                val xmlFile = File(srcDir, "$modulePath.gwt.xml")
                xmlFile.parentFile.mkdirs()
                xmlFile.writeText("<module />")

                gwtExtension.modules.set(listOf(moduleName))
                val task = project.tasks.getByName("gwtTestCompile") as GwtTestCompileTask

                // warDir을 읽기 전용으로 만들어 IO 예외를 유도
                warDir.mkdirs()
                warDir.setReadOnly()

                try {
                    // 실행 및 검증
                    val exception = shouldThrow<GradleException> {
                        task.exec()
                    }
                    exception.message shouldContain "HTML 파일 생성 중 오류가 발생했습니다"
                } finally {
                    // 정리: 테스트 후 디렉터리를 삭제할 수 있도록 쓰기 권한을 복원
                    warDir.setWritable(true)
                }
            }
        }
        context("gwt.xml 파일이 없을 때") {
            it("GradleException을 던져야 한다") {
                // 준비
                gwtExtension.modules.set(listOf("com.example.NonExistent"))
                val task = project.tasks.getByName("gwtTestCompile") as GwtTestCompileTask

                // 실행 및 검증
                val exception = shouldThrow<GradleException> {
                    task.exec()
                }
                exception.message shouldBe "XML 파일을 찾을 수 없습니다: com/example/NonExistent.gwt.xml"
            }
        }
    }
})