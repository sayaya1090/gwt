package dev.sayaya.gwt

import org.gradle.api.DefaultTask

/**
 * GWT 테스트를 위한
 */
abstract class GwtTestTask: DefaultTask() {
    init {
        dependsOn("gwtTestCompile")
        dependsOn("openWebServer")
        finalizedBy("closeWebServer")
    }
}
