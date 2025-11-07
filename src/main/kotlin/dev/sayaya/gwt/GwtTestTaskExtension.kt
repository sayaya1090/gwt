package dev.sayaya.gwt

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Test 태스크에 대한 GWT 확장 설정 클래스
 *
 * GWT 테스트 실행과 관련된 설정을 담당합니다.
 * `tasks.test.gwt { }` 블록을 통해 접근할 수 있습니다.
 *
 * ## 사용 예시
 * ```kotlin
 * tasks.test {
 *     extensions.configure<GwtTestTaskExtension>("gwt") {
 *         webPort.set(9876)
 *         codePort.set(9877)
 *     }
 * }
 * ```
 *
 * 또는 확장 함수 사용:
 * ```kotlin
 * tasks.test {
 *     gwt {
 *         webPort.set(9876)
 *     }
 * }
 * ```
 *
 * @see dev.sayaya.gwt
 */
abstract class GwtTestTaskExtension @Inject constructor(objects: ObjectFactory) {
    /**
     * GWT 테스트를 위한 내장 웹 서버의 포트
     *
     * 기본값: 8080
     *
     * 이 웹서버는 GWT 컴파일된 JavaScript 파일과 HTML 호스트 파일을
     * 제공하며, 테스트 실행 전에 자동으로 시작되고 테스트 완료 후 종료됩니다.
     */
    val webPort: Property<Int> = objects.property(Int::class.java).convention(8080)

    /**
     * GWT 코드 서버의 포트 (개발 모드용)
     *
     * 기본값: 설정되지 않음
     *
     * SuperDevMode 실행 시 사용되는 코드 서버의 포트를 지정합니다.
     */
    val codePort: Property<Int> = objects.property(Int::class.java)
}