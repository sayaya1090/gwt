
package dev.sayaya.gwt

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Document
import java.io.File
import java.io.IOException
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * GWT 테스트 모듈을 위한 HTML 호스트 파일을 생성하는 태스크입니다.
 *
 * 이 태스크는 각 GWT 모듈에 대해 테스트 실행에 필요한 HTML 파일의 존재를 확인하고,
 * 파일이 없으면 자동으로 생성합니다. HTML 파일은 GWT 컴파일러가 생성한 JavaScript를
 * 로드하는 스크립트를 포함합니다.
 *
 * ## 동작 방식
 * 1. [modules]에 지정된 각 GWT 모듈에 대해 처리
 * 2. 모듈의 `.gwt.xml` 파일을 소스셋에서 검색
 * 3. XML에서 `rename-to` 속성을 읽어 최종 모듈 이름 결정
 * 4. [war] 디렉토리에 해당 이름의 HTML 파일이 없으면 생성
 *
 * ## 생성되는 HTML 구조
 * ```html
 * <!DOCTYPE html>
 * <html>
 * <head>
 *     <title>[모듈명] Test</title>
 *     <script type="text/javascript" src="[모듈명]/[모듈명].nocache.js"></script>
 * </head>
 * <body></body>
 * </html>
 * ```
 *
 * ## 예시
 * `com.example.App.gwt.xml`에 `rename-to="app"` 속성이 있는 경우,
 * `app.html` 파일이 생성됩니다.
 *
 * @see GwtTestCompileTask
 * @see GwtTestPlugin
 */
abstract class GwtGenerateTestHtmlTask : DefaultTask() {

    /**
     * 처리할 GWT 모듈의 완전한 이름 목록.
     *
     * 예: `["com.example.App", "com.example.Test"]`
     */
    @get:Input
    abstract val modules: ListProperty<String>

    /**
     * HTML 파일이 생성될 디렉토리
     *
     * 일반적으로 GWT의 war 디렉토리를 가리킵니다 (기본값: `src/main/webapp`).
     */
    @get:OutputDirectory
    abstract val war: DirectoryProperty

    /**
     * 사용자 정의 HTML 템플릿 파일 경로 (선택사항)
     *
     * 템플릿 파일에서 사용 가능한 플레이스홀더:
     * - `{{MODULE_NAME}}`: 모듈 이름으로 치환
     *
     * 템플릿이 지정되지 않으면 기본 템플릿을 사용합니다.
     */
    @get:InputFile
    @get:Optional
    abstract val htmlTemplate: RegularFileProperty

    /**
     * HTML 파일의 title 태그에 사용할 접미사
     *
     * 기본값: "Test"
     *
     * 예: moduleName이 "app"이고 titleSuffix가 "Test"이면
     * `<title>app Test</title>`로 생성됩니다.
     */
    @get:Input
    abstract val titleSuffix: Property<String>

    init {
        group = "GWT"
        description = "Generates HTML host files for GWT test modules"
        titleSuffix.convention("Test")
    }

    /**
     * 모든 모듈에 대해 HTML 파일을 생성합니다.
     *
     * [modules]에 지정된 각 모듈에 대해 [ensureTestHtmlFileForModule]을 호출합니다.
     */
    @TaskAction
    fun generateHtmlFiles() {
        modules.get().forEach { module ->
            ensureTestHtmlFileForModule(module)
        }
    }
    /**
     * 특정 모듈에 대한 HTML 파일의 존재를 확인하고 필요시 생성합니다.
     *
     * @param module 처리할 GWT 모듈의 완전한 이름 (예: "com.example.App")
     * @throws GradleException 모듈의 `.gwt.xml` 파일을 찾을 수 없는 경우
     */
    private fun ensureTestHtmlFileForModule(module: String) {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val packagePath = module.replace('.', '/')
        val xmlFile = findXmlFileInSourceSets(sourceSets, "$packagePath.gwt.xml")
            ?: throw GradleException("XML 파일을 찾을 수 없습니다: $packagePath.gwt.xml")

        val moduleName = getModuleNameFromXml(xmlFile, module)
        val htmlFile = ensureHtmlFileExists(war.get().asFile, moduleName)
        project.logger.info("모듈 HTML 파일 체크 완료: ${htmlFile.absolutePath}")
    }
    /**
     * GWT 모듈 XML 파일에서 최종 모듈 이름을 추출합니다.
     *
     * XML의 `<module>` 태그에 `rename-to` 속성이 있으면 그 값을 사용하고,
     * 없으면 원래 모듈 이름을 반환합니다.
     *
     * @param xmlFile GWT 모듈 XML 파일
     * @param defaultName `rename-to` 속성이 없을 때 사용할 기본 이름
     * @return 최종 모듈 이름
     */
    private fun getModuleNameFromXml(xmlFile: File, defaultName: String): String {
        val document = parseXmlFile(xmlFile)
        val renameToAttribute = document.documentElement.getAttribute("rename-to")
        return renameToAttribute.ifBlank { defaultName }
    }
    /**
     * 모든 소스셋에서 지정된 경로의 파일을 검색합니다.
     *
     * 메인 및 테스트 소스셋의 소스 디렉토리와 리소스 디렉토리를 모두 검색합니다.
     *
     * @param sourceSets 검색할 소스셋 컨테이너
     * @param fullPath 찾을 파일의 상대 경로 (예: "com/example/App.gwt.xml")
     * @return 찾은 파일, 없으면 null
     */
    private fun findXmlFileInSourceSets(sourceSets: SourceSetContainer, fullPath: String): File? =
        sourceSets.firstNotNullOfOrNull { sourceSet ->
            val searchDirs = sourceSet.resources.srcDirs + sourceSet.allSource.srcDirs
            searchDirs.asSequence()
                .map { dir -> File(dir, fullPath) }
                .firstOrNull { it.exists() }
        }
    /**
     * XML 파일을 안전하게 파싱합니다.
     *
     * XXE(XML External Entity) 공격을 방지하기 위해 보안 처리 기능을 활성화합니다.
     *
     * @param xmlFile 파싱할 XML 파일
     * @return 파싱된 XML 문서
     * @throws Exception XML 파싱 오류 발생 시
     */
    private fun parseXmlFile(xmlFile: File): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        return factory.newDocumentBuilder().parse(xmlFile)
    }
    /**
     * HTML 파일이 존재하는지 확인하고, 없으면 생성합니다.
     *
     * 파일이 이미 존재하는 경우 덮어쓰지 않습니다.
     * 필요한 경우 부모 디렉토리를 자동으로 생성합니다.
     *
     * @param testDir HTML 파일이 위치할 디렉토리
     * @param moduleName HTML 파일명의 기준이 될 모듈 이름
     * @return 생성되거나 이미 존재하는 HTML 파일
     * @throws GradleException 파일 생성 중 IO 오류 발생 시
     */
    private fun ensureHtmlFileExists(testDir: File, moduleName: String): File {
        val htmlFile = File(testDir, "$moduleName.html")
        if (htmlFile.exists()) {
            project.logger.info("HTML 파일이 이미 존재합니다: ${htmlFile.absolutePath}")
            return htmlFile
        }

        try {
            htmlFile.parentFile.mkdirs()
            htmlFile.writeText(createHtmlContent(moduleName))
            project.logger.lifecycle("HTML 파일이 생성되었습니다: ${htmlFile.absolutePath}")
        } catch (e: IOException) {
            throw GradleException("HTML 파일 생성 중 오류가 발생했습니다: ${e.message}", e)
        }
        return htmlFile
    }
    /**
     * GWT 테스트용 HTML 콘텐츠를 생성합니다.
     *
     * 사용자 정의 템플릿이 제공되면 해당 템플릿을 사용하고,
     * 없으면 기본 템플릿을 사용합니다.
     *
     * @param moduleName 모듈 이름 (제목과 스크립트 경로에 사용됨)
     * @return HTML 콘텐츠 문자열
     */
    private fun createHtmlContent(moduleName: String): String {
        // 사용자 정의 템플릿이 있으면 사용
        if (htmlTemplate.isPresent) {
            val templateContent = htmlTemplate.get().asFile.readText()
            return templateContent.replace("{{MODULE_NAME}}", moduleName)
        }

        // 기본 템플릿 사용
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>$moduleName ${titleSuffix.get()}</title>
                <script type="text/javascript" src="$moduleName/$moduleName.nocache.js"></script>
            </head>
            <body></body>
            </html>
        """.trimIndent()
    }
}