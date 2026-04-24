
package dev.sayaya.gwt

import org.docstr.gwt.GwtDevModeTask
import org.docstr.gwt.GwtPluginExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

/**
 * GWT 테스트 컴파일 및 개발 모드를 설정하는 플러그인입니다.
 *
 * ## 주요 기능
 * - 기본 `org.docstr.gwt` 플러그인 적용
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

        registerGenerateHtmlTask(project, extension)
        registerGwtTestCompileTask(project)
        registerWebServerService(project, extension)

        configureGwtDevMode(project)
        configureJavaCompile(project)
        configureTestTasks(project)
        configureWarTask(project)
    }

    /**
     * 필수 플러그인을 적용하고 기본 설정을 수행합니다.
     */
    private fun applyRequiredPlugins(project: Project) {
        project.plugins.apply("java")
        project.plugins.apply("org.docstr.gwt")

        // Jakarta Servlet API 충돌 방지 (공통 유틸리티 함수 사용)
        project.excludeJakartaServletApi()
    }

    /**
     * HTML 호스트 파일 생성 태스크를 등록합니다.
     */
    private fun registerGenerateHtmlTask(
        project: Project,
        extension: GwtPluginExtension
    ): TaskProvider<GwtGenerateTestHtmlTask> =
        project.tasks.register("gwtGenerateTestHtml", GwtGenerateTestHtmlTask::class.java, Action<GwtGenerateTestHtmlTask> {
            modules.set(extension.devMode.modules.orElse(extension.modules))
            war.set(extension.devMode.war.orElse(extension.war))
        })

    /**
     * GWT 테스트 컴파일 태스크를 등록합니다.
     */
    private fun registerGwtTestCompileTask(project: Project): TaskProvider<GwtTestCompileTask> =
        project.tasks.register("gwtTestCompile", GwtTestCompileTask::class.java)

    /**
     * 웹 서버 빌드 서비스를 등록하고 테스트 태스크가 이를 사용하도록 구성합니다.
     */
    private fun registerWebServerService(project: Project, extension: GwtPluginExtension) {
        val gwtTestCompile = project.tasks.named("gwtTestCompile", GwtTestCompileTask::class.java)

        // 빌드 서비스 등록
        val webServerServiceProvider = project.gradle.sharedServices.registerIfAbsent(
            "gwtWebServer-${project.name}",
            WebServerService::class.java
        ) {
            // 1. 사용자가 설정한 war 디렉토리를 추가 (소스 리소스)
            val warDir = extension.devMode.war.orElse(extension.war)
            parameters.contentRoot.from(warDir)

            // 2. GWT 컴파일러의 실제 출력 디렉토리를 추가 (컴파일된 JS)
            // GwtTestCompileTask의 war 프로퍼티가 가리키는 곳이 실제 번들이 생성되는 위치임
            parameters.contentRoot.from(gwtTestCompile.flatMap { it.war })
        }
        project.tasks.withType(Test::class.java).configureEach {
            this.usesService(webServerServiceProvider)
            val urlProvider = webServerServiceProvider.map { service ->
                "http://127.0.0.1:${service.getPort()}/"
            }
            // 시스템 프로퍼티로 URL 전달 (GWT 테스트 러너가 이를 읽어서 사용)
            systemProperty("gwt.junit.remoteUrl", urlProvider)
            // Ktor가 사용하는 Netty의 리소스 누수 탐지기 설정
            systemProperty("io.netty.leakDetection.level", "PARANOID")
        }
    }

    /**
     * GWT 개발 모드의 extraSourceDirs를 구성합니다.
     *
     * 테스트 소스와 리소스를 extension.devMode.extraSourceDirs에 추가하여
     * 개발 모드에서도 테스트 코드를 사용할 수 있도록 설정합니다.
     *
     * **중요:** afterEvaluate에서 호출되어 사용자 설정 이후에 적용됩니다.
     *           사용자가 이미 extraSourceDirs를 설정했다면, 여기서 추가로 병합됩니다.
     */
    private fun configureGwtDevMode(project: Project) {
        project.tasks.named("gwtDevMode", GwtDevModeTask::class.java).configure {
            dependsOn("gwtGenerateTestHtml")
        }

        project.afterEvaluate {
            val extension = project.extensions.getByType(GwtPluginExtension::class.java)
            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            val mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            val testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)

            extension.devMode.extraSourceDirs.from(
                mainSourceSet.allSource.sourceDirectories,
                mainSourceSet.resources.sourceDirectories,
                mainSourceSet.output,
                mainSourceSet.runtimeClasspath,

                testSourceSet.allSource.sourceDirectories,
                testSourceSet.resources.sourceDirectories,
                testSourceSet.output,
                testSourceSet.runtimeClasspath
            )
        }
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
            dependsOn("gwtTestCompile")
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
}