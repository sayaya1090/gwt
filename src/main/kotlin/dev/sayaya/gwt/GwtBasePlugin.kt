package dev.sayaya.gwt

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test

/**
 * GWT 빌드의 기반을 구성하는 플러그인입니다. 외부 `org.docstr.gwt` 플러그인을 대체합니다.
 *
 * ## 역할
 * - `java` 플러그인 적용
 * - `gwt` 확장([GwtPluginExtension]) 생성 및 기본값 설정
 *   (war/deploy/gen/extra/cacheDir = `build/gwt/...`, gwtVersion = [DEFAULT_GWT_VERSION])
 * - `compiler`·`devMode` 공통 옵션이 미설정 시 상위 `gwt` 값을 이어받도록 convention 연결
 * - `gwtDev`(의존성 선언용, `implementation` 상속) / `gwtDevRuntimeClasspath`(해석용) 설정 생성
 * - 평가 후 GWT 의존성 추가: `implementation`에 `org.gwtproject:gwt` BOM과 `gwt-servlet`(또는 jakarta),
 *   `gwtDev`에 BOM과 `gwt-dev`, `gwt-user`, `gwt-codeserver`
 * - `gwt.extraSourceDirs`를 `main` Java 소스 디렉토리에 추가
 * - `gwtCompile`([GwtCompileTask], `build`가 의존), `gwtDevMode`([GwtDevModeTask]) 태스크 등록
 * - 평가 후 모든 `Test` 태스크 구성(이전 `org.docstr.gwt`의 기본 동작과 동일):
 *   힙 크기(devMode → gwt → 256M/512M), `gwt.args`(`-war -deploy -extra`), `gwt.persistentunitcachedir`,
 *   main·test 소스/클래스/리소스를 테스트 클래스패스 앞에 추가, `<test>Implementation`이 `gwtDevRuntimeClasspath` 상속
 *
 * ## 주요 의존관계
 * - [GwtLombokPlugin], [GwtTestPlugin]이 내부적으로 적용합니다. 별도 플러그인 ID는 공개하지 않습니다.
 *
 * ## 주의사항
 * - 의존성은 사용자 설정(`gwtVersion`, `jakarta`)을 반영하기 위해 `afterEvaluate`에서 추가됩니다.
 */
class GwtBasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)
        configureConfigurations(project)
        val extension = createExtension(project)
        project.afterEvaluate {
            configureDependencies(project, extension)
            configureTestTasks(project, extension)
        }
        registerTasks(project, extension)
    }

    private fun configureConfigurations(project: Project) {
        val configurations = project.configurations
        val implementation = configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
        val gwtDev = configurations.create(GWT_DEV_CONFIGURATION_NAME) {
            isCanBeResolved = false
            isCanBeConsumed = false
            extendsFrom(implementation)
        }
        configurations.create(GWT_DEV_RUNTIME_CLASSPATH_CONFIGURATION_NAME) {
            isCanBeResolved = true
            isCanBeConsumed = false
            extendsFrom(gwtDev)
        }
    }

    private fun createExtension(project: Project): GwtPluginExtension {
        val extension = project.extensions.create(EXTENSION_NAME, GwtPluginExtension::class.java)
        val buildDir = project.layout.buildDirectory
        extension.gwtVersion.convention(DEFAULT_GWT_VERSION)
        extension.jakarta.convention(false)
        extension.war.convention(buildDir.dir("gwt/war"))
        extension.deploy.convention(buildDir.dir("gwt/deploy"))
        extension.gen.convention(buildDir.dir("gwt/gen"))
        extension.extra.convention(buildDir.dir("gwt/extra"))
        extension.cacheDir.convention(buildDir.dir("gwt/gwt-unitCache"))
        extension.compiler.inheritFrom(extension)
        extension.devMode.inheritFrom(extension)
        return extension
    }

    private fun configureDependencies(project: Project, extension: GwtPluginExtension) {
        val dependencies = project.dependencies
        val bom = "org.gwtproject:gwt:${extension.gwtVersion.get()}"
        val servlet = if (extension.jakarta.get()) "org.gwtproject:gwt-servlet-jakarta" else "org.gwtproject:gwt-servlet"
        dependencies.add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, dependencies.platform(bom))
        dependencies.add(GWT_DEV_CONFIGURATION_NAME, dependencies.platform(bom))
        dependencies.add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, servlet)
        dependencies.add(GWT_DEV_CONFIGURATION_NAME, "org.gwtproject:gwt-dev")
        dependencies.add(GWT_DEV_CONFIGURATION_NAME, "org.gwtproject:gwt-user")
        dependencies.add(GWT_DEV_CONFIGURATION_NAME, "org.gwtproject:gwt-codeserver")

        val extraSourceDirs = extension.extraSourceDirs.files
        if (extraSourceDirs.isNotEmpty()) {
            project.extensions.getByType(SourceSetContainer::class.java)
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                .java.srcDirs(extraSourceDirs)
        }
    }

    /**
     * GWT 테스트(`GWTTestCase` 등)가 동작하도록 모든 [Test] 태스크를 구성합니다.
     * `gwt { }`·`devMode { }` 설정을 읽어야 하므로 `afterEvaluate`에서 호출됩니다.
     */
    private fun configureTestTasks(project: Project, extension: GwtPluginExtension) {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        val test = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)
        val gwtDevRuntimeClasspath = project.configurations.getByName(GWT_DEV_RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        project.tasks.withType(Test::class.java).configureEach {
            minHeapSize = extension.devMode.minHeapSize.getOrElse(GwtBaseTask.DEFAULT_MIN_HEAP_SIZE)
            maxHeapSize = extension.devMode.maxHeapSize.getOrElse(GwtBaseTask.DEFAULT_MAX_HEAP_SIZE)

            project.configurations.findByName("${name}Implementation")?.extendsFrom(gwtDevRuntimeClasspath)
            classpath = project.files(
                main.allSource.srcDirs, main.output.classesDirs, main.output.resourcesDir,
                test.allSource.srcDirs, test.output.classesDirs, test.output.resourcesDir
            ) + classpath

            systemProperty("gwt.args", gwtTestArguments(extension))
            extension.cacheDir.orNull?.asFile?.let { cacheDir ->
                systemProperty("gwt.persistentunitcachedir", cacheDir)
                doFirst { cacheDir.mkdirs() }
            }
        }
    }

    private fun registerTasks(project: Project, extension: GwtPluginExtension) {
        val gwtCompile = project.tasks.register(GWT_COMPILE_TASK_NAME, GwtCompileTask::class.java) {
            configureFrom(extension.compiler)
            configureCompilerFrom(extension.compiler)
            extraSourceDirs.from(extension.extraSourceDirs, extension.compiler.extraSourceDirs)
        }
        project.tasks.named("build") { dependsOn(gwtCompile) }

        project.tasks.register(GWT_DEV_MODE_TASK_NAME, GwtDevModeTask::class.java) {
            configureFrom(extension.devMode)
            extraSourceDirs.from(extension.extraSourceDirs, extension.devMode.extraSourceDirs)
        }
    }

    companion object {
        const val EXTENSION_NAME = "gwt"
        const val GWT_DEV_CONFIGURATION_NAME = "gwtDev"
        const val GWT_DEV_RUNTIME_CLASSPATH_CONFIGURATION_NAME = "gwtDevRuntimeClasspath"
        const val GWT_COMPILE_TASK_NAME = "gwtCompile"
        const val GWT_DEV_MODE_TASK_NAME = "gwtDevMode"
        const val DEFAULT_GWT_VERSION = "2.12.1"

        /** 테스트 JVM의 `gwt.args` 값: 상위 `gwt`의 war/deploy/extra를 `-name <절대경로>` 형식으로 공백 연결합니다. */
        internal fun gwtTestArguments(extension: GwtPluginExtension): String = listOf(
            "-war" to extension.war, "-deploy" to extension.deploy, "-extra" to extension.extra
        ).mapNotNull { (flag, dir) -> dir.orNull?.let { "$flag ${it.asFile.absolutePath}" } }
            .joinToString(" ")

        /** devMode의 공통 옵션이 설정되지 않았을 때 상위 [base] 값을 사용하도록 convention을 연결합니다. */
        private fun GwtOptions.inheritFrom(base: GwtOptions) {
            minHeapSize.convention(base.minHeapSize)
            maxHeapSize.convention(base.maxHeapSize)
            logLevel.convention(base.logLevel)
            workDir.convention(base.workDir)
            gen.convention(base.gen)
            war.convention(base.war)
            deploy.convention(base.deploy)
            extra.convention(base.extra)
            cacheDir.convention(base.cacheDir)
            generateJsInteropExports.convention(base.generateJsInteropExports)
            includeJsInteropExports.convention(base.includeJsInteropExports)
            excludeJsInteropExports.convention(base.excludeJsInteropExports)
            methodNameDisplayMode.convention(base.methodNameDisplayMode)
            sourceLevel.convention(base.sourceLevel)
            incremental.convention(base.incremental)
            style.convention(base.style)
            failOnError.convention(base.failOnError)
            setProperty.convention(base.setProperty)
            modules.convention(base.modules)
        }
    }
}
