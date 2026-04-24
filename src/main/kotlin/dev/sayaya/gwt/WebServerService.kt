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
 * 다중 컨텐츠 루트를 지원하며, 요청 시 등록된 경로를 순차적으로 탐색합니다.
 */
abstract class WebServerService : BuildService<WebServerParameters>, AutoCloseable {

    private val logger = LoggerFactory.getLogger(WebServerService::class.java)

    // Ktor 서버 인스턴스 홀더
    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    // 실제로 바인딩된 포트 번호
    private var assignedPort: Int = -1

    init {
        startServer()
    }

    private fun startServer() {
        val roots = parameters.contentRoot.files.filter { it.exists() && it.isDirectory }
        val requestedPort = parameters.port.getOrElse(ServerSocket(0).use { it.localPort })
        val targetPort = if (requestedPort == 0) ServerSocket(0).use { it.localPort } else requestedPort

        // 서버 기동 시 서빙 경로를 강력하게 출력 (LIFECYCLE 수준)
        println("Starting GWT Test Server...")
        roots.forEach { 
            println(" > Serving Directory: ${it.absolutePath}")
        }

        try {
            server = embeddedServer(Netty, port = targetPort, host = "127.0.0.1") {
                routing {
                    // 수동 경로 탐색 (DuplicatePluginException 방지)
                    get("/{staticPath...}") {
                        val pathSegments = call.parameters.getAll("staticPath") ?: emptyList()
                        val relativePath = pathSegments.joinToString("/")
                        
                        val file = findFile(roots, relativePath)
                        if (file != null) {
                            call.respondFile(file)
                        } else {
                            // index.html 처리
                            val indexPath = if (relativePath.isEmpty()) "index.html" else "$relativePath/index.html"
                            val indexFile = findFile(roots, indexPath)
                            if (indexFile != null) {
                                call.respondFile(indexFile)
                            } else {
                                call.respond(HttpStatusCode.NotFound, "File not found: $relativePath")
                            }
                        }
                    }
                }
            }.start(wait = false)

            assignedPort = targetPort
            println("GWT Test Server is running at http://127.0.0.1:$assignedPort")
        } catch (e: Exception) {
            println("Failed to start GWT Test Server: ${e.message}")
            throw e
        }
    }

    private fun findFile(roots: List<File>, relativePath: String): File? {
        for (root in roots) {
            val file = File(root, relativePath)
            if (file.exists() && file.isFile) return file
        }
        return null
    }

    fun getPort(): Int = assignedPort

    override fun close() {
        if (::server.isInitialized) {
            println("Stopping GWT Test Server...")
            server.stop(500, 1000)
        }
    }
}
