package dev.sayaya.gwt

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import org.gradle.api.services.BuildService
import org.slf4j.LoggerFactory
import java.net.ServerSocket

abstract class WebServerService : BuildService<WebServerParameters>, AutoCloseable {

    private val logger = LoggerFactory.getLogger(WebServerService::class.java)

    // Ktor 서버 인스턴스 홀더
    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    // 실제로 바인딩된 포트 번호 (동적 할당 시 필요)
    private var assignedPort: Int = -1

    init {
        // 서비스 인스턴스 생성 시 서버 시작
        startServer()
    }

    private fun startServer() {
        val rootDir = parameters.contentRoot.get().asFile
        val requestedPort = parameters.port.getOrElse(ServerSocket(0).use { it.localPort })

        // 포트가 0이면 미리 가용한 포트를 찾아서 할당
        val targetPort = if (requestedPort == 0) {
            ServerSocket(0).use { it.localPort }
        } else {
            requestedPort
        }

        logger.info("GWT 테스트 서버를 시작합니다... (Root: ${rootDir.absolutePath})")
        try {
            // Ktor Netty 엔진 설정
            server = embeddedServer(Netty, port = targetPort, host = "127.0.0.1") {
                routing {
                    // 정적 파일 서빙 설정
                    staticFiles("/", rootDir) {
                        default("index.html")
                        enableAutoHeadResponse()
                    }
                }
            }.start(wait = false) // wait=false로 비동기 시작

            assignedPort = targetPort
            logger.info("GWT 테스트 서버가 실행 중입니다: http://127.0.0.1:$assignedPort")
        } catch (e: Exception) {
            logger.error("GWT 테스트 서버 시작 실패", e)
            throw e
        }
    }

    // 테스트 태스크(클라이언트)에서 포트 정보를 가져가기 위한 메서드
    fun getPort(): Int = assignedPort

    // 리소스 정리 (AutoCloseable)
    override fun close() {
        logger.info("GWT 테스트 서버를 종료합니다...")
        // 1초 대기 후 최대 2초 내 강제 종료
        server.stop(1000, 2000)
        logger.info("서버가 안전하게 종료되었습니다.")
    }
}