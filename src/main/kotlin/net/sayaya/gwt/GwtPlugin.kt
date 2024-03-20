package net.sayaya

import kotlinx.coroutines.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.UncheckedException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import kotlin.concurrent.thread

class GwtPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("org.wisepersist.gwt")
        val openWebserver = project.tasks.register("openWebServer", WebServerTask::class.java) {
            val gwtTest = project.tasks.withType(GwtTest::class.java).named("gwtTest")
            webserverPort.set(gwtTest.get().webserverPort)
            webserverPath.set(gwtTest.get().launcherDir)
        }
        project.tasks.register("closeWebServer") {
            doLast { openWebserver.get().close() }
        }
        val gwtTest = project.tasks.register("gwtTest", GwtTest::class.java) {
            dependsOn(openWebserver)
            finalizedBy("closeWebServer")
        }
        val gwtCodeServerThread = thread(start = false) { try {
            gwtTest.get().exec()
        } catch(ignore: UncheckedException) { } }
        project.tasks.register("closeGwtCodeServer") {
            doLast { gwtCodeServerThread.interrupt() }
        }
        project.tasks.withType(Test::class.java).configureEach {
            useJUnitPlatform()
            dependsOn(openWebserver)
            doFirst {
                gwtCodeServerThread.start()
                checkCodeServerReady(gwtTest.get().port)
                // checkGwtApplicationReady(project, gwtTest.get().port, gwtTest.get().modules)
            }
            finalizedBy("closeWebServer", "closeGwtCodeServer")
        }
    }
    @OptIn(DelicateCoroutinesApi::class)
    private fun checkCodeServerReady(port: Int) {
        val codeserverUrl = URI("http://127.0.0.1:$port/").toURL()
        val job = GlobalScope.launch {
            repeat(30) {
                delay(500)
                try {
                    val conn = codeserverUrl.openConnection() as HttpURLConnection
                    conn.requestMethod = "HEAD"
                    conn.connect()
                    val responseCode = conn.responseCode
                    if(responseCode == 200) {
                        this.cancel("Codeserver prepared")
                        return@launch
                    }
                } catch(_: IOException) { }
            }
        }
        runBlocking {
            job.join()
            // if(job.isCancelled) println("GWT Codeserver is ready")
            // else throw IllegalStateException("GWT Codeserver is not ready")
        }
    }
    /*@OptIn(DelicateCoroutinesApi::class)
    private fun checkGwtApplicationReady(project: Project, port: Int, modules: List<String>) {
        val jobs = modules.map { module ->
            val xml = project.file(module + ".gwt.xml")
            val codeserverUrl = URI("http://127.0.0.1:$port/").toURL()
            GlobalScope.async {
                repeat(30) {
                    delay(1000)
                    try {
                        val conn = URL(codeserverUrl, module).openConnection() as HttpURLConnection
                        conn.requestMethod = "HEAD"
                        conn.connect()
                        val responseCode = conn.responseCode
                        if(responseCode == 200) {
                            this.cancel("GWT Application $module is ready")
                            return@launch
                        }
                    } catch(e: IOException) { }
                }
            }
        }
        val job = GlobalScope.launch {
            repeat(30) {
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
                } catch(e: IOException) { }
            }
        }
        runBlocking {
            job.join()
            // if(job.isCancelled) println("GWT Codeserver is ready")
            // else throw IllegalStateException("GWT Codeserver is not ready")
        }
    }*/
}