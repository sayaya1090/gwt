package dev.sayaya.gwt

import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.work.DisableCachingByDefault

/**
 * GWT 도구(Compiler, DevMode)를 별도 JVM 프로세스로 실행하는 태스크의 공통 기반 클래스입니다.
 *
 * ## 역할
 * - [GwtOptions]에 대응하는 태스크 프로퍼티를 선언하고, 이를 GWT 커맨드라인 인자로 변환합니다
 *   (`-name value`, 불리언은 `-name`/`-noname`, 마지막에 모듈 이름 목록).
 * - 클래스패스를 `main` 소스 디렉토리 → `main` 출력(클래스·리소스) → [extraSourceDirs] → [gwtDevRuntimeClasspath] 순으로 구성합니다.
 * - [cacheDir]가 있으면 `-Dgwt.persistentunitcachedir` JVM 인자를 추가합니다.
 *
 * ## 주요 의존관계
 * - [GwtBasePlugin]이 생성하는 `gwtDevRuntimeClasspath` 설정(gwt-dev, gwt-user, gwt-codeserver 및 `implementation`)
 * - [GwtLombokPlugin]은 이 타입의 모든 태스크에 Lombok `-javaagent`를 추가합니다.
 *
 * ## 주의사항
 * - 프로젝트에 `java` 플러그인이 적용되어 있어야 합니다(`main` 소스셋 필요).
 * - [modules]가 비어 있으면 실행 시 [GradleException]이 발생합니다.
 */
@DisableCachingByDefault(because = "Abstract base type; caching is decided by each concrete GWT task")
abstract class GwtBaseTask : JavaExec() {
    @get:Console
    abstract val logLevel: Property<String>

    @get:OutputDirectory @get:Optional
    abstract val workDir: DirectoryProperty

    @get:OutputDirectory @get:Optional
    abstract val gen: DirectoryProperty

    @get:OutputDirectory @get:Optional
    abstract val war: DirectoryProperty

    @get:OutputDirectory @get:Optional
    abstract val deploy: DirectoryProperty

    @get:OutputDirectory @get:Optional
    abstract val extra: DirectoryProperty

    @get:OutputDirectory @get:Optional
    abstract val cacheDir: DirectoryProperty

    @get:Input @get:Optional
    abstract val sourceLevel: Property<String>

    @get:Input @get:Optional
    abstract val methodNameDisplayMode: Property<String>

    @get:Input @get:Optional
    abstract val generateJsInteropExports: Property<Boolean>

    @get:Input @get:Optional
    abstract val includeJsInteropExports: ListProperty<String>

    @get:Input @get:Optional
    abstract val excludeJsInteropExports: ListProperty<String>

    @get:Input @get:Optional
    abstract val style: Property<String>

    @get:Input @get:Optional
    abstract val failOnError: Property<Boolean>

    @get:Input @get:Optional
    abstract val setProperty: ListProperty<String>

    @get:Input @get:Optional
    abstract val incremental: Property<Boolean>

    @get:Input
    abstract val modules: ListProperty<String>

    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE) @get:Optional
    abstract val extraSourceDirs: ConfigurableFileCollection

    @get:Classpath
    abstract val gwtDevRuntimeClasspath: ConfigurableFileCollection

    /** 실행 시 JVM 최소 힙 크기로 적용됩니다. */
    @get:Internal
    abstract val gwtMinHeapSize: Property<String>

    /** 실행 시 JVM 최대 힙 크기로 적용됩니다. */
    @get:Internal
    abstract val gwtMaxHeapSize: Property<String>

    init {
        group = "GWT"
        gwtMinHeapSize.convention(DEFAULT_MIN_HEAP_SIZE)
        gwtMaxHeapSize.convention(DEFAULT_MAX_HEAP_SIZE)

        project.configurations.findByName(GwtBasePlugin.GWT_DEV_RUNTIME_CLASSPATH_CONFIGURATION_NAME)
            ?.let { gwtDevRuntimeClasspath.from(it) }
        val main = project.extensions.getByType(SourceSetContainer::class.java)
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        classpath(main.allSource.sourceDirectories, main.output, extraSourceDirs, gwtDevRuntimeClasspath)

        argumentProviders.add(CommandLineArgumentProvider { gwtArguments() })
        jvmArgumentProviders.add(CommandLineArgumentProvider {
            cacheDir.orNull?.let { listOf("-Dgwt.persistentunitcachedir=${it.asFile.path}") } ?: emptyList()
        })
    }

    /**
     * GWT 메인 클래스에 전달할 인자 목록을 만듭니다. 모듈 이름은 항상 마지막에 위치합니다.
     */
    fun gwtArguments(): List<String> = buildList {
        addValue("logLevel", logLevel)
        addDirectory("workDir", workDir)
        addDirectory("gen", gen)
        addDirectory("war", war)
        addDirectory("deploy", deploy)
        addDirectory("extra", extra)
        addValue("sourceLevel", sourceLevel)
        addValue("XmethodNameDisplayMode", methodNameDisplayMode)
        addFlag("generateJsInteropExports", generateJsInteropExports)
        addEach("includeJsInteropExports", includeJsInteropExports)
        addEach("excludeJsInteropExports", excludeJsInteropExports)
        addValue("style", style)
        addFlag("failOnError", failOnError)
        addEach("setProperty", setProperty)
        addFlag("incremental", incremental)
        addAll(additionalArguments())
        addAll(modules.getOrElse(emptyList()))
    }

    /** 하위 태스크 전용 인자. 모듈 이름 앞에 추가됩니다. */
    protected open fun additionalArguments(): List<String> = emptyList()

    override fun exec() {
        if (modules.getOrElse(emptyList()).isEmpty()) {
            throw GradleException(
                "$name failed: 'modules' property is required. Please specify at least one GWT module in the gwt { ... } block."
            )
        }
        war.orNull?.asFile?.let { dir ->
            if (!dir.exists() && !dir.mkdirs()) throw GradleException("Failed to create war directory: $dir")
        }
        minHeapSize = gwtMinHeapSize.get()
        maxHeapSize = gwtMaxHeapSize.get()
        logger.info("classpath: {}", classpath.asPath)
        logger.info("main: {}", mainClass.get())
        logger.info("args: {}", allArgumentsForLog())
        super.exec()
    }

    private fun allArgumentsForLog(): String = (args.orEmpty() + gwtArguments()).joinToString(" ")

    /**
     * [options]의 값을 이 태스크의 프로퍼티에 지연 연결합니다. [extraSourceDirs]는 호출 측에서 합산합니다.
     */
    fun configureFrom(options: GwtOptions) {
        gwtMinHeapSize.set(options.minHeapSize.orElse(DEFAULT_MIN_HEAP_SIZE))
        gwtMaxHeapSize.set(options.maxHeapSize.orElse(DEFAULT_MAX_HEAP_SIZE))
        logLevel.set(options.logLevel)
        workDir.set(options.workDir)
        gen.set(options.gen)
        war.set(options.war)
        deploy.set(options.deploy)
        extra.set(options.extra)
        cacheDir.set(options.cacheDir)
        sourceLevel.set(options.sourceLevel)
        methodNameDisplayMode.set(options.methodNameDisplayMode)
        generateJsInteropExports.set(options.generateJsInteropExports)
        includeJsInteropExports.set(options.includeJsInteropExports)
        excludeJsInteropExports.set(options.excludeJsInteropExports)
        style.set(options.style)
        failOnError.set(options.failOnError)
        setProperty.set(options.setProperty)
        incremental.set(options.incremental)
        modules.set(options.modules)
    }

    companion object {
        const val DEFAULT_MIN_HEAP_SIZE = "256M"
        const val DEFAULT_MAX_HEAP_SIZE = "512M"

        /** 값이 있으면 `-name value`를 추가합니다. */
        internal fun MutableList<String>.addValue(name: String, value: Provider<*>) {
            if (value.isPresent) {
                add("-$name")
                add(value.get().toString())
            }
        }

        /** 디렉토리가 있으면 `-name <절대경로>`를 추가합니다. */
        internal fun MutableList<String>.addDirectory(name: String, value: DirectoryProperty) {
            if (value.isPresent) {
                add("-$name")
                add(value.get().asFile.absolutePath)
            }
        }

        /** 불리언 값에 따라 `-name` 또는 `-noname`(`X` 접두사 옵션은 `-Xnoname`)을 추가합니다. */
        internal fun MutableList<String>.addFlag(name: String, value: Provider<Boolean>) {
            if (!value.isPresent) return
            when {
                value.get() -> add("-$name")
                name.startsWith("X") -> add("-Xno${name.substring(1)}")
                else -> add("-no$name")
            }
        }

        /** 목록의 각 항목마다 `-name item`을 추가합니다. */
        internal fun MutableList<String>.addEach(name: String, values: ListProperty<String>) {
            values.getOrElse(emptyList()).forEach {
                add("-$name")
                add(it)
            }
        }
    }
}

/**
 * GWT 모듈을 JavaScript로 컴파일하는 태스크입니다 (`com.google.gwt.dev.Compiler`).
 *
 * ## 역할
 * - [GwtBasePlugin]이 `gwtCompile` 이름으로 등록하며, `gwt { ... }` 설정을 사용합니다.
 * - `build` 태스크가 이 태스크에 의존합니다.
 *
 * ## 주의사항
 * - 산출물은 `war` 디렉토리에 기록되며 Gradle 빌드 캐시 대상입니다.
 *
 * @see GwtTestCompileTask 테스트 소스를 포함해 devMode 설정으로 컴파일하는 하위 태스크
 */
@CacheableTask
abstract class GwtCompileTask : GwtBaseTask() {
    init {
        description = "Compiles GWT modules to JavaScript"
        mainClass.set(COMPILER_CLASS)
    }

    companion object {
        const val COMPILER_CLASS = "com.google.gwt.dev.Compiler"
    }
}

/**
 * GWT 개발 모드를 실행하는 태스크입니다 (`com.google.gwt.dev.DevMode`).
 *
 * ## 역할
 * - [GwtBasePlugin]이 `gwtDevMode` 이름으로 등록하며, `gwt { devMode { ... } }` 설정을 사용합니다.
 * - DevMode 전용 옵션([GwtDevModeOptions])을 인자로 변환합니다.
 *
 * ## 주의사항
 * - 장시간 실행되는 대화형 태스크이므로 항상 실행되며(up-to-date 아님) 캐시되지 않습니다.
 */
@DisableCachingByDefault(because = "Long-running interactive development server")
abstract class GwtDevModeTask : GwtBaseTask() {
    @get:Input @get:Optional
    abstract val startServer: Property<Boolean>

    @get:Input @get:Optional
    abstract val port: Property<Int>

    @get:OutputDirectory @get:Optional
    abstract val logdir: DirectoryProperty

    @get:Input @get:Optional
    abstract val bindAddress: Property<String>

    @get:Input @get:Optional
    abstract val codeServerPort: Property<Int>

    @get:Input @get:Optional
    abstract val superDevMode: Property<Boolean>

    @get:Input @get:Optional
    abstract val server: Property<String>

    @get:Input @get:Optional
    abstract val startupUrl: Property<String>

    @get:Input @get:Optional
    abstract val modulePathPrefix: Property<String>

    init {
        description = "Runs GWT DevMode"
        mainClass.set(DEV_MODE_CLASS)
        outputs.upToDateWhen { false }
    }

    override fun additionalArguments(): List<String> = buildList {
        addFlag("startServer", startServer)
        addValue("port", port)
        addDirectory("logdir", logdir)
        addValue("bindAddress", bindAddress)
        addValue("codeServerPort", codeServerPort)
        addFlag("superDevMode", superDevMode)
        addValue("server", server)
        addValue("startupUrl", startupUrl)
        addValue("modulePathPrefix", modulePathPrefix)
    }

    /** 공통 옵션과 DevMode 전용 옵션을 모두 연결합니다. */
    fun configureFrom(options: GwtDevModeOptions) {
        super.configureFrom(options)
        startServer.set(options.startServer)
        port.set(options.port)
        logdir.set(options.logdir)
        bindAddress.set(options.bindAddress)
        codeServerPort.set(options.codeServerPort)
        superDevMode.set(options.superDevMode)
        server.set(options.server)
        startupUrl.set(options.startupUrl)
        modulePathPrefix.set(options.modulePathPrefix)
    }

    companion object {
        const val DEV_MODE_CLASS = "com.google.gwt.dev.DevMode"
    }
}
