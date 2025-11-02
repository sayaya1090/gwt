package dev.sayaya.gwt

import org.gradle.api.Plugin
import org.gradle.api.Project

class GwtPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(GwtLombokPlugin::class.java)
        project.plugins.apply(GwtTestPlugin::class.java)
    }
}