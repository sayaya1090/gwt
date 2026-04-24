package dev.sayaya.gwt

import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.gradle.api.services.BuildService
import org.slf4j.LoggerFactory
import java.io.File
import java.net.ServerSocket

/**
 * GWT 테스트를 위한 Ktor 기반 내장 웹 서버 서비스입니다.
 * 
 * 다중 컨텐츠 루트를 지원하여 GWT 컴파일 결과물과 사용자의 정적 리소스를 
 * 동시에 서빙할 수 있도록 설계되었습니다.
 */
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
        // 모든 루트 디렉토리 목록을 가져옴 (존재하는 디렉토리만 필터링)
        val roots = parameters.contentRoot.files.filter { it.exists() && it.isDirectory }
        val requestedPort = parameters.port.getOrElse(ServerSocket(0).use { it.localPort })

        // 포트가 0이면 미리 가용한 포트를 찾아서 할당
        val targetPort = if (requestedPort == 0) {
            ServerSocket(0).use { it.localPort }
        } else {
            requestedPort
        }

        logger.info("GWT 테스트 서버를 시작합니다...")
        roots.forEach { 
            logger.info(" - Serving Directory: ${it.absolutePath}")
        }

        try {
            // Ktor Netty 엔진 설정
            server = embeddedServer(Netty, port = targetPort, host = "127.0.0.1") {
                routing {
                    // 모든 요청에 대해 다중 루트 탐색 수행
                    get("/{staticPath...}") {
                        val relativePath = call.parameters.getAll("staticPath")?.joinToString("/") ?: ""
                        
                        // 1. 요청된 경로 그대로 찾기
                        val file = findFileInRoots(roots, relativePath)
                        if (file != null) {
                            call.respondFile(file)
                            return@get
                        }

                        // 2. 디렉토리 요청인 경우 index.html 찾기
                        val indexFile = findFileInRoots(roots, if (relativePath.isEmpty()) "index.html" else "$relativePath/index.html")
                        if (indexFile != null) {
                            call.respondFile(indexFile)
                            return@get
                        }

                        // 3. 모두 없으면 404
                        call.respond(HttpStatusCode.NotFound, "File not found in any of the registered roots: $relativePath")
                    }
                }
            }.start(wait = false)

            assignedPort = targetPort
            logger.info("GWT 테스트 서버가 실행 중입니다: http://127.0.0.1:$assignedPort")
        } catch (e: Exception) {
            logger.error("GWT 테스트 서버 시작 실패", e)
            throw e
        }
    }

    /**
     * 등록된 여러 루트 디렉토리에서 파일을 순서대로 탐색합니다.
     */
    private fun findFileInRoots(roots: List<File>, relativePath: String): File? {
        for (root in roots) {
            val file = File(root, relativePath)
            if (file.exists() && file.isFile) {
                return file
            }
        }
        return null
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
