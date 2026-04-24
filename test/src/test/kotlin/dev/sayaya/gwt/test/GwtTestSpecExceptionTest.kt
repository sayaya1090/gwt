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
})