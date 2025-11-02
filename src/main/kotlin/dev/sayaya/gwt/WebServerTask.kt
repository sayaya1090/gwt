package dev.sayaya.gwt

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Ktor 기반 정적 파일 웹서버 태스크
 *
 * GWT 테스트를 위한 웹서버를 별도 스레드에서 실행합니다.
 */
abstract class WebServerTask: DefaultTask() {
    @get:Input
    abstract val webserverPort: Property<Int>

    @get:Input
    abstract val webserverPath: Property<File>

    private lateinit var webServer: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    private val startupFuture = CompletableFuture<Unit>()
    private val shutdownFuture = CompletableFuture<Unit>()
    private val serverIsRunning = AtomicBoolean(false)

    @TaskAction
    fun exec() {
        thread(isDaemon = true) {
            try {
                webServer = embeddedServer(Netty, webserverPort.get()) {
                    monitor.subscribe(ServerReady) {
                        startupFuture.complete(Unit)
                        serverIsRunning.set(true)
                    }
                    monitor.subscribe(ApplicationStopped) {
                        serverIsRunning.set(false)
                        shutdownFuture.complete(Unit)
                    }
                    routing {
                        staticFiles("/", webserverPath.get())
                    }
                }

                webServer.start(wait = false)

            } catch (t: Throwable) {
                startupFuture.completeExceptionally(t)
            }
        }

        // 서버가 준비되거나 오류가 발생할 때까지 대기 (최대 30초)
        startupFuture.get(30, TimeUnit.SECONDS)
    }

    /**
     * 서버가 정상적으로 시작되었는지 확인합니다.
     * @return 서버가 초기화되고 성공적으로 시작되었으면 true
     */
    @Internal fun isRunning(): Boolean {
        return serverIsRunning.get()
    }

    fun close() {
        if (this::webServer.isInitialized && serverIsRunning.get()) {
            // grace period와 timeout을 0으로 설정하여 즉시 종료 시도
            webServer.stop(0, 0)
        }
    }
}