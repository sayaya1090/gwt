package dev.sayaya.gwt

import org.gradle.api.Project
import org.gradle.kotlin.dsl.exclude

/**
 * GWT 플러그인에서 사용하는 공통 유틸리티 함수들
 */

/**
 * Jakarta Servlet API를 모든 설정에서 제외합니다.
 *
 * GWT는 자체 Servlet API 구현을 포함하고 있어 Jakarta Servlet API와 충돌이 발생합니다.
 * 이 확장 함수는 모든 의존성 설정에서 jakarta.servlet:jakarta.servlet-api를 제외합니다.
 *
 * ## 사용 이유
 * - GWT는 `javax.servlet` 패키지를 사용
 * - Jakarta EE 9+는 `jakarta.servlet` 패키지로 마이그레이션
 * - 두 구현이 동시에 존재하면 클래스패스 충돌 발생
 *
 * ## 사용 예시
 * ```kotlin
 * class MyGwtPlugin : Plugin<Project> {
 *     override fun apply(project: Project) {
 *         project.excludeJakartaServletApi()
 *         // ... 기타 설정
 *     }
 * }
 * ```
 */
fun Project.excludeJakartaServletApi() {
    configurations.all {
        exclude(group = "jakarta.servlet", module = "jakarta.servlet-api")
    }
}
