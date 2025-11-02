package dev.sayaya.gwt

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.docstr.gwt.AbstractBaseTask
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

class GwtLombokPluginTest : DescribeSpec({
    lateinit var project: Project

    beforeEach {
        project = ProjectBuilder.builder().build()
    }

    describe("GwtLombokPlugin") {
        context("프로젝트에 적용될 때") {
            it("java 플러그인이 함께 적용되면 성공적으로 적용되어야 한다") {
                project.pluginManager.apply("java")
                project.pluginManager.apply(GwtLombokPlugin::class.java)
                // 예외가 발생하지 않으면 성공
            }

            it("java 플러그인이 적용되지 않으면 예외가 발생해야 한다") {
                val exception = shouldThrow<PluginApplicationException> {
                    project.pluginManager.apply(GwtLombokPlugin::class.java)
                }
                exception.cause.shouldBeInstanceOf<UnknownDomainObjectException>()
            }
        }

        context("java 플러그인이 적용된 경우") {
            beforeEach {
                project.pluginManager.apply("java")
                project.pluginManager.apply(GwtLombokPlugin::class.java)
            }

            it("main 및 test 소스셋에 생성된 소스 디렉토리를 추가해야 한다") {
                val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
                val mainSrcDirs = sourceSets.getByName("main").java.srcDirs.map { it.toPathString() }
                val testSrcDirs = sourceSets.getByName("test").java.srcDirs.map { it.toPathString() }

                mainSrcDirs.any { it.endsWith("build/generated/sources/annotationProcessor/java/main") } shouldBe true
                testSrcDirs.any { it.endsWith("build/generated/sources/annotationProcessor/java/test") } shouldBe true
            }

            it("custom 소스셋에는 생성된 소스 디렉토리를 추가하지 않아야 한다") {
                val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
                sourceSets.create("custom")
                val customSrcDirs = sourceSets.getByName("custom").java.srcDirs.map { it.toPathString() }

                customSrcDirs.any { it.contains("build/generated/sources/annotationProcessor/java/custom") } shouldBe false
            }
        }

        context("GWT 태스크 설정") {
            fun setupGwtProject() {
                project.pluginManager.apply("java")
                project.pluginManager.apply("org.docstr.gwt")
                project.extensions.getByType(org.docstr.gwt.GwtPluginExtension::class.java).apply {
                    modules.set(listOf("test.Module"))
                }
                project.pluginManager.apply(GwtLombokPlugin::class.java)
            }

            fun addAnnotationProcessor(vararg jarNames: String): List<File> {
                val buildDir = project.layout.buildDirectory
                val jars = jarNames.map { name ->
                    buildDir.file(name).get().asFile.also {
                        it.parentFile.mkdirs()
                        it.createNewFile()
                    }
                }
                project.dependencies.add("annotationProcessor", project.files(jars))
                return jars
            }

            fun evaluateProject() {
                (project as ProjectInternal).evaluate()
            }

            fun getGwtTasks(): List<AbstractBaseTask> {
                return project.tasks.withType(AbstractBaseTask::class.java).toList()
            }

            it("단일 lombok 의존성이 있을 때 javaagent JVM 인수를 설정해야 한다") {
                setupGwtProject()
                val lombokJar = addAnnotationProcessor("lombok-1.18.30.jar").single()
                evaluateProject()

                val gwtTask = getGwtTasks().first()
                gwtTask.jvmArgs shouldContain "-javaagent:${lombokJar.absolutePath}=ECJ"
            }

            it("lombok 의존성이 없을 때는 javaagent를 설정하지 않아야 한다") {
                setupGwtProject()
                addAnnotationProcessor("other-processor.jar")
                evaluateProject()

                val gwtTask = getGwtTasks().first()
                gwtTask.jvmArgs.any { it.startsWith("-javaagent:") } shouldBe false
            }

            it("annotationProcessor 설정이 비어있을 때는 javaagent를 설정하지 않아야 한다") {
                setupGwtProject()
                evaluateProject()

                val gwtTask = getGwtTasks().first()
                gwtTask.jvmArgs.any { it.startsWith("-javaagent:") } shouldBe false
            }

            it("여러 개의 lombok 의존성이 있을 때 첫 번째 것을 javaagent로 설정해야 한다") {
                setupGwtProject()
                val lombokJars = addAnnotationProcessor("lombok-1.18.30.jar", "lombok-1.18.32.jar")
                evaluateProject()

                val gwtTask = getGwtTasks().first()
                val hasLombokAgent = gwtTask.jvmArgs.any { arg ->
                    arg.startsWith("-javaagent:") && lombokJars.any { jar -> arg.contains(jar.name) }
                }
                hasLombokAgent shouldBe true
            }

            it("annotationProcessor 설정이 존재하지 않으면(null) javaagent를 설정하지 않아야 한다") {
                setupGwtProject()
                // annotationProcessor 설정을 제거하여 findByName이 null을 반환하도록 한다.
                project.configurations.remove(project.configurations.getByName("annotationProcessor"))

                evaluateProject()

                // annotationProcessorConfig가 null이므로 lombok은 null이 되고, jvmArgs는 설정되지 않아야 한다.
                val gwtTask = getGwtTasks().first()
                gwtTask.jvmArgs.any { it.startsWith("-javaagent:") } shouldBe false
            }

            it("annotationProcessor 설정이 없어도 실패하지 않아야 한다") {
                project.pluginManager.apply("java")
                project.pluginManager.apply(GwtLombokPlugin::class.java)
                project.configurations.remove(project.configurations.getByName("annotationProcessor"))

                evaluateProject()
                // 예외가 발생하지 않으면 성공
            }

            it("GWT 태스크가 없어도 실패하지 않아야 한다") {
                project.pluginManager.apply("java")
                project.pluginManager.apply(GwtLombokPlugin::class.java)
                addAnnotationProcessor("lombok-1.18.30.jar")

                evaluateProject()

                getGwtTasks().isEmpty() shouldBe true
            }
        }
    }
}) {
    companion object {
        private fun File.toPathString(): String = this.path.replace(File.separatorChar, '/')
    }
}