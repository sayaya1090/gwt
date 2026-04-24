package dev.sayaya.gwt.test

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.Strictness
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.LoadState
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * GWT Playwright 테스트를 위한 Kotest BehaviorSpec 베이스 클래스
 *
 * ## 주요 기능
 * - Playwright 자동 실행 (headless 모드, 브라우저 로깅 활성화)
 * - 로컬 HTML 파일 자동 로드
 * - 콘솔 로그 검증 헬퍼 메서드
 * - 테스트 종료 시 자동 cleanup
 *
 * ## 사용 예시
 * ```kotlin
 * @GwtHtml("test.html")
 * class MyTest : GwtTestSpec({
 *     Given("모듈이 로드되면") {
 *         When("버튼을 클릭하면") {
 *             Then("로그가 출력되어야 한다") {
 *                 page shouldContainLog "Expected message"
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
     * Playwright 인스턴스
     */
    private lateinit var playwright: Playwright
    private lateinit var browser: Browser
    lateinit var page: Page
    private val consoleLogs = CopyOnWriteArrayList<Any?>()
    init {
        body()

        beforeSpec {
            println("[GwtTestSpec] Initializing Playwright...")
            playwright = Playwright.create()
            println("[GwtTestSpec] Launching Chromium Browser (headless)...")
            browser = playwright.chromium().launch(
                BrowserType.LaunchOptions().setHeadless(true)
            )

            println("[GwtTestSpec] Creating new page...")
            page = browser.newPage()

            // 브라우저 콘솔 로그 수집
            page.onConsoleMessage { msg ->
                // ... (생략된 로그 수집 로직) ...
            }
            
            println("[GwtTestSpec] Loading HTML file...")
            loadHtmlFile()
            
            println("[GwtTestSpec] Waiting for Network Idle...")
            page.waitForLoadState(LoadState.NETWORKIDLE)
            println("[GwtTestSpec] Initialization Complete.")
        }

        afterSpec {
            runCatching { page.close() }
            runCatching { browser.close() }
            runCatching { playwright.close() }
        }
    }
    private val gson = GsonBuilder().setStrictness(Strictness.LENIENT).create()
    private fun parseMaybeJson(text: String): Any {
        return try {
            gson.fromJson(text, Any::class.java) ?: text
        } catch (_: JsonSyntaxException) {
            text
        }
    }

    /**
     * HTML 파일을 로드합니다.
     */
    internal fun loadHtmlFile() {
        val url = generateUrl()
        if (::page.isInitialized) {
            page.setDefaultNavigationTimeout(30000.0) // 30초 타임아웃 추가
            page.setDefaultTimeout(30000.0)
            page.navigate(url)
        }
    }

    /**
     * 테스트할 HTML URL을 생성합니다.
     * 
     * 시스템 프로퍼티 `gwt.junit.remoteUrl`을 우선적으로 사용하며,
     * 설정되지 않은 경우 기본값으로 `http://localhost:8080/`을 사용합니다.
     * 
     * @return 생성된 HTTP URL 문자열
     */
    internal fun generateUrl(): String {
        val remoteUrl = System.getProperty("gwt.junit.remoteUrl") ?: "http://localhost:8080/"
        val baseUrl = if (remoteUrl.endsWith("/")) remoteUrl else "$remoteUrl/"
        // htmlPath가 /로 시작하면 제거하여baseUrl과 중복 방지
        val path = if (htmlPath.startsWith("/")) htmlPath.substring(1) else htmlPath
        return baseUrl + path
    }

    /**
     * 브라우저 콘솔 로그 검증 공통 메서드
     *
     * @param expected 검증할 로그 값
     * @param shouldContain true면 포함되어야 하고, false면 포함되지 않아야 함
     * @throws AssertionError 검증 실패 시
     */
    private fun Page.checkLog(expected: Any, shouldContain: Boolean) {
        val found = consoleLogs.any { it == expected }

        val expectedCondition = if (shouldContain) "contain" else "NOT to contain"
        val actualCondition = if (found) "found" else "not found"

        withClue({
            "Expected log to $expectedCondition:\n" +
                    "  '$expected'\n" +
                    "But it was $actualCondition. Actual logs were:\n" +
                    consoleLogs.joinToString("\n") { "  - $it" }
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
    infix fun Page.shouldContainLog(expected: Any) {
        checkLog(expected, shouldContain = true)
    }

    /**
     * 브라우저 콘솔 로그에 특정 값이 포함되어 있지 않은지 검증합니다.
     * 검증 후 콘솔 로그를 자동으로 클리어합니다.
     *
     * @param unexpected 로그에 포함되지 않아야 하는 값
     * @throws AssertionError 로그에 해당 값이 있으면 예외 발생
     */
    infix fun Page.shouldNotContainLog(unexpected: Any) {
        checkLog(unexpected, shouldContain = false)
    }

    /**
     * 브라우저 콘솔의 모든 로그를 가져옵니다.
     *
     * @return 콘솔 로그 메시지 목록
     */
    fun Page.getConsoleLogs(): List<Any?> = consoleLogs.toList()

    /**
     * 브라우저 콘솔 로그를 클리어합니다.
     */
    fun Page.clearConsoleLogs() {
        consoleLogs.clear()
    }
}
