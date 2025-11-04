package dev.sayaya.gwt.test

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.logging.LogEntry
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.logging.LoggingPreferences
import java.io.File
import java.util.logging.Level

/**
 * GWT Selenium 테스트를 위한 Kotest BehaviorSpec 베이스 클래스
 *
 * ## 주요 기능
 * - ChromeDriver 자동 설정 (headless 모드, 브라우저 로깅 활성화)
 * - 로컬 HTML 파일 자동 로드
 * - 콘솔 로그 검증 헬퍼 메서드
 * - 테스트 종료 시 자동 cleanup
 *
 * ## 사용 예시
 * ```kotlin
 * class MyTest : GwtSeleniumSpec({
 *     htmlPath = "src/test/webapp/test.html"
 *
 *     Given("모듈이 로드되면") {
 *         When("버튼을 클릭하면") {
 *             Then("로그가 출력되어야 한다") {
 *                 driver shouldContainLog "Expected message"
 *             }
 *         }
 *     }
 * })
 * ```
 *
 * @property htmlPath 테스트할 HTML 파일 경로 (기본값: "src/test/webapp/test.html")
 * @property webServerPort 웹서버 포트 (기본값: 9876, GWT 플러그인 기본 포트)
 * @property headless headless 모드 사용 여부 (기본값: true)
 */
abstract class GwtTestSpec(
    body: GwtTestSpec.() -> Unit
) : BehaviorSpec() {

    /**
     * 테스트할 HTML 파일 경로
     */
    var htmlPath: String = "src/test/webapp/test.html"

    /**
     * 웹서버 포트 (GWT 플러그인의 openWebServer 태스크가 사용하는 포트)
     */
    var webServerPort: Int = 9876

    /**
     * Headless 모드 사용 여부
     */
    var headless: Boolean = true

    /**
     * ChromeDriver 인스턴스
     * 테스트 내부에서 직접 접근할 수 있도록 internal로 공개됩니다.
     */
    internal lateinit var document: ChromeDriver

    init {
        body()

        beforeSpec {
            document = createChromeDriver()
            loadHtmlFile()
        }

        afterSpec {
            document.quit()
        }
    }

    /**
     * ChromeDriver를 생성하고 로깅을 설정합니다.
     */
    private fun createChromeDriver(): ChromeDriver {
        val options = ChromeOptions().apply {
            addArguments("--headless")
            addArguments("--disable-gpu")
            addArguments("--no-sandbox")
            addArguments("--disable-dev-shm-usage")

            val logPrefs = LoggingPreferences()
            logPrefs.enable(LogType.BROWSER, Level.ALL)
            setCapability("goog:loggingPrefs", logPrefs)
        }
        return ChromeDriver(options)
    }

    /**
     * HTML 파일을 로드합니다.
     * 파일 경로가 상대 경로인 경우 절대 경로로 변환하여 로드합니다.
     */
    internal fun loadHtmlFile() {
        val html = File(htmlPath)
        if (!html.exists()) {
            throw IllegalArgumentException("HTML 파일을 찾을 수 없습니다: ${html.absolutePath}")
        }
        document.get("file://${html.absolutePath}")
    }

    /**
     * 브라우저 로그에서 순수한 메시지만 파싱합니다.
     */
    private val logRegex = Regex("""\s+\d+:\d+\s+""")
    private val gson = Gson()
    private fun parseMessage(logEntry: LogEntry): Any {
        val rawMessage = logEntry.message
        val parts = logRegex.split(rawMessage, limit = 2)
        val logContentStr = parts.last()
        val parsedData = gson.fromJson(logContentStr, Any::class.java)
        if (parsedData != null) return parsedData
        return logContentStr
    }

    /**
     * 브라우저 콘솔 로그에 특정 값이 포함되어 있는지 검증합니다.
     * 검증 후 콘솔 로그를 자동으로 클리어합니다.
     *
     * @param expected 로그에 포함되어야 하는 값
     * @throws AssertionError 로그에 해당 값이 없으면 예외 발생
     */
    infix fun ChromeDriver.shouldContainLog(expected: Any) {
        val logs = this.manage().logs().get(LogType.BROWSER)
        val found = logs.asSequence().map(::parseMessage).any { it == expected }
        withClue({
            "Expected log to contain:\n" +
                    "  '$expected'\n" +
                    "But it was not found. Actual logs were:\n" +
                    logs.map(::parseMessage).joinToString("\n") { "  - $it" }
        }) {
            found shouldBe true
        }
    }

    /**
     * 브라우저 콘솔 로그에 특정 값이 포함되어 있지 않은지 검증합니다.
     * 검증 후 콘솔 로그를 자동으로 클리어합니다.
     *
     * @param unexpected 로그에 포함되지 않아야 하는 값
     * @throws AssertionError 로그에 해당 값이 있으면 예외 발생
     */
    infix fun ChromeDriver.shouldNotContainLog(unexpected: Any) {
        val logs = this.manage().logs().get(LogType.BROWSER)
        val found = logs.asSequence().map(::parseMessage).any { it == unexpected }
        withClue({
            "Expected log NOT to contain:\n" +
                    "  '$unexpected'\n" +
                    "But it was found. Actual logs were:\n" +
                    logs.map(::parseMessage).joinToString("\n") { "  - $it" }
        }) {
            found shouldBe false
        }
    }

    /**
     * 브라우저 콘솔의 모든 로그를 가져옵니다.
     *
     * @return 콘솔 로그 메시지 목록
     */
    fun ChromeDriver.getConsoleLogs(): List<Any> = this.manage().logs().get(LogType.BROWSER).map(::parseMessage)

    /**
     * 브라우저 콘솔 로그를 클리어합니다.
     */
    fun ChromeDriver.clearConsoleLogs() {
        this.manage().logs().get(LogType.BROWSER)
    }
}
