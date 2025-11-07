package dev.sayaya.gwt

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testfixtures.ProjectBuilder
import org.docstr.gwt.GwtPluginExtension
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

/**
 * GWT 플러그인 통합 테스트
 */
class GwtPluginIntegrationTest : DescribeSpec({
    /**
     * war 플러그인과 함께 사용 시 태스크 의존성 테스트
     *
     * ## 태스크 실행 순서 (./gradlew build)
     * 1. compileJava - 메인 자바 소스 컴파일
     * 2. processResources - 메인 리소스 처리
     * 3. classes - 메인 클래스 준비 완료
     * 4. gwtCompile - GWT 모듈 컴파일 (JavaScript로 변환)
     * 5. war - WAR 파일 생성 (컴파일된 GWT 출력 포함)
     * 6. assemble - 완료
     * 7. compileTestJava - 테스트 자바 소스 컴파일
     * 8. processTestResources - 테스트 리소스 처리
     * 9. testClasses - 테스트 클래스 준비 완료
     * 10. gwtTestCompile - GWT 테스트 모듈 컴파일
     * 11. openWebServer - 웹서버 시작 (테스트용)
     * 12. test - JUnit 테스트 실행 (+ GWT 코드서버 시작)
     * 13. closeWebServer - 웹서버 종료
     * 14. closeGwtCodeServer - GWT 코드서버 종료
     * 15. check - 완료
     * 16. build - 완료
     *
     * ## 핵심 의존성
     * - war → gwtCompile: WAR 파일 생성 전에 GWT 컴파일 완료 필요
     * - test → gwtTestCompile: 테스트 실행 전에 GWT 테스트 모듈 컴파일 필요
     * - test → openWebServer: 테스트 실행 전에 웹서버 시작 필요
     * - gwtCompile mustRunAfter test: 충돌 방지를 위한 순서 제약
     * - jar.enabled = false: WAR 플러그인 사용 시 JAR 불필요
     */
    describe("통합 테스트") {
        val projectDir = tempdir()
        val resourceDir = File(javaClass.classLoader.getResource("GwtPluginIntegrationTest")!!.toURI())
        resourceDir.copyRecursively(projectDir)

        val runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("build", "--stacktrace")

        val result = runner.build()
        println(result.output)
        it("gwtCompile이 성공해야 함") {
            result.task(":gwtCompile")?.outcome shouldBe TaskOutcome.SUCCESS
        }
        it("gwtCompile 결과물이 생성되어야 함") {
            result.output shouldContainOnlyOnce "Compiling module com.example.App"
            val devModeWarDir = File(projectDir, "src/main/webapp")
            devModeWarDir.shouldExist()
            val nocacheJs = File(devModeWarDir, "app/app.nocache.js")
            nocacheJs.shouldExist()
        }
        it("gwtTestCompile이 성공해야 함") {
            result.task(":gwtTestCompile")?.outcome shouldBe TaskOutcome.SUCCESS
        }
        it("gwtTestCompile 결과물이 생성되어야 함") {
            result.output shouldContainOnlyOnce "Compiling module com.example.Test"
            val devModeWarDir = File(projectDir, "src/test/webapp")
            devModeWarDir.shouldExist()
            val nocacheJs = File(devModeWarDir, "test/test.nocache.js")
            nocacheJs.shouldExist()
            val html = File(devModeWarDir, "test.html")
            html.shouldExist()
        }
        it("test가 성공해야 함") {
            result.task(":test")?.outcome shouldBe TaskOutcome.SUCCESS
        }
        it("build가 성공해야 함") {
            result.output shouldContain "BUILD SUCCESSFUL"
            result.task(":build")?.outcome shouldBe TaskOutcome.SUCCESS
        }
    }
}) {
    companion object {
        /**
         * 문자열에 특정 텍스트가 정확히 한 번만 포함되어 있는지 검증합니다.
         *
         * @param text 찾아야 할 텍스트
         * @throws AssertionError 텍스트가 0번 또는 2번 이상 나타나면 예외 발생
         */
        infix fun String.shouldContainOnlyOnce(text: String) {
            val count = this.split(text).size - 1
            withClue({
                "Expected text to contain '$text' exactly once, but it appeared $count times.\n" +
                        "Actual content:\n$this"
            }) {
                count shouldBe 1
            }
        }
    }
}
