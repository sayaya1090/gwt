package dev.sayaya.gwt.test

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain

/**
 * GwtTestSpec의 예외 발생 시나리오를 테스트합니다.
 */
class GwtTestSpecExceptionTest : BehaviorSpec({
    Given("@GwtHtml 어노테이션이 없는 경우") {
        When("loadHtmlFile()을 호출하면") {
            Then("IllegalStateException이 발생해야 한다") {
                // 어노테이션 없는 네임드 클래스 사용
                val testSpec = GwtTestSpec({
                    Given("dummy") {
                        Then("dummy") {}
                    }
                })

                // loadHtmlFile 직접 호출하여 예외 검증
                val exception = shouldThrow<IllegalStateException> {
                    testSpec.loadHtmlFile()
                }

                exception.message shouldContain "@GwtHtml 어노테이션이 필요합니다"
            }
        }
    }

    Given("@GwtHtml 페이지 이동 결과 검증") {
        val testSpec = GwtTestSpec({})
        val url = "http://127.0.0.1:49152/src/test/webapp/test.html"

        When("서버가 404를 반환하면") {
            Then("URL·상태 코드·경로 힌트를 담은 IllegalStateException이 즉시 발생해야 한다") {
                val exception = shouldThrow<IllegalStateException> { testSpec.verifyNavigation(url, 404) }
                exception.message shouldContain "HTTP 404"
                exception.message shouldContain url
                exception.message shouldContain "서빙 루트"
            }
        }
        When("서버가 500을 반환하면") {
            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalStateException> { testSpec.verifyNavigation(url, 500) }.message shouldContain "HTTP 500"
            }
        }
        When("정상 응답(200)이거나 응답이 없으면(null)") {
            Then("예외 없이 통과해야 한다") {
                shouldNotThrowAny { testSpec.verifyNavigation(url, 200) }
                shouldNotThrowAny { testSpec.verifyNavigation(url, null) }
            }
        }
    }
})