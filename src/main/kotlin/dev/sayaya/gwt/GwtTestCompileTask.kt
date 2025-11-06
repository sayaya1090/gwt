package dev.sayaya.gwt

import org.docstr.gwt.GwtCompileTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Document
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * GWT 테스트 소스를 컴파일하는 태스크입니다.
 *
 * 이 태스크는 [GwtCompileTask]를 상속받아 GWT 컴파일 기능을 수행합니다.
 *
 * ## 태스크 의존성
 * - `gwtCompile`: 메인 소스 컴파일 (자동 의존)
 * - `gwtConfigureTestClasspath`: 테스트 클래스패스 구성
 * - `gwtGenerateTestHtml`: HTML 호스트 파일 생성
 *
 * ## 사용 예시
 * ```bash
 * # 태스크 직접 실행
 * ./gradlew gwtTestCompile
 *
 * # test 태스크 실행 시 자동 실행됨
 * ./gradlew test
 * ```
 *
 * @see GwtCompileTask
 * @see GwtConfigureTestClasspathTask
 * @see GwtGenerateTestHtmlTask
 * @see GwtTestPlugin
 */
@CacheableTask
abstract class GwtTestCompileTask @Inject constructor(objects: ObjectFactory) : GwtCompileTask(objects) {
    init {
        group = "GWT"
        description = "Compiles GWT test sources"
    }
}
