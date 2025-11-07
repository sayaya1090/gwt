package dev.sayaya.gwt.test

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.Strictness
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.BehaviorSpec
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
 * @GwtHtml("src/test/webapp/test.html")
 * class MyTest : GwtTestSpec({
 *     Given("모듈이 로드되면") {
 *         When("버튼을 클릭하면") {
 *             Then("로그가 출력되어야 한다") {
 *                 document shouldContainLog "Expected message"
 *             }
 *         }
 *     }
 * })
 * ```
 */
open class GwtTestSpec(
    body: GwtTestSpec.() -> Unit
) : BehaviorSpec() {

    /**
     * 테스트할 HTML 파일 경로 (어노테이션에서 자동으로 로드됨)
     */
    private val htmlPath: String by lazy { (
            this::class.annotations
                .filterIsInstance<GwtHtml>()
                .firstOrNull() ?: throw IllegalStateException("@GwtHtml 어노테이션이 필요합니다")
            ).path
    }

    /**
     * ChromeDriver 인스턴스
     */
    lateinit var document: ChromeDriver

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
    private val gson = GsonBuilder().setStrictness(Strictness.LENIENT).create()
    private fun parseMessage(logEntry: LogEntry): Any {
        val rawMessage = logEntry.message
        val parts = logRegex.split(rawMessage, limit = 2)
        val logContentStr = parts.last()
        return try {
            gson.fromJson(logContentStr, Any::class.java) ?: logContentStr
        } catch (_: JsonSyntaxException) {
            logContentStr
        }
    }

    /**
     * 브라우저 콘솔 로그 검증 공통 메서드
     *
     * @param value 검증할 로그 값
     * @param shouldContain true면 포함되어야 하고, false면 포함되지 않아야 함
     * @throws AssertionError 검증 실패 시
     */
    private fun ChromeDriver.checkLog(value: Any, shouldContain: Boolean) {
        val logs = this.manage().logs().get(LogType.BROWSER)
        val parsedLogs = logs.map(::parseMessage)
        val found = parsedLogs.any { it == value }

        val expectedCondition = if (shouldContain) "contain" else "NOT to contain"
        val actualCondition = if (found) "found" else "not found"

        withClue({
            "Expected log to $expectedCondition:\n" +
                    "  '$value'\n" +
                    "But it was $actualCondition. Actual logs were:\n" +
                    parsedLogs.joinToString("\n") { "  - $it" }
        }) {
            found shouldBe shouldContain
        }
    }

    /**
     * 브라우저 콘솔 로그에 특정 값이 포함되어 있는지 검증합니다.
     * 검증 후 콘솔 로그를 자동으로 클리어합니다.
     *
     * @param expected 로그에 포함되어야 하는 값
     * @throws AssertionError 로그에 해당 값이 없으면 예외 발생
     */
    infix fun ChromeDriver.shouldContainLog(expected: Any) {
        checkLog(expected, shouldContain = true)
    }

    /**
     * 브라우저 콘솔 로그에 특정 값이 포함되어 있지 않은지 검증합니다.
     * 검증 후 콘솔 로그를 자동으로 클리어합니다.
     *
     * @param unexpected 로그에 포함되지 않아야 하는 값
     * @throws AssertionError 로그에 해당 값이 있으면 예외 발생
     */
    infix fun ChromeDriver.shouldNotContainLog(unexpected: Any) {
        checkLog(unexpected, shouldContain = false)
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
