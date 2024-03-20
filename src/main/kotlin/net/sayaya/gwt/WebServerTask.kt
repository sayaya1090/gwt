package net.sayaya

import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File
import kotlin.concurrent.thread

abstract class WebServerTask: DefaultTask() {
    @get:Input
    abstract val webserverPort: Property<Int>
    @get:Input
    abstract val webserverPath: Property<File>
    private lateinit var webServer: ApplicationEngine
    @TaskAction
    fun exec() {
        thread { try {
            webServer = embeddedServer(Netty, webserverPort.get()) {
                routing {
                    staticFiles("/", webserverPath.get())
                }
            }
            webServer.start(wait = true)
        } catch(ignore: Exception) { } }
    }
    fun close() {
        webServer.stop(1000, 1000)
    }
}