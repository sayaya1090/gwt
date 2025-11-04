package dev.sayaya.gwt.test

/**
 * GWT 테스트용 HTML 파일 경로를 지정하는 어노테이션
 *
 * @property path HTML 파일의 경로 (상대 경로 또는 절대 경로)
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GwtHtml(val path: String)