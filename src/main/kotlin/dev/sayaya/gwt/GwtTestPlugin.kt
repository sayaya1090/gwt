package dev.sayaya.gwt

import org.docstr.gwt.GwtCompileTask
import org.docstr.gwt.GwtDevModeTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.docstr.gwt.GwtPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.exclude

/**
 * GWT 테스트 컴파일 및 개발 모드를 설정하는 플러그인
 *
 * 이 플러그인은 다음을 수행합니다:
 * - 기본 `org.docstr.gwt` 플러그인 적용
 * - GWT 테스트 모듈 컴파일을 위한 `gwtTestCompile` 태스크 등록
 * - 테스트 소스를 포함하도록 `gwtDevMode` 설정
 * - 태스크 의존성 설정 (test는 gwtTestCompile에 의존, war는 gwtCompile에 의존)
 * - Java 컴파일에 UTF-8 인코딩 설정
 */
class GwtTestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("java")
        project.plugins.apply("org.docstr.gwt")
        project.configurations.all {
            exclude(group="jakarta.servlet", module="jakarta.servlet-api")
        }
        //val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        //val testSourceSet = sourceSets.getByName("test")

        val extension = project.extensions.getByType(GwtPluginExtension::class.java)
        val gwtCompileTask = project.tasks.named("gwtCompile")
        val gwtTestCompileTask = project.tasks.register("gwtTestCompile", GwtTestCompileTask::class.java, GwtTestCompileConfig(extension))

        project.tasks.apply {
            // 모든 JavaCompile 태스크에 UTF-8 인코딩을 설정합니다.
            withType(JavaCompile::class.java) {
                options.encoding = "UTF-8"
            }
            // 'test' 태스크가 'gwtTestCompile'에 의존하도록 설정합니다.
            named("test", Test::class.java) {
                useJUnitPlatform()
                dependsOn(gwtTestCompileTask)
            }
            project.plugins.withId("war") {
                named("war") {
                    dependsOn(gwtCompileTask)
                }
            }
        }
    }
}