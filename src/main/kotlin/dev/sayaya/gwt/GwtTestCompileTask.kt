package dev.sayaya.gwt

import org.docstr.gwt.GwtCompileConfig
import org.docstr.gwt.GwtCompileTask
import org.docstr.gwt.GwtPluginExtension
import org.docstr.gwt.options.CompilerOptions
import org.docstr.gwt.options.DevModeOptions
import org.docstr.gwt.options.GwtTestOptions
import org.docstr.gwt.options.SuperDevOptions
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import javax.inject.Inject

/**
 * GWT 테스트 소스를 컴파일하는 태스크입니다.
 *
 * 이 태스크는 [GwtCompileTask]를 상속받아 메인 소스와 테스트 소스를 모두 포함하여
 * GWT 모듈을 컴파일합니다. devMode 설정을 우선적으로 적용하며, 테스트 소스를
 * extraSourceDirs에 자동으로 추가합니다.
 *
 * ## 특징
 * - devMode 설정 우선 적용 (fallback: base 설정)
 * - 테스트 소스/리소스/클래스패스 자동 포함
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
 * @see GwtCompileTask
 * @see GwtGenerateTestHtmlTask
 * @see GwtTestPlugin
 */
@CacheableTask
abstract class GwtTestCompileTask @Inject constructor(
    objects: ObjectFactory
) : GwtCompileTask(objects) {

    init {
        group = "GWT"
        description = "Compiles GWT test sources with main sources"
        dependsOn("gwtGenerateTestHtml")

        configureTestClasspath()
        applyDevModeSettings()
    }

    /**
     * 테스트 소스와 클래스패스를 extraSourceDirs에 추가합니다.
     */
    private fun configureTestClasspath() {
        val extension = project.extensions.getByType(GwtPluginExtension::class.java)
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

    /**
     * devMode 설정을 우선 적용하여 태스크를 구성합니다.
     */
    private fun applyDevModeSettings() {
        val extension = project.extensions.getByType(GwtPluginExtension::class.java)
        val proxy = GwtDevPluginExtension(extension)
        GwtCompileConfig(proxy).execute(this)
    }

    companion object {
        /**
         * Property에 convention(fallback)을 설정하는 확장 함수입니다.
         */
        private fun <T : Any> Property<T>.withConvention(fallback: Property<T>): Property<T> = convention(fallback)

        /**
         * DirectoryProperty에 convention(fallback)을 설정하는 확장 함수입니다.
         */
        private fun DirectoryProperty.withConvention(fallback: DirectoryProperty): DirectoryProperty = convention(fallback)

        /**
         * ListProperty에 convention(fallback)을 설정하는 확장 함수입니다.
         */
        private fun <T : Any> ListProperty<T>.withConvention(fallback: ListProperty<T>): ListProperty<T> = convention(fallback)

        /**
         * ConfigurableFileCollection에 convention(fallback)을 설정하는 확장 함수입니다.
         */
        private fun ConfigurableFileCollection.withConvention(fallback: ConfigurableFileCollection): ConfigurableFileCollection = convention(fallback)

        /**
         * devMode 설정을 우선 적용하는 GwtPluginExtension 프록시 클래스입니다.
         *
         * 이 클래스는 [GwtPluginExtension]을 상속받아 모든 프로퍼티 getter를 오버라이드하며,
         * devMode에 설정된 값이 있으면 그것을 사용하고, 없으면 base 설정을 fallback으로 사용합니다.
         *
         * @property extension 원본 GWT 플러그인 확장 설정
         */
        internal class GwtDevPluginExtension(
            private val extension: GwtPluginExtension
        ) : GwtPluginExtension() {

            // 기본 옵션들은 그대로 사용
            override fun getGwtVersion(): Property<String> = extension.gwtVersion
            override fun getJakarta(): Property<Boolean> = extension.jakarta
            override fun getCompiler(): CompilerOptions = extension.compiler
            override fun getDevMode(): DevModeOptions = extension.devMode
            override fun getGwtTest(): GwtTestOptions = extension.gwtTest
            override fun getSuperDev(): SuperDevOptions = extension.superDev

            // devMode 설정을 우선 적용 (fallback: base 설정)
            override fun getMinHeapSize(): Property<String> = extension.devMode.minHeapSize.withConvention(extension.minHeapSize)
            override fun getMaxHeapSize(): Property<String> = extension.devMode.maxHeapSize.withConvention(extension.maxHeapSize)
            override fun getLogLevel(): Property<String> = extension.devMode.logLevel.withConvention(extension.logLevel)
            override fun getWorkDir(): DirectoryProperty = extension.devMode.workDir.withConvention(extension.workDir)
            override fun getGen(): DirectoryProperty = extension.devMode.gen.withConvention(extension.gen)
            override fun getWar(): DirectoryProperty = extension.devMode.war.withConvention(extension.war)
            override fun getDeploy(): DirectoryProperty = extension.devMode.deploy.withConvention(extension.deploy)
            override fun getExtra(): DirectoryProperty = extension.devMode.extra.withConvention(extension.extra)
            override fun getCacheDir(): DirectoryProperty = extension.devMode.cacheDir.withConvention(extension.cacheDir)
            override fun getGenerateJsInteropExports(): Property<Boolean> = extension.devMode.generateJsInteropExports.withConvention(extension.generateJsInteropExports)
            override fun getIncludeJsInteropExports(): ListProperty<String> = extension.devMode.includeJsInteropExports.withConvention(extension.includeJsInteropExports)
            override fun getExcludeJsInteropExports(): ListProperty<String> = extension.devMode.excludeJsInteropExports.withConvention(extension.excludeJsInteropExports)
            override fun getMethodNameDisplayMode(): Property<String> = extension.devMode.methodNameDisplayMode.withConvention(extension.methodNameDisplayMode)
            override fun getSourceLevel(): Property<String> = extension.devMode.sourceLevel.withConvention(extension.sourceLevel)
            override fun getIncremental(): Property<Boolean> = extension.devMode.incremental.withConvention(extension.incremental)
            override fun getStyle(): Property<String> = extension.devMode.style.withConvention(extension.style)
            override fun getFailOnError(): Property<Boolean> = extension.devMode.failOnError.withConvention(extension.failOnError)
            override fun getSetProperty(): ListProperty<String> = extension.devMode.setProperty.withConvention(extension.setProperty)
            override fun getModules(): ListProperty<String> = extension.devMode.modules.withConvention(extension.modules)
            override fun getExtraSourceDirs(): ConfigurableFileCollection = extension.devMode.extraSourceDirs.withConvention(extension.extraSourceDirs)
        }
    }
}