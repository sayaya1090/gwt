
package dev.sayaya.gwt

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

/**
 * GWT 테스트 컴파일을 위한 클래스패스를 구성하는 태스크입니다.
 *
 * 이 태스크는 메인 소스와 테스트 소스를 모두 포함하는 클래스패스를 구성합니다.
 * 구성된 클래스패스는 [testClasspath] 프로퍼티를 통해 다른 태스크에서 사용할 수 있습니다.
 *
 * **중요:** 이 태스크는 실행 시점(TaskAction)이 아닌 구성 시점(init)에 클래스패스를 설정합니다.
 * 이는 Gradle의 프로퍼티 finalization 메커니즘과 호환되기 위함입니다.
 *
 * ## 구성 요소
 * - 메인 소스 디렉토리 (src/main/java, src/main/kotlin 등)
 * - 테스트 소스 디렉토리 (src/test/java, src/test/kotlin 등)
 * - 메인 출력 클래스 디렉토리 (build/classes/java/main 등)
 * - 테스트 출력 클래스 디렉토리 (build/classes/java/test 등)
 * - 메인 리소스 디렉토리 (build/resources/main)
 * - 테스트 리소스 디렉토리 (build/resources/test)
 * - 테스트 런타임 클래스패스 (모든 의존성 라이브러리 포함)
 *
 * ## 사용 예시
 * ```kotlin
 * val configureClasspath = tasks.named<GwtConfigureTestClasspathTask>("gwtConfigureTestClasspath")
 * gwtTestCompile.configure {
 *     classpath(configureClasspath.get().testClasspath)
 * }
 * ```
 *
 * @see GwtTestCompileTask
 * @see GwtTestPlugin
 */
abstract class GwtConfigureTestClasspathTask : DefaultTask() {

    /**
     * 구성된 테스트 클래스패스입니다.
     *
     * 이 프로퍼티는 태스크 생성 시점에 다음 항목들을 포함하도록 설정됩니다:
     * - 메인 및 테스트 소스 디렉토리
     * - 메인 및 테스트 출력 디렉토리
     * - 테스트 런타임 의존성
     */
    @get:OutputFiles
    abstract val testClasspath: ConfigurableFileCollection

    init {
        group = "GWT"
        description = "Configures classpath for GWT test compilation"

        // 소스 컴파일 태스크에 의존
        dependsOn("compileJava", "compileTestJava")
        dependsOn("processResources", "processTestResources")

        // 클래스패스 구성 (구성 단계에서 수행)
        configureClasspath()
    }

    /**
     * 테스트 컴파일을 위한 클래스패스를 구성합니다.
     *
     * 이 메서드는 init 블록에서 호출되어 구성 단계에서 클래스패스를 설정합니다.
     * 다음 작업을 수행합니다:
     * 1. 메인 및 테스트 소스셋 가져오기
     * 2. 각 소스셋의 소스 디렉토리, 출력 디렉토리 수집
     * 3. 테스트 런타임 클래스패스 수집
     * 4. 모든 항목을 [testClasspath]에 추가
     */
    private fun configureClasspath() {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        val testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)

        // 메인 소스 경로
        val mainSourcePaths = project.files(mainSourceSet.allSource.srcDirs)

        // 메인 출력 클래스패스
        val mainOutputClasspath = mainSourceSet.output.classesDirs
            .plus(project.files(mainSourceSet.output.resourcesDir))

        // 테스트 소스 경로
        val testSourcePaths = project.files(testSourceSet.allSource.srcDirs)

        // 테스트 출력 클래스패스
        val testOutputClasspath = testSourceSet.output.classesDirs
            .plus(project.files(testSourceSet.output.resourcesDir))

        // 테스트 런타임 클래스패스 (모든 의존성 포함)
        val testRuntimeClasspath = testSourceSet.runtimeClasspath

        // 모든 클래스패스를 testClasspath에 추가
        testClasspath.from(
            mainSourcePaths,
            mainOutputClasspath,
            testSourcePaths,
            testOutputClasspath,
            testRuntimeClasspath
        )

        project.logger.info("테스트 클래스패스 구성 완료")
    }
}