package dev.sayaya.gwt

import org.gradle.api.Plugin
import org.gradle.api.Project

class GwtPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(GwtLombokPlugin::class.java)
        project.plugins.apply(GwtTestPlugin::class.java)
    }
}



/*

package dev.sayaya.gwt

import kotlinx.coroutines.*
import org.docstr.gwt.GwtPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

/**
 * GWT 테스트 자동화를 위한 메인 플러그인
 *
 * 이 플러그인은 다음을 수행합니다:
 * - Lombok과 Test 플러그인 자동 적용
 * - 웹서버와 GWT 코드서버를 통합한 gwtTest 태스크 생성
 * - Test 태스크에 자동으로 웹서버와 코드서버 통합
 *
 * ## 동작 방식
 * 1. `gwtTest` 실행 시 웹서버 → 코드서버 순서로 시작
 * 2. `test` 실행 시 웹서버 시작 → 코드서버 확인 → 테스트 실행
 * 3. 태스크 종료 시 자동으로 웹서버와 코드서버 종료
 */
class GwtPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 기존 플러그인 적용
        project.plugins.apply(GwtLombokPlugin::class.java)
        project.plugins.apply(GwtTestPlugin::class.java)

        // GWT 확장에서 devMode 설정 가져오기
        val gwtExtension = project.extensions.getByType(GwtPluginExtension::class.java)

        // 웹서버 및 GWT 테스트 통합
        val gwtTest = registerGwtTest(project)
        val openWebserver = registerOpenWebserver(project, gwtExtension)
        registerCloseWebserver(project, openWebserver)

        // gwtTest가 openWebserver에 의존하도록 설정
        gwtTest.configure {
            dependsOn(openWebserver)
            finalizedBy("closeWebServer")
        }

        injectGwtTestToTestTask(project, openWebserver, gwtTest)
    }

    /**
     * GWT 테스트 태스크 등록 (코드서버)
     */
    private fun registerGwtTest(project: Project): TaskProvider<GwtTest> =
        project.tasks.register("gwtTest", GwtTest::class.java)

    /**
     * 웹서버를 시작하는 태스크 등록
     */
    private fun registerOpenWebserver(project: Project, gwtExtension: GwtPluginExtension): TaskProvider<WebServerTask> =
        project.tasks.register("openWebServer", WebServerTask::class.java) {
            // GWT 확장의 devMode 설정을 직접 참조
            webserverPort.convention(9876)
            webserverPath.set(gwtExtension.dev.war.map { it.asFile })
        }

    /**
     * 웹서버를 종료하는 태스크 등록
     */
    private fun registerCloseWebserver(project: Project, openWebserver: TaskProvider<WebServerTask>): TaskProvider<Task> =
        project.tasks.register("closeWebServer") {
            doLast { openWebserver.get().close() }
        }

    /**
     * Test 태스크에 GWT 테스트 통합
     */
    private fun injectGwtTestToTestTask(project: Project, openWebserver: TaskProvider<WebServerTask>, gwtTest: TaskProvider<GwtTest>) {
        project.tasks.withType(Test::class.java).configureEach {
            useJUnitPlatform()
            dependsOn(openWebserver)

            // 코드서버를 백그라운드 프로세스로 시작
            doFirst {
                val gwtTestTask = gwtTest.get()
                val javaLauncher = gwtTestTask.javaLauncher.get()

                // 프로세스 빌더로 백그라운드 실행
                val processBuilder = ProcessBuilder().apply {
                    command(
                        javaLauncher.executablePath.asFile.absolutePath,
                        *gwtTestTask.jvmArgs.toTypedArray(),
                        "-cp", gwtTestTask.classpath.asPath,
                        "com.google.gwt.dev.codeserver.CodeServer",
                        *gwtTestTask.args.toTypedArray()
                    )
                    redirectErrorStream(true)
                }

                val codeServerProcess = processBuilder.start()
                project.gradle.buildFinished {
                    codeServerProcess.destroy()
                }

                // 코드서버 준비 대기
                checkCodeServerReady(gwtTestTask.port.getOrElse(8888))
            }

            finalizedBy("closeWebServer")
        }
    }

    /**
     * 코드서버가 준비될 때까지 대기
     */
    @OptIn(DelicateCoroutinesApi::class)
    internal fun checkCodeServerReady(port: Int) {
        val codeserverUrl = URI("http://127.0.0.1:$port/").toURL()
        val job = GlobalScope.launch {
            repeat(100) {
                delay(1000)
                try {
                    val conn = codeserverUrl.openConnection() as HttpURLConnection
                    conn.requestMethod = "HEAD"
                    conn.connect()
                    val responseCode = conn.responseCode
                    if(responseCode == 200) {
                        this.cancel("Codeserver prepared")
                        return@launch
                    }
                } catch(e: IOException) {
                    // 코드서버가 아직 준비되지 않음
                }
            }
        }
        runBlocking {
            job.join()
        }
    }
}
 */