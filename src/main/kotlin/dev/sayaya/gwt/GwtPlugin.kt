package dev.sayaya.gwt

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * GWT 개발을 위한 통합 Gradle 플러그인
 *
 * 이 플러그인은 [GwtLombokPlugin]과 [GwtTestPlugin]을 조합하여
 * GWT 프로젝트에 필요한 모든 기능을 제공합니다.
 *
 * ## 제공 기능
 * - Lombok 어노테이션 처리 자동 설정 (via [GwtLombokPlugin])
 * - GWT 테스트 컴파일 및 실행 환경 구성 (via [GwtTestPlugin])
 * - 테스트용 웹서버 자동 관리
 * - HTML 호스트 파일 자동 생성
 *
 * ## 사용 예시
 * ```kotlin
 * plugins {
 *     id("dev.sayaya.gwt") version "2.2.7"
 * }
 * ```
 *
 * @see GwtLombokPlugin
 * @see GwtTestPlugin
 */
class GwtPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(GwtLombokPlugin::class.java)
        project.plugins.apply(GwtTestPlugin::class.java)
    }
}