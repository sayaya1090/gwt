package net.sayaya.gwt

import io.kotest.assertions.nondeterministic.until
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.scopes.BehaviorSpecGivenContainerScope
import kotlinx.coroutines.delay
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import kotlin.time.Duration.Companion.seconds

abstract class GwtSpec (body: BehaviorSpec.() -> Unit = {}) : BehaviorSpec(body) {
    companion object {
        private suspend fun waitUntil(until: kotlin.time.Duration, delay: kotlin.time.Duration = 0.5.seconds, predicate: () -> Boolean): Unit = until(until) {
            val result = try { predicate() } catch (exception: Exception) { false }
            if(result.not()) delay(delay.inWholeMilliseconds)
            result
        }

        fun BehaviorSpec.Connect(name: String = "Connection", url: String, test: suspend BehaviorSpecGivenContainerScope.(ChromeDriver) -> Unit) {
            Given(name) {
                val document = ChromeDriver(ChromeOptions().addArguments("--headless"))
                document.get("http://127.0.0.1:8080/$url")
                waitUntil(10.seconds) {                                                         // Wait until the GWT webserver is ready
                    var body: WebElement? = null
                    try { body = document.findElement(By.tagName("body")) } catch(ignore: NoSuchElementException) { }
                    body!=null && body.text.startsWith("Compiling").not() && body.text.isNotBlank()
                }
                test(document)
                document.quit()
            }
        }
    }
}