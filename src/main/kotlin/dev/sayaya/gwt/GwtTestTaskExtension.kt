package dev.sayaya.gwt

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * 'tasks.test.gwt' 블록을 위한 확장(Extension) 클래스.
 * GWT 테스트 실행과 관련된 설정을 담당합니다.
 */
abstract class GwtTestTaskExtension @Inject constructor(objects: ObjectFactory) {
    /**
     * GWT 테스트를 위한 내장 웹 서버의 포트.
     */
    val webPort: Property<Int> = objects.property(Int::class.java).convention(8080)

    /**
     * GWT 코드 서버의 포트.
     */
    val codePort: Property<Int> = objects.property(Int::class.java)
}