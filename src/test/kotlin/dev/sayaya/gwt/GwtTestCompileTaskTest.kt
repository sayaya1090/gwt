package dev.sayaya.gwt

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

class GwtTestCompileTaskTest : StringSpec({
    "클래스패스는 main과 test 소스, 그리고 test 런타임 의존성을 모두 포함해야 한다" {
        // 준비 (Arrange)
        val project: Project = ProjectBuilder.builder().build()
        project.pluginManager.apply("java")

        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val main = sourceSets.getByName("main")
        val test = sourceSets.getByName("test")

        // 테스트를 위한 가상 소스 디렉토리 생성 및 소스셋에 추가
        val mainSrcDir = File(project.projectDir, "src/main/java").apply { mkdirs() }
        main.java.srcDir(mainSrcDir)

        val testSrcDir = File(project.projectDir, "src/test/java").apply { mkdirs() }
        test.java.srcDir(testSrcDir)

        // 테스트를 위한 가상 의존성(jar) 파일 생성 및 의존성 추가
        val fakeJar = project.layout.buildDirectory.file("libs/fake.jar").get().asFile.apply {
            parentFile.mkdirs()
            createNewFile()
        }
        project.dependencies.add(test.runtimeOnlyConfigurationName, project.files(fakeJar))

        // 실행 (Act)
        val task = project.tasks.register("gwtTestCompile", GwtTestCompileTask::class.java).get()
        val taskClasspathFiles = task.classpath.files

        // 검증 (Assert)
        // java 플러그인이 기본으로 설정하는 출력 디렉토리 경로를 가져온다.
        val mainClassesDirs = main.output.classesDirs.files
        val testClassesDirs = test.output.classesDirs.files

        // 클래스패스에 포함되어야 할 파일들을 정의한다.
        val expectedFiles = mutableSetOf<File>()
        expectedFiles.add(mainSrcDir)
        expectedFiles.add(testSrcDir)
        expectedFiles.addAll(mainClassesDirs)
        expectedFiles.addAll(testClassesDirs)
        expectedFiles.add(fakeJar)

        // 태스크의 클래스패스가 예상된 모든 파일을 포함하는지 확인한다.
        taskClasspathFiles.shouldContainAll(expectedFiles)
    }
})