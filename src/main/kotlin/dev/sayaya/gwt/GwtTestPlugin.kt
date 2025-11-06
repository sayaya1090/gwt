package dev.sayaya.gwt

import org.docstr.gwt.GwtDevModeConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.docstr.gwt.GwtPluginExtension
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.exclude

/**
 * GWT 테스트 컴파일 및 개발 모드를 설정하는 플러그인
 *
 * 이 플러그인은 다음을 수행합니다:
 * - 기본 `org.docstr.gwt` 플러그인 적용
 * - GWT 테스트 모듈 컴파일을 위한 `gwtTestCompile` 태스크 등록
 * - 테스트 소스를 포함하도록 `gwtDevMode` 설정
 * - 태스크 의존성 설정 (test는 gwtTestCompile에 의존, war는 gwtCompile에 의존)
 * - Java 컴파일에 UTF-8 인코딩 설정
 *
 * ## 동작 방식
 * 1. `gwtTest` 실행 시 웹서버 → 코드서버 순서로 시작
 * 2. `test` 실행 시 웹서버 시작 → 코드서버 구동 확인 → 테스트 실행
 * 3. 태스크 종료 시 자동으로 웹서버와 코드서버 종료
 */
class GwtTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 1. 필수 플러그인 적용 및 기본 설정
        project.plugins.apply("java")
        project.plugins.apply("org.docstr.gwt")
        project.configurations.all {
            exclude(group = "jakarta.servlet", module = "jakarta.servlet-api")
        }

        // 2. 태스크 등록
        val extension = project.extensions.getByType(GwtPluginExtension::class.java)
        val configureClasspathTask = registerConfigureClasspathTask(project)
        val gwtCompileTask = project.tasks.named("gwtCompile")
        val gwtDevModeTask = project.tasks.named("gwtDevMode")
        val processTestResourcesTask = project.tasks.named("processTestResources")
        val gwtTestCompileTask = project.tasks.register("gwtTestCompile", GwtTestCompileTask::class.java, GwtTestCompileConfig(extension))
        val openWebserverTask = registerOpenWebserverTask(project, extension)
        val closeWebserverTask = registerCloseWebserver(project, openWebserverTask)
        val gwtTestTask = registerGwtTest(project)
        val gwtGenerateHtmlTask = generateHtmlTask(project)
        gwtGenerateHtmlTask.configure {
            dependsOn(processTestResourcesTask)
        }
        gwtTestCompileTask.configure {
            dependsOn(configureClasspathTask)
            dependsOn(gwtGenerateHtmlTask)
        }
        gwtDevModeTask.configure {
            dependsOn(configureClasspathTask)
            dependsOn(gwtGenerateHtmlTask)
        }

        // 3. 태스크 설정 및 의존성 연결
        configureJavaCompile(project)
        configureGwtTestTask(project, openWebserverTask)
        configureTestTasks(project, gwtTestTask)
        configureWarTask(project)
    }

    /**
     * 클래스패스 설정 태스크 등록
     */
    private fun registerConfigureClasspathTask(project: Project): TaskProvider<GwtConfigureTestClasspathTask> =
        project.tasks.register("gwtConfigureTestClasspath", GwtConfigureTestClasspathTask::class.java)


    /**
     * GWT 테스트 태스크 등록 (코드서버)
     */
    private fun registerGwtTest(project: Project): TaskProvider<GwtTestTask> =
        project.tasks.register("gwtTest", GwtTestTask::class.java)

    // HTML 생성 태스크 등록
    private fun generateHtmlTask(project: Project): TaskProvider<GwtGenerateTestHtmlTask> =
        project.tasks.register("gwtGenerateTestHtml", GwtGenerateTestHtmlTask::class.java, Action<GwtGenerateTestHtmlTask> {
            val extension = project.extensions.getByType(GwtPluginExtension::class.java)
            this.modules.set(extension.devMode.modules.orElse(extension.modules))
            this.war.set(extension.devMode.war.orElse(extension.war))
        })

    /**
     * WebServerTask를 등록하고 기본 설정을 수행합니다.
     * - 'openWebServer' 태스크의 포트를 'test' 태스크의 'gwt' 확장('webPort')에 연결합니다.
     */
    private fun registerOpenWebserverTask(project: Project, gwtExtension: GwtPluginExtension): TaskProvider<WebServerTask> {
        val task = project.tasks.register("openWebServer", WebServerTask::class.java) {
            webserverPath.set(gwtExtension.war.map { it.asFile })
        }
        project.tasks.withType(Test::class.java).configureEach {
            extensions.create("gwt", GwtTestTaskExtension::class.java)
        }
        task.configure {
            val testTaskProvider = project.tasks.named("test", Test::class.java)
            val webPortProvider = testTaskProvider.flatMap { testTask ->
                testTask.extensions.getByType(GwtTestTaskExtension::class.java).webPort
            }
            webserverPort.set(webPortProvider)
        }
        return task
    }

    /**
     * 웹서버를 종료하는 태스크 등록
     */
    private fun registerCloseWebserver(project: Project, openWebserver: TaskProvider<WebServerTask>): TaskProvider<Task> =
        project.tasks.register("closeWebServer") {
            doLast {
                openWebserver.get().close()
            }
        }

    /**
     * 모든 JavaCompile 태스크에 UTF-8 인코딩을 설정합니다.
     */
    private fun configureJavaCompile(project: Project) {
        project.tasks.withType(JavaCompile::class.java) {
            options.encoding = "UTF-8"
        }
    }

    /**
     * 'gwtTest' 태스크 관련 설정을 수행합니다.
     * - 'gwtTest' 태스크가 'openWebServer' 태스크에 의존하도록 설정합니다.
     */
    private fun configureGwtTestTask(project: Project, openWebserverTask: TaskProvider<WebServerTask>) {
        project.tasks.named("gwtTest") {
            dependsOn(openWebserverTask)
        }
    }

    /**
     * Test 태스크 관련 설정을 수행합니다.
     * - 'gwt' 확장 생성
     * - 'gwtTestCompile' 태스크 의존성 추가
     */
    private fun configureTestTasks(project: Project, gwtTestTask: TaskProvider<GwtTestTask>) {
        project.tasks.withType(Test::class.java).configureEach {
            useJUnitPlatform()
            dependsOn(gwtTestTask)
        }
    }

    /**
     * 'war' 태스크가 존재할 경우 'test' 태스크에 대한 의존성을 설정합니다.
     */
    private fun configureWarTask(project: Project) {
        project.plugins.withId("war") {
            project.tasks.named("war") {
                dependsOn("test")
            }
        }
    }
}