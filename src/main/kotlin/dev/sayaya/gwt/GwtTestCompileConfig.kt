package dev.sayaya.gwt

import org.docstr.gwt.GwtPluginExtension
import org.gradle.api.Action

class GwtTestCompileConfig(private val extension: GwtPluginExtension) : Action<GwtTestCompileTask> {
    override fun execute(task: GwtTestCompileTask) {
        // gwt.devMode.modules 값을 gwtTestCompile 태스크의 modules 프로퍼티로 설정합니다.
        task.modules.set(extension.devMode.modules)
        // 테스트 리소스가 먼저 처리되도록 의존성을 추가합니다.
        task.dependsOn("processTestResources")
    }
}