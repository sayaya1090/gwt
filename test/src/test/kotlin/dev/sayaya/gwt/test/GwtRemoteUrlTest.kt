package dev.sayaya.gwt.test

import io.kotest.matchers.shouldBe
import java.io.File

/**
 * gwt.junit.remoteUrl 시스템 프로퍼티가 있을 때 http:// 경로로 로드되는지 확인하는 테스트
 */
@GwtHtml("build/test-resources/GwtRemoteUrlTest/test.html")
class GwtRemoteUrlTest : GwtTestSpec({
    val testHtml = File("build/test-resources/GwtRemoteUrlTest/test.html")
    
    beforeSpec {
        testHtml.parentFile.mkdirs()
        testHtml.writeText("<html><body><h1>Remote URL Test</h1></body></html>")
    }

    afterSpec {
        File("build/test-resources/GwtRemoteUrlTest").deleteRecursively()
        System.clearProperty("gwt.junit.remoteUrl")
    }

    Given("gwt.junit.remoteUrl 프로퍼티가 설정되었을 때") {
        System.setProperty("gwt.junit.remoteUrl", "http://127.0.0.1:18080")

        When("generateUrl()을 호출하면") {
            val url = generateUrl()

            Then("URL은 http://로 시작하고 파일명을 포함해야 한다") {
                url shouldBe "http://127.0.0.1:18080/test.html"
            }
        }
    }

    Given("gwt.junit.remoteUrl 프로퍼티가 슬래시로 끝날 때") {
        System.setProperty("gwt.junit.remoteUrl", "http://127.0.0.1:18080/")

        When("generateUrl()을 호출하면") {
            val url = generateUrl()

            Then("중복 슬래시 없이 URL이 생성되어야 한다") {
                url shouldBe "http://127.0.0.1:18080/test.html"
            }
        }
    }

    Given("gwt.junit.remoteUrl 프로퍼티가 없을 때") {
        System.clearProperty("gwt.junit.remoteUrl")

        When("generateUrl()을 호출하면") {
            val url = generateUrl()

            Then("URL은 file://로 시작해야 한다") {
                url shouldBe "file://${testHtml.absolutePath}"
            }
        }
    }
})
