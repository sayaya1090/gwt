# GWT 테스트 실행 시퀀스 분석

이 문서는 Gradle 빌드 라이프사이클 내에서 GWT 테스트가 실행되는 전체 프로세스를 설명합니다.

## 1. 테스트 실행 워크플로우 (Sequence Diagram)

```mermaid
sequenceDiagram
    participant User as 개발자 (./gradlew test)
    participant Gradle as Gradle Lifecycle
    participant Task1 as gwtGenerateTestHtml
    participant Task2 as gwtTestCompile
    participant Service as WebServerService (Ktor)
    participant TestTask as Test Task (Kotest)
    participant Spec as GwtTestSpec (Playwright)
    participant Browser as Chromium Browser

    User->>Gradle: 빌드 시작
    
    rect rgb(240, 240, 240)
    Note over Task1, Task2: GWT 결과물 준비 단계
    Gradle->>Task1: gwtGenerateTestHtml 실행
    Task1-->>Gradle: .html 호스트 파일 생성 완료
    
    Gradle->>Task2: gwtTestCompile 실행
    Task2-->>Gradle: GWT JS 컴파일 완료 (WAR 디렉토리)
    end

    rect rgb(230, 240, 255)
    Note over Service, TestTask: 테스트 및 서버 구동 단계
    Gradle->>Service: WebServerService 초기화 (Shared Build Service)
    Service->>Service: Ktor 서버 시작 (지정된 WAR 디렉토리 서빙)
    
    Gradle->>TestTask: 테스트 태스크 실행
    TestTask->>Spec: GwtTestSpec 초기화
    
    Spec->>Browser: Playwright 브라우저 실행
    Spec->>Browser: page.navigate(URL) 요청
    Browser->>Service: HTML 및 컴파일된 JS 요청
    Service-->>Browser: 리소스 응답
    
    Note over Spec, Browser: 브라우저 내에서 GWT 로직 실행 및 콘솔 로그 수집
    
    Spec->>Browser: 테스트 완료 후 브라우저 종료
    end

    Gradle-->>Service: 빌드 종료 시 stop() 호출
    Service->>Service: Ktor 서버 셧다운
    Gradle-->>User: 최종 빌드 결과 보고
```

## 2. 주요 단계별 상세 분석

### 2.1 사전 준비 단계 (Preparation)
- **`gwtGenerateTestHtml`**: GWT 모듈을 브라우저에서 실행하기 위해 필요한 기본 HTML 구조를 자동으로 생성합니다.
- **`gwtTestCompile`**: GWT의 Java 소스 코드를 브라우저가 이해할 수 있는 JavaScript로 변환합니다. 이때 `devMode` 설정을 활용하여 컴파일 속도를 최적화합니다.

### 2.2 인프라 구성 (Infrastructure)
- **`WebServerService`**: 
    - Gradle의 `BuildService` 인터페이스를 구현하여 빌드 전체 과정에서 단 하나의 서버 인스턴스만 존재하도록 보장합니다.
    - 테스트 태스크가 여러 개라도 서버는 한 번만 뜨고 공유됩니다.
    - `contentRoot`는 GWT 컴파일 결과가 담긴 WAR 디렉토리로 설정됩니다.

### 2.3 테스트 실행 (Execution)
- **`GwtTestSpec`**: 
    - `beforeSpec` 단계에서 Playwright를 초기화하고 브라우저를 띄웁니다.
    - `GwtHtml` 어노테이션에 정의된 경로를 통해 서버의 특정 페이지로 접속합니다.
    - `page.onConsoleMessage` 리스너를 통해 브라우저 내부에서 발생하는 로그를 수집하여 Kotest의 Assertion 로직으로 전달합니다.

### 2.4 리소스 정리 (Cleanup)
- 테스트가 종료되면 `afterSpec`에서 브라우저를 닫습니다.
- 전체 Gradle 빌드가 완료되면 Gradle이 `WebServerService`의 `stop()`을 호출하여 Ktor 서버를 안전하게 종료합니다.
