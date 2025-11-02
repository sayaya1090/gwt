package com.example

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.logging.LoggingPreferences
import java.io.File
import java.util.logging.Level

internal class ExampleTest: BehaviorSpec({
    val document = ChromeDriver(ChromeOptions().addArguments("--headless").apply {
        val logPrefs = LoggingPreferences()
        logPrefs.enable(LogType.BROWSER, Level.ALL)
        setCapability("goog:loggingPrefs", logPrefs)
    })
    val html = File("src/test/webapp/test.html")
    document.get("file://${html.absolutePath}")
    Given("a") {
        When("b") {
            Then("c") {

            }
        }
    }
    afterSpec {
        document.quit()
    }
}) {
    companion object {
        infix fun ChromeDriver.shouldContainLog(expectedText: String) {
            val logEntries = this.manage().logs().get(LogType.BROWSER)
            val foundLog = logEntries.any { log ->
                log.message.contains(expectedText)
            }
            foundLog shouldBe true
            executeScript("console.clear();")
        }
    }
}