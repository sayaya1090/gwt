
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
 * - 웹 서버 자동 시작/종료
 * - Java 컴파일 UTF-8 인코딩 설정
 *
 * ## 태스크 실행 흐름
 * ```
 * test
 * ├── dependsOn: openWebServer
 * ├── dependsOn: gwtTestCompile
 * │   └── dependsOn: gwtGenerateTestHtml
 * └── finalizedBy: closeWebServer
 *
 * gwtDevMode
 * └── dependsOn: gwtGenerateTestHtml
 * ```
 *
 * @see GwtTestCompileTask
 * @see GwtGenerateTestHtmlTask
 */
class GwtTestPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        applyRequiredPlugins(project)

        val extension = project.extensions.getByType(GwtPluginExtension::class.java)

        registerGenerateHtmlTask(project, extension)
        registerGwtTestCompileTask(project)
        registerWebServerTasks(project, extension)

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
     * 웹 서버 시작/종료 태스크를 등록하고 구성합니다.
     */
    private fun registerWebServerTasks(project: Project, extension: GwtPluginExtension) {
        // Test 태스크에 gwt 확장 추가
        project.tasks.withType(Test::class.java).configureEach {
            extensions.create("gwt", GwtTestTaskExtension::class.java)
        }

        // 웹 서버 시작 태스크
        val openWebServerTask = project.tasks.register("openWebServer", WebServerTask::class.java) {
            webserverPath.set(extension.war.map { it.asFile })

            // Test 태스크의 webPort 설정과 연결
            val testTaskProvider = project.tasks.named("test", Test::class.java)
            val webPortProvider = testTaskProvider.flatMap { testTask ->
                testTask.extensions.getByType(GwtTestTaskExtension::class.java).webPort
            }
            webserverPort.set(webPortProvider)
        }

        // 웹 서버 종료 태스크
        project.tasks.register("closeWebServer") {
            doLast {
                openWebServerTask.get().close()
            }
        }
    }

    /**
     * GWT 개발 모드 태스크를 구성합니다.
     *
     * 테스트 소스와 리소스를 extraSourceDirs에 추가하여
     * 개발 모드에서도 테스트 코드를 사용할 수 있도록 설정합니다.
     *
     * **중요:** extension을 직접 수정하지 않고 태스크의 extraSourceDirs만 수정합니다.
     */
    private fun configureGwtDevMode(project: Project) {
        project.tasks.named("gwtDevMode", GwtDevModeTask::class.java).configure {
            dependsOn("gwtGenerateTestHtml")

            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            val testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)

            extraSourceDirs.from(
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
            dependsOn("openWebServer")
            finalizedBy("closeWebServer")
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