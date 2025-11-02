package dev.sayaya.gwt

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.docstr.gwt.AbstractBaseTask
import org.gradle.kotlin.dsl.exclude

/**
 * GWT를 위한 Lombok 어노테이션 처리를 설정하는 플러그인
 *
 * 이 플러그인은 다음을 수행합니다:
 * - main과 test 소스셋에 생성된 소스 디렉토리 추가
 * - ECJ와 함께 Lombok java agent를 사용하도록 GWT 컴파일러 태스크 설정
 * - annotationProcessor 설정에서 Lombok 자동 감지
 */
class GwtLombokPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("java")
        project.plugins.apply("org.docstr.gwt")
        project.configurations.all {
            exclude(group="jakarta.servlet", module="jakarta.servlet-api")
        }
        project.extensions.getByType(SourceSetContainer::class.java).apply {
            named("main") {
                java.srcDirs(project.layout.buildDirectory.dir("generated/sources/annotationProcessor/java/main"))
            }
            named("test") {
                java.srcDirs(project.layout.buildDirectory.dir("generated/sources/annotationProcessor/java/test"))
            }
        }
        project.afterEvaluate {
            val annotationProcessorConfig = project.configurations.findByName("annotationProcessor")
            val lombok = annotationProcessorConfig?.find { it.name.startsWith("lombok") }
            if (lombok != null) {
                project.tasks.withType(AbstractBaseTask::class.java).configureEach {
                    jvmArgs = jvmArgs + "-javaagent:${lombok}=ECJ"
                }
            }
        }
    }
}