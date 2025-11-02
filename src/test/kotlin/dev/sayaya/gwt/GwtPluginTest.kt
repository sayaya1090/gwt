package dev.sayaya.gwt

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class GwtPluginTest : DescribeSpec({
    lateinit var project: Project

    beforeEach {
        project = ProjectBuilder.builder().build()
    }

    describe("GwtPlugin") {
        it("GwtLombokPlugin을 적용해야 한다") {
            // GwtLombokPlugin이 'java' 플러그인에 의존하므로 먼저 적용해야 합니다.
            project.pluginManager.apply("java")
            project.pluginManager.apply(GwtPlugin::class.java)

            // GwtPlugin이 GwtLombokPlugin을 적용했는지 확인합니다.
            project.plugins.hasPlugin(GwtLombokPlugin::class.java) shouldBe true
        }
    }
})
