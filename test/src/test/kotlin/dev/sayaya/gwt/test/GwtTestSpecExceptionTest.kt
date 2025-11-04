package dev.sayaya.gwt.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.io.File

/**
 * GwtTestSpec의 예외 발생 시나리오를 테스트합니다.
 */
class GwtTestSpecExceptionTest : BehaviorSpec({

    Given("존재하지 않는 HTML 파일 경로") {
        val nonExistentPath = "build/test-resources/NonExistent/missing.html"

        When("loadHtmlFile()을 호출하면") {
            Then("IllegalArgumentException이 발생해야 한다") {
                // ChromeDriver 임시 생성
                val options = ChromeOptions().apply {
                    addArguments("--headless")
                    addArguments("--disable-gpu")
                    addArguments("--no-sandbox")
                    addArguments("--disable-dev-shm-usage")
                }
                val driver = ChromeDriver(options)

                try {
                    // 테스트 대상 스펙 생성
                    val testSpec = object : GwtTestSpec({
                        htmlPath = nonExistentPath
                        headless = true

                        Given("dummy") {
                            Then("dummy") {}
                        }
                    }) {}

                    // document 필드에 driver 할당
                    testSpec.document = driver

                    // loadHtmlFile 직접 호출하여 예외 검증
                    val exception = shouldThrow<IllegalArgumentException> {
                        testSpec.loadHtmlFile()
                    }

                    exception.message shouldContain "HTML 파일을 찾을 수 없습니다"
                    exception.message shouldContain "missing.html"
                } finally {
                    driver.quit()
                }
            }
        }
    }

    Given("빈 디렉토리에 HTML 파일이 없는 경우") {
        val tempDir = File("build/test-resources/EmptyDir")
        tempDir.mkdirs()
        val nonExistentPath = "${tempDir.path}/test.html"

        When("loadHtmlFile()을 호출하면") {
            Then("절대 경로를 포함한 IllegalArgumentException이 발생해야 한다") {
                val options = ChromeOptions().apply {
                    addArguments("--headless")
                    addArguments("--disable-gpu")
                    addArguments("--no-sandbox")
                    addArguments("--disable-dev-shm-usage")
                }
                val driver = ChromeDriver(options)

                try {
                    val testSpec = object : GwtTestSpec({
                        htmlPath = nonExistentPath
                        headless = true

                        Given("dummy") {
                            Then("dummy") {}
                        }
                    }) {}

                    testSpec.document = driver

                    val exception = shouldThrow<IllegalArgumentException> {
                        testSpec.loadHtmlFile()
                    }

                    exception.message shouldContain "HTML 파일을 찾을 수 없습니다"
                    exception.message shouldContain File(nonExistentPath).absolutePath
                } finally {
                    driver.quit()
                }
            }
        }

        afterSpec {
            tempDir.deleteRecursively()
        }
    }
})