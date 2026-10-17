package dev.sayaya.gwt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.types.shouldBeInstanceOf
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testfixtures.ProjectBuilder

class GwtBasePluginTest : DescribeSpec({
    lateinit var project: Project
    lateinit var gwt: GwtPluginExtension

    beforeEach {
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply(GwtBasePlugin::class.java)
        gwt = project.extensions.getByType(GwtPluginExtension::class.java)
    }

    fun Project.evaluate() = (this as ProjectInternal).evaluate()
    fun Project.dependencyCoordinates(configuration: String): List<String> =
        configurations.getByName(configuration).dependencies.map { "${it.group}:${it.name}:${it.version}" }

    describe("GwtBasePlugin 적용 시") {
        it("java 플러그인과 gwt 확장을 등록한다") {
            project.plugins.hasPlugin("java") shouldBe true
            project.extensions.findByName("gwt").shouldBeInstanceOf<GwtPluginExtension>()
        }

        it("기본값을 build/gwt 하위와 기본 GWT 버전으로 설정한다") {
            gwt.gwtVersion.get() shouldBe GwtBasePlugin.DEFAULT_GWT_VERSION
            gwt.jakarta.get() shouldBe false
            gwt.war.get().asFile.path.replace('\\', '/') shouldEndWith "build/gwt/war"
            gwt.deploy.get().asFile.path.replace('\\', '/') shouldEndWith "build/gwt/deploy"
            gwt.gen.get().asFile.path.replace('\\', '/') shouldEndWith "build/gwt/gen"
            gwt.extra.get().asFile.path.replace('\\', '/') shouldEndWith "build/gwt/extra"
            gwt.cacheDir.get().asFile.path.replace('\\', '/') shouldEndWith "build/gwt/gwt-unitCache"
        }

        it("devMode는 지정하지 않은 공통 옵션을 상위 설정에서 이어받고, 지정하면 우선한다") {
            gwt.modules.set(listOf("com.example.App"))
            gwt.devMode.modules.get() shouldBe listOf("com.example.App")
            gwt.devMode.war.get() shouldBe gwt.war.get()

            gwt.devMode.modules.set(listOf("com.example.Test"))
            gwt.devMode.modules.get() shouldBe listOf("com.example.Test")
        }

        it("gwtDev는 implementation을, gwtDevRuntimeClasspath는 gwtDev를 상속한다") {
            val gwtDev = project.configurations.getByName(GwtBasePlugin.GWT_DEV_CONFIGURATION_NAME)
            val runtime = project.configurations.getByName(GwtBasePlugin.GWT_DEV_RUNTIME_CLASSPATH_CONFIGURATION_NAME)
            gwtDev.extendsFrom.map { it.name } shouldContain "implementation"
            gwtDev.isCanBeResolved shouldBe false
            runtime.extendsFrom.map { it.name } shouldContain GwtBasePlugin.GWT_DEV_CONFIGURATION_NAME
            runtime.isCanBeResolved shouldBe true
        }

        it("평가 후 gwtVersion에 맞춘 GWT 의존성을 추가한다") {
            gwt.gwtVersion.set("2.13.1")
            project.evaluate()

            project.dependencyCoordinates("implementation") shouldContainAll listOf(
                "org.gwtproject:gwt:2.13.1",
                "org.gwtproject:gwt-servlet:null"
            )
            project.dependencyCoordinates(GwtBasePlugin.GWT_DEV_CONFIGURATION_NAME) shouldContainAll listOf(
                "org.gwtproject:gwt:2.13.1",
                "org.gwtproject:gwt-dev:null",
                "org.gwtproject:gwt-user:null",
                "org.gwtproject:gwt-codeserver:null"
            )
        }

        it("jakarta가 true이면 gwt-servlet-jakarta를 추가한다") {
            gwt.jakarta.set(true)
            project.evaluate()

            val implementation = project.dependencyCoordinates("implementation")
            implementation shouldContain "org.gwtproject:gwt-servlet-jakarta:null"
            implementation shouldNotContain "org.gwtproject:gwt-servlet:null"
        }

        it("extraSourceDirs를 main 자바 소스 디렉토리에 추가한다") {
            val extra = project.file("src/extra/java")
            gwt.extraSourceDirs.from(extra)
            project.evaluate()

            val main = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
            main.java.srcDirs shouldContain extra
        }

        it("gwtCompile을 등록하고 build가 이에 의존한다") {
            val compile = project.tasks.getByName(GwtBasePlugin.GWT_COMPILE_TASK_NAME)
            compile.shouldBeInstanceOf<GwtCompileTask>()
            compile.mainClass.get() shouldBe GwtCompileTask.COMPILER_CLASS

            val build = project.tasks.getByName("build")
            build.taskDependencies.getDependencies(build).map { it.name } shouldContain GwtBasePlugin.GWT_COMPILE_TASK_NAME
        }

        it("gwtDevMode를 등록한다") {
            val devMode = project.tasks.getByName(GwtBasePlugin.GWT_DEV_MODE_TASK_NAME)
            devMode.shouldBeInstanceOf<GwtDevModeTask>()
            devMode.mainClass.get() shouldBe GwtDevModeTask.DEV_MODE_CLASS
        }
    }

    describe("GWT 인자 생성") {
        it("gwtCompile은 설정을 GWT 인자로 변환하고 모듈을 마지막에 둔다") {
            gwt.modules.set(listOf("com.example.App"))
            gwt.logLevel.set("INFO")
            gwt.war.set(project.file("out/war"))
            gwt.sourceLevel.set("auto")
            gwt.methodNameDisplayMode.set("FULL")
            gwt.generateJsInteropExports.set(true)
            gwt.includeJsInteropExports.set(listOf("com.a.*", "com.b.*"))
            gwt.style.set("PRETTY")
            gwt.failOnError.set(false)
            gwt.setProperty.set(listOf("user.agent=safari"))
            gwt.incremental.set(false)

            val task = project.tasks.getByName("gwtCompile") as GwtCompileTask
            val args = task.gwtArguments()

            args.subList(0, 2) shouldBe listOf("-logLevel", "INFO")
            args shouldContain "-war"
            args[args.indexOf("-war") + 1] shouldBe project.file("out/war").absolutePath
            args[args.indexOf("-sourceLevel") + 1] shouldBe "auto"
            args[args.indexOf("-XmethodNameDisplayMode") + 1] shouldBe "FULL"
            args shouldContain "-generateJsInteropExports"
            args.filter { it == "-includeJsInteropExports" }.size shouldBe 2
            args shouldContainAll listOf("com.a.*", "com.b.*", "-nofailOnError", "-noincremental", "user.agent=safari")
            args[args.indexOf("-style") + 1] shouldBe "PRETTY"
            args.last() shouldBe "com.example.App"
        }

        it("값이 없는 옵션은 인자로 전달하지 않는다") {
            gwt.modules.set(listOf("com.example.App"))
            val args = (project.tasks.getByName("gwtCompile") as GwtCompileTask).gwtArguments()
            args.none { it == "-style" || it == "-sourceLevel" || it == "-logLevel" } shouldBe true
        }

        it("X 접두사 불리언 옵션이 false이면 -Xno 형식을 사용한다") {
            val args = buildList<String> {
                with(GwtBaseTask) {
                    addFlag("XclassMetadata", project.provider { false })
                    addFlag("strict", project.provider { true })
                }
            }
            args shouldBe listOf("-XnoclassMetadata", "-strict")
        }

        it("gwtDevMode는 devMode 전용 인자를 모듈 앞에 추가한다") {
            gwt.modules.set(listOf("com.example.App"))
            gwt.devMode {
                modules.set(listOf("com.example.Test"))
                startServer.set(false)
                port.set(8888)
                bindAddress.set("0.0.0.0")
                codeServerPort.set(9997)
                superDevMode.set(true)
                server.set("com.example.Launcher")
                startupUrl.set("test.html")
                modulePathPrefix.set("gwt")
                logdir.set(project.file("out/log"))
            }

            val args = (project.tasks.getByName("gwtDevMode") as GwtDevModeTask).gwtArguments()
            args shouldContainAll listOf(
                "-nostartServer", "-port", "8888", "-bindAddress", "0.0.0.0",
                "-codeServerPort", "9997", "-superDevMode", "-server", "com.example.Launcher",
                "-startupUrl", "test.html", "-modulePathPrefix", "gwt", "-logdir", project.file("out/log").absolutePath
            )
            args.last() shouldBe "com.example.Test"
            args shouldNotContain "com.example.App"
        }

        it("cacheDir를 gwt.persistentunitcachedir JVM 인자로 전달한다") {
            val task = project.tasks.getByName("gwtCompile") as GwtCompileTask
            val jvmArgs = task.jvmArgumentProviders.flatMap { it.asArguments() }
            jvmArgs.single { it.startsWith("-Dgwt.persistentunitcachedir=") } shouldContain "gwt-unitCache"
        }

        it("힙 크기는 설정값을, 미설정 시 기본값을 사용한다") {
            val compile = project.tasks.getByName("gwtCompile") as GwtCompileTask
            compile.gwtMinHeapSize.get() shouldBe GwtBaseTask.DEFAULT_MIN_HEAP_SIZE
            compile.gwtMaxHeapSize.get() shouldBe GwtBaseTask.DEFAULT_MAX_HEAP_SIZE

            gwt.maxHeapSize.set("2g")
            compile.gwtMaxHeapSize.get() shouldBe "2g"
        }
    }

    describe("compiler { } 블록") {
        it("컴파일 전용 옵션을 gwtCompile 인자로 변환한다") {
            gwt.modules.set(listOf("com.example.App"))
            gwt.compiler {
                strict.set(true)
                closureFormattedOutput.set(false)
                compileReport.set(true)
                classMetadata.set(false)
                draftCompile.set(true)
                checkAssertions.set(false)
                fragmentCount.set(5)
                namespace.set("PACKAGE")
                optimize.set(9)
                saveSource.set(true)
                validateOnly.set(false)
                localWorkers.set(4)
                saveSourceOutput.set(project.file("out/src"))
            }

            val args = (project.tasks.getByName("gwtCompile") as GwtCompileTask).gwtArguments()
            args shouldContainAll listOf(
                "-strict", "-XnoclosureFormattedOutput", "-compileReport", "-XnoclassMetadata",
                "-draftCompile", "-nocheckAssertions", "-saveSource", "-novalidateOnly"
            )
            args[args.indexOf("-XfragmentCount") + 1] shouldBe "5"
            args[args.indexOf("-Xnamespace") + 1] shouldBe "PACKAGE"
            args[args.indexOf("-optimize") + 1] shouldBe "9"
            args[args.indexOf("-localWorkers") + 1] shouldBe "4"
            args[args.indexOf("-saveSourceOutput") + 1] shouldBe project.file("out/src").absolutePath
            args.last() shouldBe "com.example.App"
        }

        it("strict가 false이거나 미설정이면 -strict를 전달하지 않는다") {
            gwt.modules.set(listOf("com.example.App"))
            val task = project.tasks.getByName("gwtCompile") as GwtCompileTask
            task.gwtArguments().none { it.contains("strict") } shouldBe true

            gwt.compiler.strict.set(false)
            task.gwtArguments().none { it.contains("strict") } shouldBe true
        }

        it("compiler의 공통 옵션은 미설정 시 상위 gwt 값을 이어받고, 지정하면 gwtCompile에 우선 적용된다") {
            gwt.maxHeapSize.set("1g")
            gwt.style.set("OBFUSCATED")
            val task = project.tasks.getByName("gwtCompile") as GwtCompileTask
            task.gwtMaxHeapSize.get() shouldBe "1g"
            task.style.get() shouldBe "OBFUSCATED"

            gwt.compiler.maxHeapSize.set("3g")
            gwt.compiler.style.set("PRETTY")
            task.gwtMaxHeapSize.get() shouldBe "3g"
            task.style.get() shouldBe "PRETTY"
        }
    }

    describe("Test 태스크 구성") {
        it("힙 크기는 devMode → gwt → 기본값 순으로 적용된다") {
            project.evaluate()
            val test = project.tasks.getByName("test") as org.gradle.api.tasks.testing.Test
            test.minHeapSize shouldBe GwtBaseTask.DEFAULT_MIN_HEAP_SIZE
            test.maxHeapSize shouldBe GwtBaseTask.DEFAULT_MAX_HEAP_SIZE
        }

        it("gwt 힙 설정과 devMode 힙 설정을 반영한다") {
            gwt.minHeapSize.set("1024M")
            gwt.maxHeapSize.set("2048M")
            gwt.devMode.maxHeapSize.set("3g")
            project.evaluate()
            val test = project.tasks.getByName("test") as org.gradle.api.tasks.testing.Test
            test.minHeapSize shouldBe "1024M"
            test.maxHeapSize shouldBe "3g"
        }

        it("gwt.args와 gwt.persistentunitcachedir 시스템 프로퍼티를 설정한다") {
            gwt.war.set(project.file("src/main/webapp"))
            project.evaluate()
            val test = project.tasks.getByName("test") as org.gradle.api.tasks.testing.Test
            test.systemProperties["gwt.args"] shouldBe
                "-war ${project.file("src/main/webapp").absolutePath} " +
                "-deploy ${gwt.deploy.get().asFile.absolutePath} " +
                "-extra ${gwt.extra.get().asFile.absolutePath}"
            test.systemProperties["gwt.persistentunitcachedir"].toString() shouldContain "gwt-unitCache"
        }

        it("main·test 소스 디렉토리를 테스트 클래스패스에 추가하고 testImplementation이 gwtDevRuntimeClasspath를 상속한다") {
            project.evaluate()
            // 저장소가 없는 ProjectBuilder 환경에서 클래스패스를 해석할 수 있도록 원격 GWT 의존성은 비운다.
            listOf("implementation", GwtBasePlugin.GWT_DEV_CONFIGURATION_NAME).forEach {
                project.configurations.getByName(it).dependencies.clear()
            }
            val test = project.tasks.getByName("test") as org.gradle.api.tasks.testing.Test
            val main = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
            test.classpath.files shouldContainAll main.java.srcDirs
            project.configurations.getByName("testImplementation").extendsFrom.map { it.name } shouldContain
                GwtBasePlugin.GWT_DEV_RUNTIME_CLASSPATH_CONFIGURATION_NAME
        }
    }

    describe("실행 전 검증") {
        it("modules가 비어 있으면 GradleException을 던진다") {
            val task = project.tasks.getByName("gwtCompile") as GwtCompileTask
            val error = shouldThrow<GradleException> { task.exec() }
            error.message!! shouldContain "'modules' property is required"
        }
    }
})
