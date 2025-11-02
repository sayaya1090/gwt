package dev.sayaya.gwt

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class GwtPluginTest : DescribeSpec({
    lateinit var project: Project

    beforeEach {
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply(GwtPlugin::class.java)
    }

    describe("GwtPlugin") {
        it("GwtLombokPlugin을 적용해야 한다") {
            project.plugins.hasPlugin(GwtLombokPlugin::class.java) shouldBe true
        }
        it("GwtTestPlugin을 적용해야 한다") {
            project.plugins.hasPlugin(GwtTestPlugin::class.java) shouldBe true
        }
    }
})
