package dev.sayaya.gwt

import org.gradle.api.provider.Property

/**
 * GWT 테스트 설정을 위한 익스텐션입니다.
 * 
 * ## 주요 설정
 * - [webPort]: 테스트용 웹 서버가 사용할 포트 번호를 지정합니다.
 */
abstract class GwtTestTaskExtension {
    /**
     * 테스트용 웹 서버가 사용할 포트 번호입니다.
     * 
     * 설정하지 않거나 0으로 설정하면 사용 가능한 랜덤 포트를 자동으로 할당합니다.
     */
    abstract val webPort: Property<Int>
}
