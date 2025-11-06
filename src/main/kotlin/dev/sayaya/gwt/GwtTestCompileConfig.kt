package dev.sayaya.gwt

import org.docstr.gwt.GwtCompileConfig
import org.docstr.gwt.GwtPluginExtension
import org.docstr.gwt.options.CompilerOptions
import org.docstr.gwt.options.DevModeOptions
import org.docstr.gwt.options.GwtTestOptions
import org.docstr.gwt.options.SuperDevOptions
import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * [GwtTestCompileTask]의 설정을 구성하는 Action 클래스입니다.
 *
 * 이 클래스는 `gwtDevMode` 태스크의 설정을 미러링하여 테스트 컴파일에 적용합니다.
 * [GwtPluginExtension]의 `devMode` 설정을 우선적으로 사용하고,
 * 값이 없으면 기본 설정을 폴백(fallback)으로 사용합니다.
 *
 * ## 설정 우선순위
 * 1. `gwt.devMode.*` 설정 (최우선)
 * 2. `gwt.*` 기본 설정 (폴백)
 *
 * ## 예시
 * ```kotlin
 * gwt {
 *     sourceLevel = "11"           // 기본값
 *     devMode {
 *         sourceLevel = "17"       // 이 값이 사용됨
 *     }
 * }
 * ```
 *
 * @property extension GWT 플러그인 확장 설정
 * @see GwtTestCompileTask
 * @see GwtPluginExtension
 */
class GwtTestCompileConfig(private val extension: GwtPluginExtension) : Action<GwtTestCompileTask> {

    /**
     * 태스크에 설정을 적용합니다.
     *
     * [GwtDevPluginExtension]을 통해 devMode 설정이 우선 적용되도록 하고,
     * [GwtCompileConfig]에 위임하여 실제 설정을 수행합니다.
     *
     * @param task 설정을 적용할 GwtTestCompileTask 인스턴스
     */
    override fun execute(task: GwtTestCompileTask) {
        val delegate = GwtCompileConfig(GwtDevPluginExtension(extension))
        delegate.execute(task)
    }

    companion object {
        /**
         * Property에 convention(기본값)을 설정하는 확장 함수입니다.
         */
        private fun <T: Any> Property<T>.withConvention(fallback: Property<T>): Property<T> = convention(fallback)

        /**
         * DirectoryProperty에 convention(기본값)을 설정하는 확장 함수입니다.
         */
        private fun DirectoryProperty.withConvention(fallback: DirectoryProperty): DirectoryProperty = convention(fallback)

        /**
         * ListProperty에 convention(기본값)을 설정하는 확장 함수입니다.
         */
        private fun <T: Any> ListProperty<T>.withConvention(fallback: ListProperty<T>): ListProperty<T> = convention(fallback)

        /**
         * ConfigurableFileCollection에 convention(기본값)을 설정하는 확장 함수입니다.
         */
        private fun ConfigurableFileCollection.withConvention(fallback: ConfigurableFileCollection): ConfigurableFileCollection = convention(fallback)

        /**
         * devMode 설정을 우선적으로 적용하는 GwtPluginExtension 래퍼 클래스입니다.
         *
         * 이 클래스는 [GwtPluginExtension]을 상속받아 모든 getter 메서드를 오버라이드하며,
         * devMode 설정 값이 있으면 그것을 사용하고, 없으면 기본 설정을 사용합니다.
         *
         * @property extension 원본 GWT 플러그인 확장 설정
         */
        private class GwtDevPluginExtension(private val extension: GwtPluginExtension): GwtPluginExtension() {
            override fun getGwtVersion(): Property<String> = extension.gwtVersion
            override fun getJakarta(): Property<Boolean> = extension.jakarta
            override fun getCompiler(): CompilerOptions = extension.compiler
            override fun getDevMode(): DevModeOptions = extension.devMode
            override fun getGwtTest(): GwtTestOptions = extension.gwtTest
            override fun getSuperDev(): SuperDevOptions = extension.superDev

            // devMode 설정을 우선 적용하고, 없으면 기본 설정 사용
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