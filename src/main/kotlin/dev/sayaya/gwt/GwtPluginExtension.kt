package dev.sayaya.gwt

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * 사용자 빌드 스크립트의 `gwt { ... }` 블록에 대응하는 확장 객체입니다.
 *
 * ## 역할
 * - GWT 버전과 메인 모듈 컴파일(`gwtCompile`) 옵션을 정의합니다.
 * - 하위 블록 [compiler]로 GWT 컴파일러 전용 옵션(`strict`, `optimize` 등)을 정의합니다.
 * - 하위 블록 [devMode]로 개발 모드·테스트 모듈 컴파일 옵션을 정의합니다.
 * - [GwtTestPlugin]이 `test { webPort = ... }` 하위 블록([GwtTestTaskExtension])을 추가로 붙입니다(ExtensionAware).
 *
 * ## 주요 의존관계
 * - [GwtBasePlugin]이 `gwt` 이름으로 생성하고 기본값(convention)을 설정합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * gwt {
 *     gwtVersion = "2.13.1"
 *     modules = listOf("com.example.App")
 *     war = file("src/main/webapp")
 *     compiler { strict = true }
 *     devMode {
 *         modules = listOf("com.example.Test")
 *         war = file("src/test/webapp")
 *     }
 * }
 * ```
 *
 * ## 주의사항
 * - 이전 기반 플러그인(`org.docstr.gwt`)의 `superDev { }`, `gwtTest { }` 블록과
 *   `gwtSuperDev` 태스크는 제공하지 않습니다.
 */
abstract class GwtPluginExtension @Inject constructor(objects: ObjectFactory) : GwtOptions() {
    /** 사용할 GWT 버전. `org.gwtproject:gwt` BOM 버전으로 사용됩니다. 기본값 [GwtBasePlugin.DEFAULT_GWT_VERSION]. */
    abstract val gwtVersion: Property<String>

    /** `true`이면 `gwt-servlet` 대신 `gwt-servlet-jakarta`를 의존성으로 추가합니다. 기본값 `false`. */
    abstract val jakarta: Property<Boolean>

    /** GWT 컴파일러 전용 옵션 (`gwtCompile`, `gwtTestCompile`) */
    val compiler: GwtCompilerOptions = objects.newInstance(GwtCompilerOptions::class.java)

    /** `compiler { ... }` 블록을 구성합니다. */
    fun compiler(action: Action<in GwtCompilerOptions>) = action.execute(compiler)

    /** 개발 모드 및 테스트 모듈 컴파일 옵션 */
    val devMode: GwtDevModeOptions = objects.newInstance(GwtDevModeOptions::class.java)

    /** `devMode { ... }` 블록을 구성합니다. */
    fun devMode(action: Action<in GwtDevModeOptions>) = action.execute(devMode)
}
