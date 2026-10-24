package dev.sayaya.gwt

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.process.CommandLineArgumentProvider

/**
 * GWT를 위한 Lombok 어노테이션 처리를 설정하는 플러그인
 *
 * 이 플러그인은 다음을 수행합니다:
 * - 기반 플러그인([GwtBasePlugin]) 적용
 * - main과 test 소스셋에 생성된 소스 디렉토리 추가
 * - ECJ와 함께 Lombok java agent를 사용하도록 GWT 컴파일러 태스크 설정
 * - annotationProcessor 설정에서 Lombok 자동 감지
 */
class GwtLombokPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("java")
        project.plugins.apply(GwtBasePlugin::class.java)

        // Jakarta Servlet API 충돌 방지 (공통 유틸리티 함수 사용)
        project.excludeJakartaServletApi()

        project.extensions.getByType(SourceSetContainer::class.java).apply {
            named("main") {
                java.srcDirs(project.layout.buildDirectory.dir("generated/sources/annotationProcessor/java/main"))
            }
            named("test") {
                java.srcDirs(project.layout.buildDirectory.dir("generated/sources/annotationProcessor/java/test"))
            }
        }
        project.afterEvaluate {
            // 설정 시점에 annotationProcessor를 해석하지 않도록, Lombok jar 탐색은 태스크 실행 시점으로 미룬다.
            val annotationProcessor = project.configurations.findByName("annotationProcessor") ?: return@afterEvaluate
            project.tasks.withType(GwtBaseTask::class.java).configureEach {
                jvmArgumentProviders.add(LombokAgentArgumentProvider(annotationProcessor))
            }
        }
    }
}

/**
 * [processorPath]에서 첫 번째 Lombok jar를 찾아 `-javaagent:<jar>=ECJ` 인자를 실행 시점에 만듭니다.
 * Lombok이 없으면 인자를 추가하지 않습니다.
 */
internal class LombokAgentArgumentProvider(
    @get:Classpath val processorPath: FileCollection
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> =
        processorPath.firstOrNull { LOMBOK_JAR.matches(it.name) }
            ?.let { listOf("-javaagent:${it.absolutePath}=ECJ") }
            ?: emptyList()

    private companion object {
        // Lombok jar 파일명 패턴: lombok-1.18.30.jar, lombok-1.18.30-sources.jar 등
        val LOMBOK_JAR = Regex("^lombok-\\d+\\.\\d+\\.\\d+.*\\.jar$")
    }
}