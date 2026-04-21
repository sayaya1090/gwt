# GWT Gradle Plugin Architecture

이 문서는 프로젝트의 전체적인 설계 철학, 레이어 구조 및 기술 스택을 설명합니다.

## 1. 시스템 아키텍처 개요 (High-Level Architecture)

본 플러그인은 GWT 개발 및 테스트를 위해 세 가지 핵심 레이어로 구성되어 있습니다. 각 레이어는 Gradle 빌드 수명 주기 내에서 유기적으로 결합되어 동작합니다.

### Layer 1: Gradle Integration Layer (빌드 자동화)
프로젝트의 진입점이며, Gradle 태스크와 설정을 관리합니다.
- **Plugins**: `GwtPlugin`, `GwtLombokPlugin`, `GwtTestPlugin`. 프로젝트 구성 및 기능 활성화.
- **Tasks**: `GwtGenerateTestHtmlTask`, `GwtTestCompileTask`. GWT 결과물을 생성하고 테스트 환경을 준비.

### Layer 2: Infrastructure Layer (실행 환경)
테스트 리소스를 제공하기 위한 런타임 인프라를 담당합니다.
- **Build Service**: `WebServerService`. Gradle의 Shared Build Service를 통해 빌드 수명 주기 동안 하나의 Ktor 서버 인스턴스를 유지 및 관리.
- **Server**: Ktor (Netty). GWT 컴파일된 JS와 HTML 파일을 서빙하는 경량 웹 서버.

### Layer 3: Testing Framework Layer (검증 도구)
사용자가 작성하는 테스트 코드와 실제 브라우저 사이의 가교 역할을 합니다.
- **Test Base**: `GwtTestSpec`. Kotest와 Playwright를 결합하여 브라우저 자동화 및 어설션 환경 제공.
- **Automation**: Playwright. Chromium 브라우저를 구동하여 실제 GWT 로직을 실행하고 결과를 수집.

---

## 2. 핵심 설계 원칙

### 2.1 리소스 효율성 (Shared Build Service)
여러 개의 테스트 태스크가 병렬로 실행되더라도, `WebServerService`는 단 하나의 서버 인스턴스만 유지합니다. 이를 통해 포트 충돌을 방지하고 메모리 사용량을 최적화하며, 빌드 종료 시 리소스를 안전하게 해제합니다.

### 2.2 테스트 격리 및 자동화 (Task Dependency)
사용자가 `./gradlew test`를 호출하면, 플러그인은 태스크 의존성(dependsOn)을 통해 HTML 생성 및 GWT 컴파일이 선행되도록 보장합니다. 개발자는 복잡한 준비 과정 없이 테스트 코드 작성에만 집중할 수 있습니다.

### 2.3 투명한 로깅 (Console Interception)
브라우저 내부에서 발생하는 GWT 로그를 Playwright의 리스너를 통해 캡처하여 테스트 결과 리포트에 투명하게 노출합니다. 이는 브라우저 기반 테스트의 디버깅 난이도를 대폭 낮춰줍니다.

---

## 3. 기술 스택 (Tech Stack)

| 구분 | 기술 | 역할 |
| :--- | :--- | :--- |
| **Build Tool** | Gradle | 빌드 시스템 및 플러그인 프레임워크 |
| **Language** | Kotlin | 플러그인 로직 및 테스트 라이브러리 개발 |
| **Web Server** | Ktor (Netty) | 테스트용 정적 파일 서빙 |
| **Browser Automation** | Playwright | 브라우저 제어 및 로그 수집 |
| **Testing Spec** | Kotest | BDD 스타일 테스트 프레임워크 |
| **GWT Compiler** | org.docstr.gwt | 내부 GWT 컴파일 엔진 |

---

## 4. 상세 설계 문서 링크

- [**Use Cases**](./use-cases.md): 플러그인의 목적과 주요 활용 시나리오
- [**Class Diagram**](./class-diagram.md): 상세 클래스 구조 및 상속 관계
- [**Sequence Diagram**](./sequence-diagram.md): 테스트 실행 시의 시간적 흐름과 데이터 교환
