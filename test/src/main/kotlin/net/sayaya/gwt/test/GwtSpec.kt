package net.sayaya.gwt.test

import io.kotest.assertions.nondeterministic.until
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecGivenContainerScope
import kotlinx.coroutines.delay
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.io.File
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.seconds

abstract class GwtSpec (body: BehaviorSpec.() -> Unit = {}) : BehaviorSpec(body) {
    companion object {
        private suspend fun waitUntil(until: kotlin.time.Duration, delay: kotlin.time.Duration = 0.5.seconds, predicate: () -> Boolean): Unit = until(until) {
            val result = try { predicate() } catch (exception: Exception) { false }
            if(result.not()) delay(delay.inWholeMilliseconds)
            result
        }

        fun BehaviorSpec.Given(html: String, module: String, port: Int = 8080, timeout: Int=30,
                                 js: List<String> = emptyList(),
                                 css: List<String> = emptyList(),
                                 launcherDir: String = "src/test/resources/static",
                                 createHtml: Boolean = true,
                                 clear: Boolean = false,
                                 name: String = "Connection", test: suspend BehaviorSpecGivenContainerScope.(ChromeDriver) -> Unit) {
            Given(name) {
                val file: File
                var clearTask: ()->Unit = { }
                if(createHtml) {
                    val fileName = if(html.contains("#")) html.substring(0, html.indexOf("#")) else html
                    file = File("$launcherDir/$fileName")
                    file.writeText(
                        """
                        <!DOCTYPE html>
                        <html lang="ko">
                            <head>
                                <script type="text/javascript" src="$module/$module.nocache.js"></script>
                                ${js.joinToString(separator = "\n        ") { "<script type=\"text/javascript\" src=\"$it\"></script>" }}
                                ${css.joinToString(separator = "\n        ") { "<link rel=\"stylesheet\" href=\"$it\" />" }}
                            </head>
                            <body></body>
                        </html>
                        """.trimIndent()
                    )
                    if(clear) clearTask = { file.delete() }
                }
                val document = ChromeDriver(ChromeOptions().addArguments("--headless"))
                document.get("http://127.0.0.1:$port/$html")
                waitUntil(timeout.seconds) {               // Wait until the GWT webserver is ready
                    sleep(1000)
                    var body: WebElement? = null
                    try { body = document.findElement(By.tagName("body")) } catch(ignore: NoSuchElementException) { }
                    body!=null && body.text.startsWith("Compiling").not()
                }
                test(document)
                document.quit()
                clearTask.invoke()
            }
        }
    }
}