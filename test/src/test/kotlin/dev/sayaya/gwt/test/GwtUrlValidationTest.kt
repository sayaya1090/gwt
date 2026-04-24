package dev.sayaya.gwt.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.io.File

@GwtHtml("build/test-resources/GwtUrlValidationTest/test.html")
private class DummySpec : GwtTestSpec({})

class GwtUrlValidationTest : BehaviorSpec({
    val mockHtml = File("build/test-resources/GwtUrlValidationTest/test.html")
    
    beforeSpec {
        mockHtml.parentFile.mkdirs()
        mockHtml.writeText("<html></html>")
    }

    afterSpec {
        File("build/test-resources/GwtUrlValidationTest").deleteRecursively()
        System.clearProperty("gwt.junit.remoteUrl")
    }

    Given("GwtTestSpec의 URL 생성 로직 검증") {
        val spec = DummySpec()

        When("Scenario A: gwt.junit.remoteUrl 프로퍼티가 설정된 경우 (HTTP + 랜덤 포트)") {
            System.setProperty("gwt.junit.remoteUrl", "http://127.0.0.1:49152")
            Then("기대 결과: http://127.0.0.1:49152/test.html") {
                spec.generateUrl() shouldBe "http://127.0.0.1:49152/test.html"
            }
        }

        When("Scenario B: 프로퍼티가 누락된 경우") {
            System.clearProperty("gwt.junit.remoteUrl")
            Then("기본값(http://localhost:8080/...) 반환") {
                spec.generateUrl() shouldBe "http://localhost:8080/test.html"
            }
        }

        When("Scenario C-1: URL 끝에 슬래시가 누락된 경우") {
            System.setProperty("gwt.junit.remoteUrl", "http://localhost:8080")
            Then("슬래시가 포함된 경로 반환") {
                spec.generateUrl() shouldBe "http://localhost:8080/test.html"
            }
        }

        When("Scenario C-2: URL 끝에 슬래시가 중복된 경우") {
            System.setProperty("gwt.junit.remoteUrl", "http://localhost:8080/")
            Then("중복 슬래시 없이 경로 반환") {
                spec.generateUrl() shouldBe "http://localhost:8080/test.html"
            }
        }
    }
})
