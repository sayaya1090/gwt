package net.sayaya.gwt

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.gradle.testfixtures.ProjectBuilder

internal class GwtPluginIntegrationTest: BehaviorSpec({
    Given("Gradle setup") {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply(GwtPlugin::class.java)
        `when`("Plugin applied") {
            val tasks = project.tasks
            then("Tasks should be registered") {
                tasks.getByName("openWebServer") shouldNotBe null
                tasks.getByName("closeWebServer") shouldNotBe null
                tasks.getByName("gwtTest")  shouldNotBe null
                tasks.getByName("closeGwtCodeServer")  shouldNotBe null
            }
        }
    }
    Given("Plugin configured") {
        val buildFile = """
            plugins {
                id("net.sayaya.gwt")
            }
        """.trimIndent()
    }
})