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
 * 'gwtTestCompile' 태스크를 위한 설정 로직.
 *
 * 이 클래스는 'gwtDevMode' 태스크의 설정을 미러링(mirroring)합니다.
 * 'gwt' 블록의 devMode 설정을 우선적으로 사용하고, 값이 없으면 gwt 블록의 기본 설정을 사용합니다.
 * 이를 통해 'gwtTestCompile' 태스크가 GWT 컴파일에 필요한 모든 설정을 갖추도록 보장합니다.
 */
class GwtTestCompileConfig(private val extension: GwtPluginExtension) : Action<GwtTestCompileTask> {
    override fun execute(task: GwtTestCompileTask) {
        val delegate = GwtCompileConfig(GwtDevPluginExtension(extension))
        delegate.execute(task)
    }
    companion object {
        private fun <T: Any> Property<T>.withConvention(fallback: Property<T>): Property<T> = convention(fallback)
        private fun DirectoryProperty.withConvention(fallback: DirectoryProperty): DirectoryProperty = convention(fallback)
        private fun <T: Any> ListProperty<T>.withConvention(fallback: ListProperty<T>): ListProperty<T> = convention(fallback)
        private fun ConfigurableFileCollection.withConvention(fallback: ConfigurableFileCollection): ConfigurableFileCollection = convention(fallback)

        private class GwtDevPluginExtension(private val extension: GwtPluginExtension): GwtPluginExtension() {
            override fun getGwtVersion(): Property<String> = extension.gwtVersion
            override fun getJakarta(): Property<Boolean> = extension.jakarta
            override fun getCompiler(): CompilerOptions = extension.compiler
            override fun getDevMode(): DevModeOptions = extension.devMode
            override fun getGwtTest(): GwtTestOptions = extension.gwtTest
            override fun getSuperDev(): SuperDevOptions = extension.superDev
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
