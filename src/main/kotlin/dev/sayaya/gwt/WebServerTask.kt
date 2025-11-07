package dev.sayaya.gwt

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

/**
 * Ktor 기반 정적 파일 웹서버 태스크
 *
 * GWT 테스트를 위한 웹서버를 코루틴 기반으로 실행합니다.
 *
 * ## 주요 기능
 * - Netty 기반 비동기 웹서버 실행
 * - 코루틴을 활용한 비동기 처리
 * - 서버 시작 완료까지 대기 (기본 30초, 설정 가능)
 * - 정적 파일 서빙
 *
 * ## 사용 예시
 * 일반적으로 직접 실행하지 않으며, `test` 태스크가 자동으로 의존성을 처리합니다.
 * ```bash
 * ./gradlew openWebServer  # 수동 실행
 * ./gradlew closeWebServer # 수동 종료
 * ```
 *
 * @see GwtTestPlugin
 */
abstract class WebServerTask: DefaultTask() {
    /**
     * 웹서버가 사용할 포트 번호
     */
    @get:Input
    abstract val webserverPort: Property<Int>

    /**
     * 정적 파일이 위치한 디렉토리 경로
     */
    @get:Input
    abstract val webserverPath: Property<File>

    /**
     * 서버 시작 타임아웃 (초 단위)
     *
     * 기본값: 30초
     *
     * 서버 시작 대기 시간을 설정합니다. 이 시간 내에 서버가 시작되지 않으면
     * TimeoutCancellationException이 발생합니다.
     */
    @get:Input
    abstract val startupTimeoutSeconds: Property<Long>

    /**
     * Ktor 내장 웹서버 인스턴스
     */
    private lateinit var webServer: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    /**
     * 서버 실행을 관리하는 코루틴 스코프
     */
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 서버 시작 완료를 대기하는 Deferred
     */
    private lateinit var serverStartJob: CompletableDeferred<Unit>

    /**
     * 서버 실행 상태를 나타내는 원자적 플래그
     */
    private val serverIsRunning = AtomicBoolean(false)

    init {
        group = "GWT"
        dependsOn("gwtTestCompile")
        startupTimeoutSeconds.convention(30L)
    }

    @TaskAction
    fun exec() = runBlocking {
        serverStartJob = CompletableDeferred()

        serverScope.launch {
            try {
                webServer = embeddedServer(Netty, webserverPort.get()) {
                    monitor.subscribe(ServerReady) {
                        serverIsRunning.set(true)
                        serverStartJob.complete(Unit)
                    }
                    monitor.subscribe(ApplicationStopped) {
                        serverIsRunning.set(false)
                    }
                    routing {
                        staticFiles("/", webserverPath.get())
                    }
                }

                webServer.start(wait = false)

            } catch (t: Throwable) {
                serverStartJob.completeExceptionally(t)
            }
        }

        // 서버가 준비되거나 오류가 발생할 때까지 대기
        withTimeout(startupTimeoutSeconds.get().seconds) {
            serverStartJob.await()
        }
    }

    /**
     * 서버가 정상적으로 시작되었는지 확인합니다.
     * @return 서버가 초기화되고 성공적으로 시작되었으면 true
     */
    @Internal fun isRunning(): Boolean {
        return serverIsRunning.get()
    }

    /**
     * 웹서버를 즉시 종료합니다.
     *
     * 서버가 초기화되어 실행 중인 경우에만 종료를 시도합니다.
     * 코루틴 스코프도 함께 취소하여 모든 리소스를 정리합니다.
     *
     * 이 메서드는 `closeWebServer` 태스크에서 자동으로 호출되며,
     * 테스트가 완료되면 `finalizedBy`를 통해 반드시 실행됩니다.
     */
    fun close() {
        if (this::webServer.isInitialized && serverIsRunning.get()) {
            webServer.stop(0, 0)
        }
        serverScope.cancel()
    }
}
