package dev.sayaya.gwt

import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import javax.inject.Inject

/**
 * GWT 테스트 소스를 컴파일하는 태스크입니다.
 *
 * 이 태스크는 [GwtCompileTask]를 상속받아 메인 소스와 테스트 소스를 모두 포함하여
 * GWT 모듈을 컴파일합니다. `gwt.devMode` 설정을 사용하며, devMode에 지정하지 않은 공통 옵션은
 * [GwtBasePlugin]의 convention에 따라 상위 `gwt` 설정을 이어받습니다.
 *
 * ## 특징
 * - devMode 설정 우선 적용 (fallback: base 설정)
 * - 테스트 소스/리소스/클래스패스 자동 포함 (`gwt.devMode.extraSourceDirs`에 추가)
 * - HTML 호스트 파일 생성 태스크에 자동 의존
 *
 * ## 사용 예시
 * ```bash
 * # 직접 실행
 * ./gradlew gwtTestCompile
 *
 * # test 실행 시 자동 실행
 * ./gradlew test
 * ```
 *
 * ## 주의사항
 * - [GwtBasePlugin]이 적용된 프로젝트에서만 생성할 수 있습니다(`gwt` 확장 필요).
 *
 * @see GwtCompileTask
 * @see GwtGenerateTestHtmlTask
 * @see GwtTestPlugin
 */
@CacheableTask
abstract class GwtTestCompileTask @Inject constructor() : GwtCompileTask() {

    init {
        description = "Compiles GWT test sources with main sources"
        dependsOn("gwtGenerateTestHtml")

        val extension = project.extensions.getByType(GwtPluginExtension::class.java)
        configureTestClasspath(extension)
        configureFrom(extension.devMode)
        configureCompilerFrom(extension.compiler)
        extraSourceDirs.from(extension.extraSourceDirs, extension.devMode.extraSourceDirs)
    }

    /**
     * 메인·테스트 소스와 클래스패스를 devMode의 extraSourceDirs에 추가합니다.
     */
    private fun configureTestClasspath(extension: GwtPluginExtension) {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        val testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)

        extension.devMode.extraSourceDirs.from(
            mainSourceSet.allSource.sourceDirectories,
            mainSourceSet.resources.sourceDirectories,
            mainSourceSet.output,

            testSourceSet.allSource.sourceDirectories,
            testSourceSet.resources.sourceDirectories,
            testSourceSet.output,
            testSourceSet.runtimeClasspath
        )
    }
}
