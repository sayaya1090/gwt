package dev.sayaya.gwt

import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
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
class GwtPluginIntegrationTest : FunSpec({
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
    /*test("war 플러그인과 함께 사용 시 올바른 의존성이 설정되어야 함") {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("java")
        project.pluginManager.apply("war")
        project.pluginManager.apply(GwtPlugin::class.java)

        // GWT 확장 설정 (모듈 필수)
        val extension = project.extensions.getByType(GwtPluginExtension::class.java)
        extension.modules.set(listOf("test.Module"))

        val warTask = project.tasks.findByName("war")
        val jarTask = project.tasks.findByName("jar")
        val gwtCompileTask = project.tasks.findByName("gwtCompile")
        val testTask = project.tasks.findByName("test")
        val gwtTestCompileTask = project.tasks.findByName("gwtTestCompile")

        warTask shouldNotBe null
        jarTask shouldNotBe null
        gwtCompileTask shouldNotBe null
        testTask shouldNotBe null
        gwtTestCompileTask shouldNotBe null

        // war가 gwtCompile에 의존
        warTask!!.dependsOn.any { it.toString().contains("gwtCompile") } shouldBe true

        // test가 gwtTestCompile에 의존
        testTask!!.dependsOn.any { it.toString().contains("gwtTestCompile") } shouldBe true

        // jar 비활성화
        jarTask!!.enabled shouldBe false
    }*/
    test("전체 통합 테스트가 성공해야 함") {
        val projectDir = tempdir()
        val resourceDir = File(javaClass.classLoader.getResource("GwtPluginIntegrationTest")!!.toURI())
        resourceDir.copyRecursively(projectDir)

        val runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("build", "--stacktrace")

        val result = runner.build()
        println(result.output)
        result.output shouldContain "BUILD SUCCESSFUL"
        result.task(":build")?.outcome shouldBe TaskOutcome.SUCCESS
        println(result.output)
    }
})
