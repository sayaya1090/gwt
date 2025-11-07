package dev.sayaya.gwt

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.docstr.gwt.GwtPluginExtension
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class GwtTestCompileTaskTest : DescribeSpec({
    lateinit var project: Project
    lateinit var gwtExtension: GwtPluginExtension


    beforeEach {
        project = ProjectBuilder.builder().withName("test-project").build()
        project.plugins.apply(GwtTestPlugin::class.java)
        gwtExtension = project.extensions.getByType(GwtPluginExtension::class.java)

        // `modules`가 비어있으면 예외가 발생하므로, 모든 테스트에서 기본값을 설정합니다.
        gwtExtension.modules.set(listOf("com.example.Dummy"))
    }
    describe("TestCompileTask") {
        it("는 'gwtGenerateHtmlTask' 태스크에 의존해야 한다") {
            val task = project.tasks.getByName("gwtTestCompile") as GwtTestCompileTask
            val dependencyNames = task.taskDependencies.getDependencies(task).map { it.name }
            dependencyNames shouldContain "gwtGenerateTestHtml"
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
    describe("GwtDevPluginExtension 프록시 클래스") {
        lateinit var proxy: GwtTestCompileTask.Companion.GwtDevPluginExtension

        beforeEach {
            proxy = GwtTestCompileTask.Companion.GwtDevPluginExtension(gwtExtension)
        }

        context("기본 위임 프로퍼티") {
            it("gwtVersion은 extension의 gwtVersion을 그대로 반환한다") {
                gwtExtension.gwtVersion.set("2.11.0")
                proxy.gwtVersion.get() shouldBe "2.11.0"
            }

            it("jakarta는 extension의 jakarta를 그대로 반환한다") {
                gwtExtension.jakarta.set(true)
                proxy.jakarta.get() shouldBe true

                gwtExtension.jakarta.set(false)
                proxy.jakarta.get() shouldBe false
            }

            it("compiler는 extension의 compiler를 그대로 반환한다") {
                proxy.compiler shouldBe gwtExtension.compiler
            }

            it("devMode는 extension의 devMode를 그대로 반환한다") {
                proxy.devMode shouldBe gwtExtension.devMode
            }

            it("gwtTest는 extension의 gwtTest를 그대로 반환한다") {
                proxy.gwtTest shouldBe gwtExtension.gwtTest
            }

            it("superDev는 extension의 superDev를 그대로 반환한다") {
                proxy.superDev shouldBe gwtExtension.superDev
            }
        }

        context("Property<String> 타입 - devMode 우선 적용") {
            it("minHeapSize는 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                gwtExtension.minHeapSize.set("1g")
                proxy.minHeapSize.get() shouldBe "1g"

                gwtExtension.devMode.minHeapSize.set("2g")
                proxy.minHeapSize.get() shouldBe "2g"
            }

            it("maxHeapSize는 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                gwtExtension.maxHeapSize.set("2g")
                proxy.maxHeapSize.get() shouldBe "2g"

                gwtExtension.devMode.maxHeapSize.set("4g")
                proxy.maxHeapSize.get() shouldBe "4g"
            }

            it("logLevel은 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                gwtExtension.logLevel.set("INFO")
                proxy.logLevel.get() shouldBe "INFO"

                gwtExtension.devMode.logLevel.set("DEBUG")
                proxy.logLevel.get() shouldBe "DEBUG"
            }

            it("methodNameDisplayMode는 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                gwtExtension.methodNameDisplayMode.set("NONE")
                proxy.methodNameDisplayMode.get() shouldBe "NONE"

                gwtExtension.devMode.methodNameDisplayMode.set("FULL")
                proxy.methodNameDisplayMode.get() shouldBe "FULL"
            }

            it("sourceLevel은 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                gwtExtension.sourceLevel.set("8")
                proxy.sourceLevel.get() shouldBe "8"

                gwtExtension.devMode.sourceLevel.set("11")
                proxy.sourceLevel.get() shouldBe "11"
            }

            it("style은 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                gwtExtension.style.set("OBF")
                proxy.style.get() shouldBe "OBF"

                gwtExtension.devMode.style.set("PRETTY")
                proxy.style.get() shouldBe "PRETTY"
            }
        }

        context("Property<Boolean> 타입 - devMode 우선 적용") {
            it("generateJsInteropExports는 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                gwtExtension.generateJsInteropExports.set(false)
                proxy.generateJsInteropExports.get() shouldBe false

                gwtExtension.devMode.generateJsInteropExports.set(true)
                proxy.generateJsInteropExports.get() shouldBe true
            }

            it("incremental은 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                gwtExtension.incremental.set(false)
                proxy.incremental.get() shouldBe false

                gwtExtension.devMode.incremental.set(true)
                proxy.incremental.get() shouldBe true
            }

            it("failOnError는 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                gwtExtension.failOnError.set(false)
                proxy.failOnError.get() shouldBe false

                gwtExtension.devMode.failOnError.set(true)
                proxy.failOnError.get() shouldBe true
            }
        }

        context("DirectoryProperty 타입 - devMode 우선 적용") {
            it("workDir은 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                val baseDir = project.layout.projectDirectory.dir("baseWork")
                val devDir = project.layout.projectDirectory.dir("devWork")

                gwtExtension.workDir.set(baseDir)
                proxy.workDir.get() shouldBe baseDir

                gwtExtension.devMode.workDir.set(devDir)
                proxy.workDir.get() shouldBe devDir
            }

            it("gen은 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                val baseDir = project.layout.projectDirectory.dir("baseGen")
                val devDir = project.layout.projectDirectory.dir("devGen")

                gwtExtension.gen.set(baseDir)
                proxy.gen.get() shouldBe baseDir

                gwtExtension.devMode.gen.set(devDir)
                proxy.gen.get() shouldBe devDir
            }

            it("war은 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                val baseDir = project.layout.projectDirectory.dir("baseWar")
                val devDir = project.layout.projectDirectory.dir("devWar")

                gwtExtension.war.set(baseDir)
                proxy.war.get() shouldBe baseDir

                gwtExtension.devMode.war.set(devDir)
                proxy.war.get() shouldBe devDir
            }

            it("deploy은 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                val baseDir = project.layout.projectDirectory.dir("baseDeploy")
                val devDir = project.layout.projectDirectory.dir("devDeploy")

                gwtExtension.deploy.set(baseDir)
                proxy.deploy.get() shouldBe baseDir

                gwtExtension.devMode.deploy.set(devDir)
                proxy.deploy.get() shouldBe devDir
            }

            it("extra은 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                val baseDir = project.layout.projectDirectory.dir("baseExtra")
                val devDir = project.layout.projectDirectory.dir("devExtra")

                gwtExtension.extra.set(baseDir)
                proxy.extra.get() shouldBe baseDir

                gwtExtension.devMode.extra.set(devDir)
                proxy.extra.get() shouldBe devDir
            }

            it("cacheDir은 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                val baseDir = project.layout.projectDirectory.dir("baseCache")
                val devDir = project.layout.projectDirectory.dir("devCache")

                gwtExtension.cacheDir.set(baseDir)
                proxy.cacheDir.get() shouldBe baseDir

                gwtExtension.devMode.cacheDir.set(devDir)
                proxy.cacheDir.get() shouldBe devDir
            }
        }

        context("ListProperty<String> 타입 - devMode 우선 적용") {
            it("includeJsInteropExports는 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                gwtExtension.includeJsInteropExports.set(listOf("A"))
                proxy.includeJsInteropExports.get() shouldBe listOf("A")

                gwtExtension.devMode.includeJsInteropExports.set(listOf("B"))
                proxy.includeJsInteropExports.get() shouldBe listOf("B")
            }

            it("excludeJsInteropExports는 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                gwtExtension.excludeJsInteropExports.set(listOf("C"))
                proxy.excludeJsInteropExports.get() shouldBe listOf("C")

                gwtExtension.devMode.excludeJsInteropExports.set(listOf("D"))
                proxy.excludeJsInteropExports.get() shouldBe listOf("D")
            }

            it("setProperty는 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                gwtExtension.setProperty.set(listOf("foo=bar"))
                proxy.setProperty.get() shouldBe listOf("foo=bar")

                gwtExtension.devMode.setProperty.set(listOf("a=b"))
                proxy.setProperty.get() shouldBe listOf("a=b")
            }

            it("modules는 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                gwtExtension.modules.set(listOf("com.example.Base"))
                proxy.modules.get() shouldBe listOf("com.example.Base")

                gwtExtension.devMode.modules.set(listOf("com.example.Test"))
                proxy.modules.get() shouldBe listOf("com.example.Test")
            }
        }

        context("ConfigurableFileCollection 타입 - devMode 우선 적용") {
            it("extraSourceDirs는 devMode 설정을 우선하고, 없으면 base를 사용한다") {
                val baseFile = project.file("base.txt")
                baseFile.writeText("base")
                val baseFiles = project.files(baseFile)
                gwtExtension.extraSourceDirs.from(baseFiles)
                proxy.extraSourceDirs.files.shouldContain(baseFile)

                val devFile = project.file("dev.txt")
                devFile.writeText("dev")
                val devFiles = project.files(devFile)
                gwtExtension.devMode.extraSourceDirs.from(devFiles)
                proxy.extraSourceDirs.files.shouldContain(devFile)
            }
        }
    }
})
