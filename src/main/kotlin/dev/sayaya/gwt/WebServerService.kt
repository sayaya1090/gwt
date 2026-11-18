package dev.sayaya.gwt

import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.gradle.api.services.BuildService
import org.gradle.api.logging.Logging
import java.io.File

/**
 * GWT 테스트를 위한 Ktor 기반 내장 웹 서버 서비스입니다.
 * 
 * 다중 컨텐츠 루트를 지원하며, 요청 시 등록된 경로를 순차적으로 탐색합니다.
 */
abstract class WebServerService : BuildService<WebServerParameters>, AutoCloseable {

    // Gradle 로그 레벨(-q, --warn 등)로 제어되는 로거
    private val logger = Logging.getLogger(WebServerService::class.java)

    // Ktor 서버 인스턴스 홀더
    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    // 실제로 바인딩된 포트 번호
    private var assignedPort: Int = -1

    init {
        startServer()
    }

    private fun startServer() {
        val roots = parameters.contentRoot.files.filter { it.exists() && it.isDirectory }
        // 0(또는 미지정)이면 Netty가 직접 빈 포트를 고른다. 미리 빈 포트를 찾아 두었다가 다시 바인딩하면
        // 그 사이 다른 프로세스가 포트를 가져갈 수 있다(TOCTOU).
        val requestedPort = parameters.port.getOrElse(0)

        // 서버 기동 시 서빙 경로를 LIFECYCLE 수준으로 출력
        logger.lifecycle("Starting GWT Test Server...")
        roots.forEach { 
            logger.lifecycle(" > Serving Directory: {}", it.absolutePath)
        }

        try {
            server = embeddedServer(Netty, port = requestedPort, host = "127.0.0.1") {
                routing {
                    // 수동 경로 탐색 (DuplicatePluginException 방지)
                    get("/{staticPath...}") {
                        val pathSegments = call.parameters.getAll("staticPath") ?: emptyList()
                        val relativePath = pathSegments.joinToString("/")
                        // 요청 경로를 모든 루트에서 먼저 찾고, 없으면 디렉토리의 index.html을 찾는다.
                        val indexPath = if (relativePath.isEmpty()) "index.html" else "$relativePath/index.html"
                        val file = listOf(relativePath, indexPath).firstNotNullOfOrNull { findFile(roots, it) }
                        if (file != null) {
                            call.respondFile(file)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "File not found: $relativePath")
                        }
                    }
                }
            }.start(wait = false)

            assignedPort = runBlocking { server.engine.resolvedConnectors() }.first().port
            logger.lifecycle("GWT Test Server is running at http://127.0.0.1:{}", assignedPort)
        } catch (e: Exception) {
            logger.error("Failed to start GWT Test Server: {}", e.message)
            throw e
        }
    }

    /**
     * 등록된 루트를 순서대로 탐색해 [relativePath]에 해당하는 파일을 찾습니다.
     *
     * 정규화한 경로가 루트 밖으로 벗어나면(`..` 등) 해당 루트에서는 찾지 않습니다.
     * 심볼릭 링크는 해석하지 않으므로 루트 안의 링크는 그대로 제공됩니다.
     */
    private fun findFile(roots: List<File>, relativePath: String): File? {
        for (root in roots) {
            val base = root.absoluteFile.toPath().normalize()
            val target = base.resolve(relativePath).normalize()
            if (!target.startsWith(base)) continue
            val file = target.toFile()
            if (file.isFile) return file
        }
        return null
    }

    fun getPort(): Int = assignedPort

    override fun close() {
        if (::server.isInitialized) {
            logger.lifecycle("Stopping GWT Test Server...")
            server.stop(500, 1000)
        }
    }
}
