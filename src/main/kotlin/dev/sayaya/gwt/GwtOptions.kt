package dev.sayaya.gwt

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * GWT 컴파일러와 개발 모드가 공통으로 사용하는 옵션 묶음입니다.
 *
 * ## 역할
 * - `gwt { ... }` 블록([GwtPluginExtension])과 `gwt { devMode { ... } }` 블록([GwtDevModeOptions])의 공통 상위 타입입니다.
 * - 각 프로퍼티는 GWT 커맨드라인 인자(`-war`, `-sourceLevel`, `-style` 등)에 1:1로 대응하며,
 *   [GwtBaseTask.configureFrom]을 통해 실행 태스크로 지연(lazy) 연결됩니다.
 *
 * ## 주의사항
 * - Gradle Managed Type이므로 직접 생성하지 말고 `ObjectFactory.newInstance` 또는 extension 생성을 통해 인스턴스화해야 합니다.
 * - 값을 설정하지 않은 프로퍼티는 해당 인자를 GWT에 전달하지 않습니다(GWT 기본값 사용).
 */
abstract class GwtOptions {
    /** GWT 프로세스의 최소 힙 크기 (예: `256M`). 미설정 시 `256M`. */
    abstract val minHeapSize: Property<String>

    /** GWT 프로세스의 최대 힙 크기 (예: `1024M`). 미설정 시 `512M`. */
    abstract val maxHeapSize: Property<String>

    /** `-logLevel`: ERROR, WARN, INFO, TRACE, DEBUG, SPAM, ALL */
    abstract val logLevel: Property<String>

    /** `-workDir`: 컴파일러 작업 디렉토리 */
    abstract val workDir: DirectoryProperty

    /** `-gen`: 생성된 소스 디버그 출력 디렉토리 */
    abstract val gen: DirectoryProperty

    /** `-war`: 컴파일 결과(JS, 리소스)가 기록되는 디렉토리 */
    abstract val war: DirectoryProperty

    /** `-deploy`: 서버 배포용 산출물 디렉토리 */
    abstract val deploy: DirectoryProperty

    /** `-extra`: 배포되지 않는 부가 산출물 디렉토리 */
    abstract val extra: DirectoryProperty

    /** `-Dgwt.persistentunitcachedir`: GWT 유닛 캐시 디렉토리 */
    abstract val cacheDir: DirectoryProperty

    /** `-generateJsInteropExports` / `-nogenerateJsInteropExports` */
    abstract val generateJsInteropExports: Property<Boolean>

    /** `-includeJsInteropExports` (항목마다 반복) */
    abstract val includeJsInteropExports: ListProperty<String>

    /** `-excludeJsInteropExports` (항목마다 반복) */
    abstract val excludeJsInteropExports: ListProperty<String>

    /** `-XmethodNameDisplayMode`: NONE, ONLY_METHOD_NAME, ABBREVIATED, FULL */
    abstract val methodNameDisplayMode: Property<String>

    /** `-sourceLevel`: Java 소스 레벨 (예: `auto`, `17`) */
    abstract val sourceLevel: Property<String>

    /** `-incremental` / `-noincremental` */
    abstract val incremental: Property<Boolean>

    /** `-style`: OBFUSCATED, PRETTY, DETAILED */
    abstract val style: Property<String>

    /** `-failOnError` / `-nofailOnError` */
    abstract val failOnError: Property<Boolean>

    /** `-setProperty name=value` (항목마다 반복) */
    abstract val setProperty: ListProperty<String>

    /** 컴파일/실행할 GWT 모듈 이름 목록 (예: `com.example.App`). 필수. */
    abstract val modules: ListProperty<String>

    /** GWT 클래스패스에 추가할 소스/리소스/클래스 디렉토리 */
    abstract val extraSourceDirs: ConfigurableFileCollection
}

/**
 * `gwt { devMode { ... } }` 블록의 옵션입니다.
 *
 * ## 역할
 * - `gwtDevMode` 태스크와 테스트 모듈 컴파일(`gwtTestCompile`) 태스크의 설정 원천입니다.
 * - [GwtOptions]의 공통 옵션은 값을 지정하지 않으면 [GwtBasePlugin]이 설정한 convention에 따라
 *   상위 `gwt { ... }` 블록의 값을 그대로 이어받습니다. 단, [extraSourceDirs]는 상속하지 않고 태스크에서 합산됩니다.
 * - 아래 DevMode 전용 옵션은 `com.google.gwt.dev.DevMode` 인자에 대응합니다.
 */
abstract class GwtDevModeOptions : GwtOptions() {
    /** `-startServer` / `-nostartServer` */
    abstract val startServer: Property<Boolean>

    /** `-port`: 내장 웹 서버 포트 */
    abstract val port: Property<Int>

    /** `-logdir`: 로그 출력 디렉토리 */
    abstract val logdir: DirectoryProperty

    /** `-bindAddress`: 바인딩 주소 */
    abstract val bindAddress: Property<String>

    /** `-codeServerPort`: 코드 서버 포트 */
    abstract val codeServerPort: Property<Int>

    /** `-superDevMode` / `-nosuperDevMode` */
    abstract val superDevMode: Property<Boolean>

    /** `-server`: 사용할 서블릿 컨테이너 런처 */
    abstract val server: Property<String>

    /** `-startupUrl`: 시작 URL */
    abstract val startupUrl: Property<String>

    /** `-modulePathPrefix`: 모듈 출력 경로 접두사 */
    abstract val modulePathPrefix: Property<String>
}
