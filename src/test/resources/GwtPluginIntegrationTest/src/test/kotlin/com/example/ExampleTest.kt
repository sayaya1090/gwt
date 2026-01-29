package com.example

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import io.kotest.core.spec.style.BehaviorSpec
import java.io.File

internal class ExampleTest: BehaviorSpec({
    val playwright = Playwright.create()
    val browser = playwright.chromium().launch(
        BrowserType.LaunchOptions().setHeadless(true)
    )
    val document = browser.newPage()

    val html = File("src/test/webapp/test.html")
    Given("a GWT web application") {
        When("the test page is loaded") {
            Then("it should print the initialization log to the console") {
                document.waitForConsoleMessage(Page.WaitForConsoleMessageOptions().setPredicate {
                    it.text() == "Hello from Main"
                }) {
                    document.navigate("file://${html.absolutePath}")
                }
            }
        }
    }
    afterSpec {
        runCatching { document.close() }
        runCatching { browser.close() }
        runCatching { playwright.close() }
    }
})