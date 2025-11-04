package dev.sayaya.gwt.test

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

    Given("존재하지 않는 HTML 파일 경로가 지정된 경우") {
        @Ignored @GwtHtml("build/test-resources/NonExistent/missing.html")
        class TestSpecWithInvalidPath(body: GwtTestSpec.() -> Unit) : GwtTestSpec(body)

        When("loadHtmlFile()을 호출하면") {
            Then("IllegalArgumentException이 발생해야 한다") {
                // 어노테이션 없는 네임드 클래스 사용
                val testSpec = TestSpecWithInvalidPath({
                    Given("dummy") {
                        Then("dummy") {}
                    }
                })

                // loadHtmlFile 직접 호출하여 예외 검증
                val exception = shouldThrow<IllegalArgumentException> {
                    testSpec.loadHtmlFile()
                }

                exception.message shouldContain "HTML 파일을 찾을 수 없습니다"
                exception.message shouldContain "missing.html"
            }
        }
    }
})