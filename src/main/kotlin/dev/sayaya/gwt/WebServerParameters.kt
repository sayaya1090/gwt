package dev.sayaya.gwt

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildServiceParameters

// 서비스 파라미터 정의
interface WebServerParameters : BuildServiceParameters {
    // 서버가 서빙할 루트 디렉토리 목록
    val contentRoot: ConfigurableFileCollection

    // 사용자가 지정한 포트 (없으면 0 또는 랜덤)
    val port: Property<Int>
}