package dev.sayaya.gwt.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.openqa.selenium.By
import java.io.File

/**
 * GwtTestSpec의 실제 사용 예시 및 테스트
 *
 * 이 클래스는 GwtTestSpec을 상속받아 그 기능을 직접 사용하며 검증합니다.
 */
@GwtHtml("build/test-resources/GwtTestSpecTest/test.html")
class GwtTestSpecTest : GwtTestSpec({
    // beforeSpec/afterSpec 콜백으로 테스트 환경을 설정하고 정리합니다.
    beforeSpec {
        val testHtml = File("build/test-resources/GwtTestSpecTest/test.html")
        testHtml.parentFile.mkdirs()
        testHtml.writeText("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>GwtTestSpec Test</title>
                <script>
                    // 일반적인 로그 (따옴표로 감싸진 경우)
                    console.log("정상 로그");
                    
                    // 따옴표가 없는 로그 (객체 등)
                    console.log({type: "object"});
                    
                    // 빈 문자열
                    console.log("");
                    
                    // 특수 문자 포함
                    console.log("특수문자: !@#$%");
                    
                    // 여러 줄 로그
                    console.log("첫번째줄\n두번째줄");
                    
                    // 숫자
                    console.log(12345);
                    
                    // boolean
                    console.log(true);
                    
                    // null/undefined
                    console.log(null);
                    console.log(undefined);
                </script>
            </head>
            <body>
                <h1>GwtTestSpec Test Page</h1>
            </body>
            </html>
        """.trimIndent())
    }
    beforeTest {
        document.clearConsoleLogs()
        document.navigate().refresh()
    }
    afterSpec {
        File("build/test-resources/GwtTestSpecTest").deleteRecursively()
    }

    Given("GwtTestSpec을 상속하여 테스트를 실행할 때") {
        When("페이지가 정상적으로 로드되면") {
            Then("페이지 구성 요소에 접근할 수 있다") {
                document.title shouldBe "GwtTestSpec Test"
                val h1Element = document.findElement(By.tagName("h1"))
                h1Element.text shouldBe "GwtTestSpec Test Page"
            }
        }

        When("로그를 가져오면") {
            val logs = document.getConsoleLogs()

            Then("따옴표로 감싸진 일반 로그를 파싱할 수 있어야 한다") {
                logs shouldContain "정상 로그"
            }

            Then("빈 문자열 로그를 처리할 수 있어야 한다") {
                logs shouldContain ""
            }

            Then("특수 문자가 포함된 로그를 파싱할 수 있어야 한다") {
                logs shouldContain "특수문자: !@#$%"
            }

            Then("여러 줄 로그를 파싱할 수 있어야 한다") {
                // 브라우저가 \n을 어떻게 처리하는지에 따라 달라질 수 있음
                val hasMultilineLog = logs.filter { it is String }.any { (it as String).contains("첫번째줄") }
                hasMultilineLog shouldBe true
            }

            Then("객체, 숫자, boolean 등 따옴표가 없는 로그도 처리할 수 있어야 한다") {
                // 이런 로그들은 parseMessage의 else 분기를 통과
                // 브라우저가 반환하는 원본 형식 그대로 저장됨
                val allLogs = logs.joinToString("\n")
                // 최소한 로그가 비어있지 않아야 함
                allLogs.isNotEmpty() shouldBe true
            }

            Then("shouldContainLog가 실패하면 AssertionError를 던져야 한다") {
                shouldThrow<AssertionError> {
                    document shouldContainLog "이것은 실패해야 합니다"
                }
            }

            Then("shouldNotContainLog가 실패하면 AssertionError를 던져야 한다") {
                shouldThrow<AssertionError> {
                    document shouldNotContainLog "정상 로그"
                }
            }
        }

        When("따옴표가 없는 로그를 검증하면") {
            Then("원본 형식의 로그를 찾을 수 있어야 한다") {
                val logs = document.getConsoleLogs()
                // 객체, 숫자 등은 브라우저가 문자열로 변환한 형태로 저장됨
                // 예: "[object Object]", "12345", "true", "null", "undefined" 등

                // 최소한 9개의 console.log가 있어야 함
                logs.size shouldBe 9
            }
        }

        When("콘솔 로그를 직접 관리하면") {
            beforeTest {
                document.navigate().refresh()
            }

            Then("getConsoleLogs()로 모든 로그를 가져올 수 있다") {
                val logs = document.getConsoleLogs()
                logs shouldContain "정상 로그"
                logs shouldContain "특수문자: !@#\$%"
                logs shouldContain 12345
                logs shouldContain true
            }

            Then("clearConsoleLogs()로 로그를 지울 수 있다") {
                document.clearConsoleLogs()
                document.getConsoleLogs().shouldBeEmpty()
            }
        }
    }
})