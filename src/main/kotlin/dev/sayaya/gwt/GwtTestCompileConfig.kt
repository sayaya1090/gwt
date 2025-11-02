package dev.sayaya.gwt

import org.docstr.gwt.GwtPluginExtension
import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileCollection
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
        val devMode = extension.devMode
        val project = task.project

        // devMode와 기본 gwt 설정 간의 값 승계 로직을 사용하여 프로퍼티를 설정합니다.
        setProperty(task.logLevel, devMode.logLevel, extension.logLevel)
        setProperty(task.gen, devMode.gen, extension.gen)
        setProperty(task.war, devMode.war, extension.war)
        setProperty(task.deploy, devMode.deploy, extension.deploy)
        setProperty(task.extra, devMode.extra, extension.extra)
        setProperty(task.cacheDir, devMode.cacheDir, extension.cacheDir)
        setProperty(task.workDir, devMode.workDir, extension.workDir)
        setProperty(task.sourceLevel, devMode.sourceLevel, extension.sourceLevel)
        setProperty(task.style, devMode.style, extension.style)
        setProperty(task.incremental, devMode.incremental, extension.incremental)
        setProperty(task.failOnError, devMode.failOnError, extension.failOnError)
        setProperty(task.generateJsInteropExports, devMode.generateJsInteropExports, extension.generateJsInteropExports)
        setProperty(task.methodNameDisplayMode, devMode.methodNameDisplayMode, extension.methodNameDisplayMode)

        // List 타입 프로퍼티 설정
        setListProperty(task.includeJsInteropExports, devMode.includeJsInteropExports, extension.includeJsInteropExports)
        setListProperty(task.excludeJsInteropExports, devMode.excludeJsInteropExports, extension.excludeJsInteropExports)
        setListProperty(task.setProperty, devMode.setProperty, extension.setProperty)
        setListProperty(task.modules, devMode.modules, extension.modules)

        // FileCollection 타입 프로퍼티 설정
        setFileCollection(task.extraSourceDirs, devMode.extraSourceDirs, extension.extraSourceDirs)

        // 클래스패스와 인자 설정
        task.configureClasspath(project)
        task.configureArgs()
    }

    /**
     * devMode의 값을 우선으로 하여 Property를 설정하는 헬퍼 함수.
     */
    private fun <T : Any> setProperty(target: Property<T>, devModeProp: Property<T>, baseProp: Property<T>, default: T? = null) {
        if (devModeProp.isPresent) target.set(devModeProp)
        else if (baseProp.isPresent) target.set(baseProp)
    }

    /**
     * devMode의 값을 우선으로 하여 ListProperty를 설정하는 헬퍼 함수.
     */
    private fun <T : Any> setListProperty(target: ListProperty<T>, devModeProp: ListProperty<T>, baseProp: ListProperty<T>) {
        val devValue = devModeProp.getOrElse(emptyList())
        if (devValue.isNotEmpty()) target.set(devValue)
        else target.set(baseProp)
    }

    /**
     * devMode의 값을 우선으로 하여 ConfigurableFileCollection을 설정하는 헬퍼 함수.
     */
    private fun setFileCollection(
        target: ConfigurableFileCollection,
        devModeCollection: ConfigurableFileCollection,
        baseCollection: ConfigurableFileCollection
    ) {
        if (!devModeCollection.isEmpty) target.from(devModeCollection)
        else if (!baseCollection.isEmpty) target.from(baseCollection)
    }
}