package dev.sayaya.gwt

import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

class GwtLombokPluginIntegrationTest : StringSpec({
    "Lombok 플러그인이 적용된 빌드는 성공해야 한다" {
        // 준비: 테스트 프로젝트를 위한 임시 디렉터리 생성 및 리소스 복사
        val projectDir = tempdir()
        val resourceDir = File(javaClass.classLoader.getResource("GwtPluginLombokPluginTest")!!.toURI())
        resourceDir.copyRecursively(projectDir)

        // 실행: GradleRunner를 사용하여 'build' 태스크 실행
        val runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("build", "--stacktrace")

        val result = runner.build()

        println(result.output)
        // 검증: 빌드 결과 확인
        result.output shouldContain "BUILD SUCCESSFUL"
        result.task(":gwtCompile")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":build")?.outcome shouldBe TaskOutcome.SUCCESS
    }
})