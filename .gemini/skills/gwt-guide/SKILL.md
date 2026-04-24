# sayaya-gwt 플러그인 개발자 가이드

이 스킬은 `sayaya-gwt` Gradle 플러그인 프로젝트의 구조, 내부 동작 원리 및 개발 표준을 정의합니다. 이 플러그인은 `org.docstr.gwt`를 확장하여 Lombok 통합, 테스트용 내장 웹서버, HTML 호스트 자동 생성 기능을 제공합니다.

## 기술 스택 및 버전

| 라이브러리 | 버전 | 역할 |
|-----------|------|------|
| Gradle | 8.x+ | 빌드 시스템 |
| Kotlin | 2.3.20 | 플러그인 구현 언어 |
| org.docstr.gwt | 2.2.9 | 기반 GWT Gradle 플러그인 |
| Ktor | 3.4.2 | 테스트용 내장 웹서버 (Netty) |
| Playwright | 1.59.0 | 브라우저 자동화 테스트 |
| Kotest | 6.1.10 | 테스트 프레임워크 |
| sayaya-gwt | 2.2.9.1 | 현재 프로젝트 버전 |

## 플러그인 구성 모듈

### 1. GwtPlugin (Entry Point)
- `dev.sayaya.gwt` 아이디로 등록됩니다.
- `GwtLombokPlugin`과 `GwtTestPlugin`을 내부적으로 적용하는 통합 플러그인입니다.

### 2. GwtLombokPlugin
- GWT 컴파일 시 Lombok 어노테이션 프로세싱이 정상 작동하도록 Java Agent를 주입합니다.
- GWT가 바이트코드가 아닌 소스코드를 트랜스파일하므로, 컴파일 타임에 소스를 변조하는 Lombok 지원이 필수적입니다.

### 3. GwtTestPlugin & Infrastructure
- **WebServerService**: Gradle `BuildService`를 사용하여 테스트용 Ktor 웹서버의 생명주기를 관리합니다. 병렬 테스트 실행 시에도 고유한 포트를 할당받아 격리된 환경을 제공합니다.
- **GwtGenerateTestHtmlTask**: GWT 모듈 설정을 기반으로 테스트에 필요한 HTML 호스트 파일을 자동으로 생성합니다.
- **GwtTestCompileTask**: GWT 테스트 코드를 JavaScript로 컴파일합니다.
- **시스템 프로퍼티 연동**: 테스트 실행 시 `gwt.junit.remoteUrl` 프로퍼티를 통해 웹서버 URL을 테스트 코드(`GwtTestSpec`)에 전달합니다.

## 개발 가이드라인

### 테스트 인프라 유지보수 (`gwt-test` 라이브러리)
- `GwtTestSpec`은 Kotest의 `BehaviorSpec`을 확장합니다.
- `page shouldContainLog "text"`와 같은 커스텀 매처를 통해 브라우저 콘솔 로그를 검증합니다.
- `page.onConsoleMessage`를 통해 수집된 로그는 JSON 파싱을 시도하며, 실패 시 문자열로 처리합니다.

### 코드 작성 규칙
- **Lazy Configuration**: Gradle의 `Property`, `Provider` API를 사용하여 구성을 최대한 지연시키십시오.
- **Build Service 활용**: 외부 리소스(네트워크 포트 등)가 필요한 작업은 반드시 `BuildService`를 통해 관리하여 빌드 종료 시 리소스 누수를 방지하십시오.
- **로그 및 가독성**: `project.logger.lifecycle`을 사용하여 주요 설정 단계(예: Remote URL 설정)를 사용자에게 알리십시오.

## 테스트 작성 예시 (플러그인 기능 검증)
플러그인 자체의 동작을 테스트할 때는 `src/test/kotlin` 하위의 통합 테스트를 참조하십시오. 사용자의 프로젝트 환경을 시뮬레이션하기 위해 `resources/` 내에 예제 GWT 프로젝트 구조를 유지합니다.

