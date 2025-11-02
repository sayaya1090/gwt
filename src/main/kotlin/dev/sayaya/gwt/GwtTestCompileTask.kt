package dev.sayaya.gwt

import org.docstr.gwt.GwtCompileTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSetContainer
import javax.inject.Inject

abstract class GwtTestCompileTask @Inject constructor(objects: ObjectFactory) : GwtCompileTask(objects) {
    init {
        group = "GWT"
        description = "Compiles GWT test sources"

        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val mainSourceSet = sourceSets.named("main")
        val testSourceSet = sourceSets.named("test")

        // GWT 테스트 컴파일에 필요한 클래스패스를 지연 설정합니다.
        // 1. test의 런타임 클래스패스 (컴파일된 클래스 및 모든 의존성)
        // 2. main과 test의 소스 디렉터리 (GWT 컴파일러가 .java 파일을 찾기 위해 필요)
        classpath = testSourceSet.get().runtimeClasspath +
                project.files(mainSourceSet.map { it.java.srcDirs }) +
                project.files(testSourceSet.map { it.java.srcDirs })
    }
}