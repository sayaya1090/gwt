package dev.sayaya.gwt

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

/**
 * GWT 테스트 컴파일 및 개발 모드를 설정하는 플러그인입니다.
 *
 * ## 주요 기능
 * - 기반 플러그인([GwtBasePlugin]) 적용: `gwt` 확장, `gwtCompile`·`gwtDevMode` 태스크, GWT 의존성
 * - GWT 테스트 모듈 컴파일을 위한 태스크 등록
 * - 테스트 소스를 포함하도록 개발 모드 설정
 * - Gradle Build Service를 이용한 웹 서버 자동 생명주기 관리
 * - Java 컴파일 UTF-8 인코딩 설정
 *
 * ## 태스크 실행 흐름
 * ```
 * test
 * ├── uses: WebServerService
 * ├── dependsOn: gwtTestCompile
 * │   └── dependsOn: gwtGenerateTestHtml
 *
 * gwtDevMode
 * └── dependsOn: gwtGenerateTestHtml
 * ```
 *
 * @see GwtTestCompileTask
 * @see GwtGenerateTestHtmlTask
 * @see WebServerService
 */
class GwtTestPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        applyRequiredPlugins(project)

        val extension = project.extensions.getByType(GwtPluginExtension::class.java)
        // gwt { test { webPort = ... } } 하위 블록
        val testExtension = (extension as ExtensionAware).extensions
            .create(GwtTestTaskExtension::class.java, "test", GwtTestTaskExtension::class.java)

        registerGenerateHtmlTask(project, extension)
        registerGwtTestCompileTask(project, extension)
        registerWebServerService(project, extension, testExtension)

        configureGwtDevMode(project)
        configureJavaCompile(project)
        configureTestTasks(project)
        configureWarTask(project)
    }

    /**
     * 필수 플러그인을 적용하고 기본 설정을 수행합니다.
     */
    private fun applyRequiredPlugins(project: Project) {
        // java 플러그인과 Jakarta Servlet API 제외는 GwtBasePlugin이 처리한다.
        project.plugins.apply(GwtBasePlugin::class.java)
    }

    /**
     * HTML 호스트 파일 생성 태스크를 등록합니다.
     */
    private fun registerGenerateHtmlTask(
        project: Project,
        extension: GwtPluginExtension
    ): TaskProvider<GwtGenerateTestHtmlTask> =
        project.tasks.register(GWT_GENERATE_TEST_HTML_TASK_NAME, GwtGenerateTestHtmlTask::class.java, Action<GwtGenerateTestHtmlTask> {
            modules.set(extension.devMode.modules.orElse(extension.modules))
            war.set(extension.devMode.war.orElse(extension.war))
        })

    /**
     * GWT 테스트 컴파일 태스크를 등록합니다.
     *
     * 공통 옵션은 `devMode { }`(미지정 시 상위 `gwt`), 컴파일러 전용 옵션은 `compiler { }`를 따르며,
     * HTML 호스트 파일 생성 이후에 실행됩니다. `gwtCompile`과 같이 등록 시점에 구성합니다.
     */
    private fun registerGwtTestCompileTask(
        project: Project,
        extension: GwtPluginExtension
    ): TaskProvider<GwtTestCompileTask> =
        project.tasks.register(GWT_TEST_COMPILE_TASK_NAME, GwtTestCompileTask::class.java) {
            dependsOn(GWT_GENERATE_TEST_HTML_TASK_NAME)
            configureFrom(extension.devMode)
            configureCompilerFrom(extension.compiler)
            extraSourceDirs.from(extension.extraSourceDirs, extension.devMode.extraSourceDirs)
        }

    /**
     * 웹 서버 빌드 서비스를 등록하고 테스트 태스크가 이를 사용하도록 구성합니다.
     */
    private fun registerWebServerService(
        project: Project,
        extension: GwtPluginExtension,
        testExtension: GwtTestTaskExtension
    ) {
        val gwtTestCompile = project.tasks.named(GWT_TEST_COMPILE_TASK_NAME, GwtTestCompileTask::class.java)

        // 빌드 서비스 등록
        val webServerServiceProvider = project.gradle.sharedServices.registerIfAbsent(
            webServerServiceName(project),
            WebServerService::class.java
        ) {
            // gwt { test { webPort } }. 미지정 또는 0이면 서버가 빈 포트를 고른다.
            parameters.port.set(testExtension.webPort)

            // 사용자가 설정한 war 디렉토리 (소스 리소스)
            val warDir = extension.devMode.war.orElse(extension.war)
            parameters.contentRoot.from(warDir)

            // gwtTestCompile의 실제 출력 디렉토리 (번들 JS 및 복사된 테스트 리소스 포함)
            parameters.contentRoot.from(gwtTestCompile.flatMap { it.war })
        }

        project.tasks.withType(Test::class.java).configureEach {
            this.usesService(webServerServiceProvider)
            
            // 서버는 처음 쓰일 때 기동하므로, 실제 포트는 테스트 JVM 인자를 만들 때 지연 조회한다.
            jvmArgumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
                val service = webServerServiceProvider.get()
                listOf("-Dgwt.junit.remoteUrl=http://127.0.0.1:${service.getPort()}/")
            })
        }
    }

    /**
     * GWT 개발 모드의 extraSourceDirs를 구성합니다.
     *
     * 테스트 소스·리소스 디렉토리와 테스트 런타임 클래스패스(테스트·main 출력과 의존성 포함)를
     * extension.devMode.extraSourceDirs에 추가하여, gwtDevMode와 gwtTestCompile이 테스트 코드를 사용할 수 있게 합니다.
     * main 소스 디렉토리와 출력은 [GwtBaseTask]가 이미 클래스패스에 넣으므로 다시 추가하지 않습니다.
     *
     * 지연 평가되는 FileCollection이므로 사용자가 나중에 추가한 extraSourceDirs와도 합산됩니다.
     */
    private fun configureGwtDevMode(project: Project) {
        project.tasks.named(GwtBasePlugin.GWT_DEV_MODE_TASK_NAME, GwtDevModeTask::class.java).configure {
            dependsOn(GWT_GENERATE_TEST_HTML_TASK_NAME)
        }

        val extension = project.extensions.getByType(GwtPluginExtension::class.java)
        val testSourceSet = project.extensions.getByType(SourceSetContainer::class.java)
            .getByName(SourceSet.TEST_SOURCE_SET_NAME)
        extension.devMode.extraSourceDirs.from(
            testSourceSet.allSource.sourceDirectories,
            testSourceSet.runtimeClasspath
        )
    }

    /**
     * Java 컴파일 태스크를 구성합니다.
     *
     * - UTF-8 인코딩 설정
     * - processTestResources 의존성 추가 (리소스 먼저 처리)
     */
    private fun configureJavaCompile(project: Project) {
        project.tasks.withType(JavaCompile::class.java).configureEach {
            options.encoding = "UTF-8"
        }

        // 컴파일 전에 테스트 리소스 처리
        project.tasks.named("compileJava") {
            inputs.files(project.tasks.named("processTestResources"))
        }
    }

    /**
     * Test 태스크를 구성합니다.
     *
     * - JUnit Platform 사용 설정
     * - 웹 서버 시작/종료 의존성 설정
     */
    private fun configureTestTasks(project: Project) {
        project.tasks.withType(Test::class.java).configureEach {
            useJUnitPlatform()
            dependsOn(GWT_TEST_COMPILE_TASK_NAME)
        }
    }

    /**
     * war 태스크를 구성합니다.
     *
     * war 플러그인이 적용된 경우, war 태스크가 test에 의존하도록 설정합니다.
     */
    private fun configureWarTask(project: Project) {
        project.plugins.withId("war") {
            project.tasks.named("war") {
                dependsOn("test")
            }
        }
    }

    companion object {
        const val GWT_GENERATE_TEST_HTML_TASK_NAME = "gwtGenerateTestHtml"
        const val GWT_TEST_COMPILE_TASK_NAME = "gwtTestCompile"

        /**
         * 프로젝트별 [WebServerService] 등록 이름.
         *
         * 빌드 서비스는 이름으로 공유되므로 빌드 안에서 유일한 [Project.getPath]를 사용합니다
         * (예: `:a:client` → `gwtWebServer:a:client`). 루트 프로젝트는 기존 이름(`gwtWebServer-<name>`)을 유지합니다.
         */
        internal fun webServerServiceName(project: Project): String =
            if (project == project.rootProject) "gwtWebServer-${project.name}" else "gwtWebServer${project.path}"
    }
}