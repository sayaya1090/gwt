package dev.sayaya

import dev.sayaya.gwt.GwtTestTaskExtension
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testfixtures.ProjectBuilder

class TestExtensionsTest : DescribeSpec({
    lateinit var project: Project
    lateinit var testTask: Test

    beforeEach {
        project = ProjectBuilder.builder().build()
        // 'Test' 태스크는 'java' 플러그인에 의해 자동으로 생성되므로 적용합니다.
        project.plugins.apply("java")
        testTask = project.tasks.getByName("test") as Test
    }

    describe("Test.gwt 확장 함수") {
        it("는 'gwt' 확장이 없으면 새로 생성해야 한다") {
            // 준비 (Arrange)
            // beforeEach에서 'gwt' 확장이 없는 상태로 시작합니다.

            // 실행 (Act)
            testTask.gwt {
                // 비어있는 설정 액션을 실행합니다.
            }

            // 검증 (Assert)
            val extension = testTask.extensions.findByName("gwt")
            extension.shouldNotBeNull()
            extension.shouldBeInstanceOf<GwtTestTaskExtension>()
        }

        it("는 'gwt' 확장이 이미 있으면 기존 인스턴스를 사용해야 한다") {
            // 준비 (Arrange)
            val initialExtension = testTask.extensions.create("gwt", GwtTestTaskExtension::class.java)

            // 실행 (Act)
            testTask.gwt {
                // 비어있는 설정 액션을 실행합니다.
            }

            // 검증 (Assert)
            val finalExtension = testTask.extensions.findByName("gwt")
            // 확장 함수 호출 후에도 객체 인스턴스가 동일한지 확인합니다.
            finalExtension shouldBe initialExtension
        }

        it("는 gwt 블록 내에서 프로퍼티를 설정할 수 있어야 한다") {
            // 준비 (Arrange)
            val expectedPort = 9999

            // 실행 (Act)
            testTask.gwt {
                webPort.set(expectedPort)
            }

            // 검증 (Assert)
            val extension = testTask.extensions.getByType(GwtTestTaskExtension::class.java)
            extension.webPort.get() shouldBe expectedPort
        }
    }
})