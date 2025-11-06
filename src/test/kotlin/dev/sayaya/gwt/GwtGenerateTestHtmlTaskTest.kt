
package dev.sayaya.gwt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.docstr.gwt.GwtPluginExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

class GwtGenerateTestHtmlTaskTest : DescribeSpec({
    lateinit var project: Project
    lateinit var task: GwtGenerateTestHtmlTask
    lateinit var gwtExtension: GwtPluginExtension
    lateinit var warDir: File
    lateinit var srcDir: File

    beforeEach {
        project = ProjectBuilder.builder().withName("test-project").build()
        project.plugins.apply(JavaPlugin::class.java)
        project.plugins.apply(GwtTestPlugin::class.java)

        gwtExtension = project.extensions.getByType(GwtPluginExtension::class.java)

        // 디렉토리 설정
        warDir = project.file("build/war")
        srcDir = project.file("src/main/java")
        srcDir.mkdirs()
        warDir.mkdirs()

        // 소스셋 설정
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        sourceSets.getByName("main").java.srcDir(srcDir)

        // 태스크 가져오기
        task = project.tasks.getByName("gwtGenerateTestHtml") as GwtGenerateTestHtmlTask

        // 기본 설정
        task.war.set(warDir)
    }

    afterEach {
        // 테스트 후 정리
        warDir.deleteRecursively()
        srcDir.deleteRecursively()
    }

    describe("GwtGenerateTestHtmlTask의 기본 설정") {
        it("태스크가 정상적으로 등록되어야 한다") {
            task.name shouldBe "gwtGenerateTestHtml"
            task.group shouldBe "GWT"
            task.description shouldBe "Generates HTML host files for GWT test modules"
        }

        it("modules 프로퍼티가 설정 가능해야 한다") {
            task.modules.set(listOf("com.example.Test"))
            task.modules.get() shouldBe listOf("com.example.Test")
        }

        it("war 프로퍼티가 설정 가능해야 한다") {
            val customWar = project.file("custom/war")
            task.war.set(customWar)
            task.war.get().asFile shouldBe customWar
        }
    }

    describe("HTML 파일 생성") {
        context("모듈 XML 파일이 존재하고 HTML 파일이 없는 경우") {
            it("모듈 이름으로 HTML 파일을 생성해야 한다") {
                // 준비
                val moduleName = "com.example.App"
                val modulePath = moduleName.replace('.', '/')
                val xmlFile = File(srcDir, "$modulePath.gwt.xml")
                xmlFile.parentFile.mkdirs()
                xmlFile.writeText("""
                    <module>
                        <inherits name='com.google.gwt.user.User'/>
                    </module>
                """.trimIndent())

                task.modules.set(listOf(moduleName))
                val expectedHtmlFile = File(warDir, "com.example.App.html")
                expectedHtmlFile.exists().shouldBeFalse()

                // 실행
                task.generateHtmlFiles()

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
                xmlFile.writeText("""
                    <module rename-to='$renamedModule'>
                        <inherits name='com.google.gwt.user.User'/>
                    </module>
                """.trimIndent())

                task.modules.set(listOf(moduleName))
                val expectedHtmlFile = File(warDir, "$renamedModule.html")
                expectedHtmlFile.exists().shouldBeFalse()

                // 실행
                task.generateHtmlFiles()

                // 검증
                expectedHtmlFile.exists().shouldBeTrue()
                val content = expectedHtmlFile.readText()
                content shouldContain "<title>$renamedModule Test</title>"
                content shouldContain """<script type="text/javascript" src="$renamedModule/$renamedModule.nocache.js"></script>"""
            }

            it("여러 모듈에 대해 각각 HTML 파일을 생성해야 한다") {
                // 준비
                val module1 = "com.example.Module1"
                val module2 = "com.example.Module2"

                listOf(module1, module2).forEach { moduleName ->
                    val modulePath = moduleName.replace('.', '/')
                    val xmlFile = File(srcDir, "$modulePath.gwt.xml")
                    xmlFile.parentFile.mkdirs()
                    xmlFile.writeText("<module><inherits name='com.google.gwt.user.User'/></module>")
                }

                task.modules.set(listOf(module1, module2))

                // 실행
                task.generateHtmlFiles()

                // 검증
                File(warDir, "$module1.html").exists().shouldBeTrue()
                File(warDir, "$module2.html").exists().shouldBeTrue()
            }
        }

        context("HTML 파일이 이미 존재하는 경우") {
            it("기존 파일을 덮어쓰지 않아야 한다") {
                // 준비
                val moduleName = "com.example.App"
                val modulePath = moduleName.replace('.', '/')
                val xmlFile = File(srcDir, "$modulePath.gwt.xml")
                xmlFile.parentFile.mkdirs()
                xmlFile.writeText("<module><inherits name='com.google.gwt.user.User'/></module>")

                task.modules.set(listOf(moduleName))

                val existingHtmlFile = File(warDir, "com.example.App.html")
                existingHtmlFile.parentFile.mkdirs()
                val originalContent = "<!-- This is a pre-existing file -->"
                existingHtmlFile.writeText(originalContent)

                // 실행
                task.generateHtmlFiles()

                // 검증
                existingHtmlFile.readText() shouldBe originalContent
            }
        }

        context("war 디렉토리가 존재하지 않는 경우") {
            it("디렉토리를 생성하고 HTML 파일을 만들어야 한다") {
                // 준비
                val moduleName = "com.example.App"
                val modulePath = moduleName.replace('.', '/')
                val xmlFile = File(srcDir, "$modulePath.gwt.xml")
                xmlFile.parentFile.mkdirs()
                xmlFile.writeText("<module><inherits name='com.google.gwt.user.User'/></module>")

                val newWarDir = project.file("build/new-war")
                newWarDir.exists().shouldBeFalse()

                task.war.set(newWarDir)
                task.modules.set(listOf(moduleName))

                // 실행
                task.generateHtmlFiles()

                // 검증
                newWarDir.exists().shouldBeTrue()
                File(newWarDir, "com.example.App.html").exists().shouldBeTrue()

                // 정리
                newWarDir.deleteRecursively()
            }
        }
    }

    describe("XML 파일 파싱") {
        context("rename-to 속성이 있는 경우") {
            it("rename-to 값을 모듈 이름으로 사용해야 한다") {
                // 준비
                val moduleName = "com.example.Original"
                val renamedTo = "Renamed"
                val modulePath = moduleName.replace('.', '/')
                val xmlFile = File(srcDir, "$modulePath.gwt.xml")
                xmlFile.parentFile.mkdirs()
                xmlFile.writeText("""<module rename-to="$renamedTo"></module>""")

                task.modules.set(listOf(moduleName))

                // 실행
                task.generateHtmlFiles()

                // 검증
                File(warDir, "$renamedTo.html").exists().shouldBeTrue()
                File(warDir, "$moduleName.html").exists().shouldBeFalse()
            }
        }

        context("rename-to 속성이 빈 문자열인 경우") {
            it("원래 모듈 이름을 사용해야 한다") {
                // 준비
                val moduleName = "com.example.App"
                val modulePath = moduleName.replace('.', '/')
                val xmlFile = File(srcDir, "$modulePath.gwt.xml")
                xmlFile.parentFile.mkdirs()
                xmlFile.writeText("""<module rename-to=""></module>""")

                task.modules.set(listOf(moduleName))

                // 실행
                task.generateHtmlFiles()

                // 검증
                File(warDir, "$moduleName.html").exists().shouldBeTrue()
            }
        }

        context("복잡한 XML 구조인 경우") {
            it("XML을 정상적으로 파싱해야 한다") {
                // 준비
                val moduleName = "com.example.Complex"
                val modulePath = moduleName.replace('.', '/')
                val xmlFile = File(srcDir, "$modulePath.gwt.xml")
                xmlFile.parentFile.mkdirs()
                xmlFile.writeText("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <module rename-to="complex">
                        <inherits name='com.google.gwt.user.User'/>
                        <inherits name='com.google.gwt.junit.JUnit'/>
                        <entry-point class='com.example.client.Complex'/>
                        <source path="client"/>
                        <source path="shared"/>
                    </module>
                """.trimIndent())

                task.modules.set(listOf(moduleName))

                // 실행
                task.generateHtmlFiles()

                // 검증
                File(warDir, "complex.html").exists().shouldBeTrue()
            }
        }
    }

    describe("HTML 파일 내용 검증") {
        it("올바른 HTML 구조를 가져야 한다") {
            // 준비
            val moduleName = "com.example.Test"
            val modulePath = moduleName.replace('.', '/')
            val xmlFile = File(srcDir, "$modulePath.gwt.xml")
            xmlFile.parentFile.mkdirs()
            xmlFile.writeText("<module></module>")

            task.modules.set(listOf(moduleName))

            // 실행
            task.generateHtmlFiles()

            // 검증
            val htmlFile = File(warDir, "$moduleName.html")
            val content = htmlFile.readText()

            content shouldContain "<!DOCTYPE html>"
            content shouldContain "<html>"
            content shouldContain "<head>"
            content shouldContain "<title>$moduleName Test</title>"
            content shouldContain "<body>"
            content shouldContain "</html>"
        }

        it("JavaScript 로드 스크립트를 포함해야 한다") {
            // 준비
            val moduleName = "com.example.App"
            val renamedTo = "app"
            val modulePath = moduleName.replace('.', '/')
            val xmlFile = File(srcDir, "$modulePath.gwt.xml")
            xmlFile.parentFile.mkdirs()
            xmlFile.writeText("""<module rename-to="$renamedTo"></module>""")

            task.modules.set(listOf(moduleName))

            // 실행
            task.generateHtmlFiles()

            // 검증
            val htmlFile = File(warDir, "$renamedTo.html")
            val content = htmlFile.readText()

            content shouldContain """<script type="text/javascript" src="$renamedTo/$renamedTo.nocache.js"></script>"""
        }
    }

    describe("예외 처리") {
        context("모듈 XML 파일이 존재하지 않는 경우") {
            it("GradleException을 던져야 한다") {
                // 준비
                val moduleName = "com.example.NonExistent"
                task.modules.set(listOf(moduleName))

                // 실행 및 검증
                val exception = shouldThrow<GradleException> {
                    task.generateHtmlFiles()
                }
                exception.message shouldBe "XML 파일을 찾을 수 없습니다: com/example/NonExistent.gwt.xml"
            }
        }

        context("war 디렉토리가 읽기 전용인 경우") {
            it("GradleException을 던져야 한다") {
                // 준비
                val moduleName = "com.example.App"
                val modulePath = moduleName.replace('.', '/')
                val xmlFile = File(srcDir, "$modulePath.gwt.xml")
                xmlFile.parentFile.mkdirs()
                xmlFile.writeText("<module></module>")

                task.modules.set(listOf(moduleName))

                // war 디렉토리를 읽기 전용으로 만들기
                warDir.setReadOnly()

                try {
                    // 실행 및 검증
                    val exception = shouldThrow<GradleException> {
                        task.generateHtmlFiles()
                    }
                    exception.message shouldContain "HTML 파일 생성 중 오류가 발생했습니다"
                } finally {
                    // 정리
                    warDir.setWritable(true)
                }
            }
        }

        context("잘못된 XML 형식인 경우") {
            it("예외를 던져야 한다") {
                // 준비
                val moduleName = "com.example.Invalid"
                val modulePath = moduleName.replace('.', '/')
                val xmlFile = File(srcDir, "$modulePath.gwt.xml")
                xmlFile.parentFile.mkdirs()
                xmlFile.writeText("This is not valid XML")

                task.modules.set(listOf(moduleName))

                // 실행 및 검증
                shouldThrow<Exception> {
                    task.generateHtmlFiles()
                }
            }
        }
    }

    describe("소스셋 탐색") {
        context("여러 소스 디렉토리가 있는 경우") {
            it("모든 소스 디렉토리를 탐색해야 한다") {
                // 준비
                val moduleName = "com.example.MultiSource"
                val modulePath = moduleName.replace('.', '/')

                val additionalSrcDir = project.file("src/main/resources")
                additionalSrcDir.mkdirs()

                val xmlFile = File(additionalSrcDir, "$modulePath.gwt.xml")
                xmlFile.parentFile.mkdirs()
                xmlFile.writeText("<module></module>")

                val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
                sourceSets.getByName("main").resources.srcDir(additionalSrcDir)

                task.modules.set(listOf(moduleName))

                // 실행
                task.generateHtmlFiles()

                // 검증
                File(warDir, "$moduleName.html").exists().shouldBeTrue()
            }
        }

        context("test 소스셋에 XML이 있는 경우") {
            it("test 소스셋에서 XML을 찾을 수 있어야 한다") {
                // 준비
                val moduleName = "com.example.TestModule"
                val modulePath = moduleName.replace('.', '/')

                val testSrcDir = project.file("src/test/java")
                testSrcDir.mkdirs()

                val xmlFile = File(testSrcDir, "$modulePath.gwt.xml")
                xmlFile.parentFile.mkdirs()
                xmlFile.writeText("<module></module>")

                val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
                sourceSets.getByName("test").java.srcDir(testSrcDir)

                task.modules.set(listOf(moduleName))

                // 실행
                task.generateHtmlFiles()

                // 검증
                File(warDir, "$moduleName.html").exists().shouldBeTrue()

                // 정리
                testSrcDir.deleteRecursively()
            }
        }
    }

    describe("에지 케이스") {
        context("모듈 이름에 특수 문자가 있는 경우") {
            it("파일명에서 특수 문자를 처리해야 한다") {
                // 준비
                val moduleName = "com.example.Special_Module-Test"
                val modulePath = moduleName.replace('.', '/')
                val xmlFile = File(srcDir, "$modulePath.gwt.xml")
                xmlFile.parentFile.mkdirs()
                xmlFile.writeText("<module></module>")

                task.modules.set(listOf(moduleName))

                // 실행
                task.generateHtmlFiles()

                // 검증
                File(warDir, "$moduleName.html").exists().shouldBeTrue()
            }
        }

        context("빈 모듈 목록인 경우") {
            it("아무 작업도 하지 않아야 한다") {
                // 준비
                task.modules.set(emptyList())

                // 실행 - 예외가 발생하지 않아야 함
                task.generateHtmlFiles()

                // 검증 - war 디렉토리에 HTML 파일이 없어야 함
                warDir.listFiles()?.filter { it.extension == "html" }?.isEmpty() shouldBe true
            }
        }
    }
})