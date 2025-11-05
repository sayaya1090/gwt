package dev.sayaya.gwt

import org.docstr.gwt.GwtCompileTask
import org.gradle.api.GradleException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Document
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * GWT 테스트 소스를 컴파일하고, 테스트 실행에 필요한 HTML 호스트 파일을 확인 및 생성하는 태스크입니다.
 *
 * 이 태스크는 `GwtCompileTask`를 상속받아 GWT 컴파일 기능을 수행하며,
 * 추가적으로 각 GWT 모듈에 대한 테스트용 HTML 파일을 관리합니다.
 *
 * ## 주요 기능
 * - **모듈 XML (`*.gwt.xml`) 파일 탐색**: 소스셋을 탐색하여 GWT 모듈 정의 파일을 찾습니다.
 * - **모듈 이름 추출**: XML 파일의 `rename-to` 속성을 읽어 최종 모듈 이름을 결정합니다.
 * - **HTML 파일 생성**: `war` 디렉터리에 모듈 이름에 해당하는 HTML 파일이 없으면,
 *   GWT 테스트 실행을 위한 기본 HTML 파일을 자동으로 생성합니다.
 *
 * @see GwtCompileTask
 * @see GwtTestPlugin
 */
@CacheableTask
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
        dependsOn("processTestResources")
    }
    @TaskAction override fun exec() {
        modules.get().forEach(::ensureTestHtmlFileForModule)
        super.exec()
    }
    private fun ensureTestHtmlFileForModule(module: String) {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val packagePath = module.replace('.', '/')
        val xmlFile = findXmlFileInSourceSets(sourceSets, "$packagePath.gwt.xml")
            ?: throw GradleException("XML 파일을 찾을 수 없습니다: $packagePath.gwt.xml")

        val moduleName = getModuleNameFromXml(xmlFile, module)

        val htmlFile = ensureHtmlFileExists(war.get().asFile, moduleName)
        project.logger.info("모듈 HTML 파일 체크 및 완료: ${htmlFile.absolutePath}")
    }

    private fun getModuleNameFromXml(xmlFile: File, defaultName: String): String {
        val document = parseXmlFile(xmlFile)
        val renameToAttribute = document.documentElement.getAttribute("rename-to")
        return renameToAttribute.ifBlank { defaultName }
    }

    private fun findXmlFileInSourceSets(sourceSets: SourceSetContainer, fullPath: String): File? =
        sourceSets.firstNotNullOfOrNull { sourceSet ->
            // GWT 모듈 파일은 리소스 또는 소스 디렉터리에 있을 수 있습니다.
            val searchDirs = sourceSet.resources.srcDirs + sourceSet.allSource.srcDirs
            searchDirs.asSequence()
                .map { dir -> File(dir, fullPath) }
                .firstOrNull { it.exists() }
        }

    private fun parseXmlFile(xmlFile: File): Document {
        val factory = DocumentBuilderFactory.newInstance()
        // XXE(XML External Entity) 공격을 방지하기 위해 보안 처리 기능을 활성화합니다.
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        return factory.newDocumentBuilder().parse(xmlFile)
    }

    private fun ensureHtmlFileExists(testDir: File, moduleName: String): File {
        val htmlFile = File(testDir, "$moduleName.html")
        if (htmlFile.exists()) return htmlFile

        try {
            htmlFile.parentFile.mkdirs() // 디렉터리가 없는 경우 생성
            htmlFile.writeText(createHtmlContent(moduleName))
            project.logger.info("HTML 파일이 생성되었습니다: ${htmlFile.absolutePath}")
        } catch (e: IOException) {
            throw GradleException("HTML 파일 생성 중 오류가 발생했습니다: ${e.message}", e)
        }
        return htmlFile
    }

    private fun createHtmlContent(moduleName: String): String = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>$moduleName Test</title>
                <script type="text/javascript" src="$moduleName/$moduleName.nocache.js"></script>
            </head>
            <body></body>
            </html>
        """.trimIndent()
}
