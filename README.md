# GWT Gradle 플러그인 확장

Lombok 지원 및 테스트 컴파일 기능을 갖춘 GWT(Google Web Toolkit) 개발을 위한 향상된 Gradle 플러그인입니다.

## 개요

이 프로젝트는 기본 [org.docstr.gwt](https://github.com/docstr/gwt-gradle-plugin) 플러그인을 확장하여 추가 기능을 제공하는 Gradle 플러그인 세트입니다:

- **Lombok 통합**: GWT 프로젝트를 위한 원활한 Lombok 어노테이션 처리
- **테스트 컴파일**: 프로덕션 코드와 함께 GWT 테스트 모듈 컴파일 및 실행
- **개발 모드**: 테스트 소스가 포함된 향상된 개발 모드
- **자동 설정**: 합리적인 기본값으로 간편한 설정

## 설치

### Gradle (Kotlin DSL)

```kotlin
plugins {
    id("dev.sayaya.gwt") version "2.2.7"
}
```

### Gradle (Groovy DSL)

```groovy
plugins {
    id 'dev.sayaya.gwt' version '2.2.7'
}
```

## 사용 가능한 플러그인

### 1. `dev.sayaya.gwt` (메인 플러그인)

`gwt.lombok`과 `gwt.test` 플러그인을 모두 적용하는 편의 플러그인입니다.

```kotlin
plugins {
    id("dev.sayaya.gwt") version "2.2.7"
}
dependencies {
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}
```


**기능:**
- `gwtTestCompile` 태스크 등록
- 테스트 소스를 포함하도록 `gwtDevMode` 설정
- 태스크 의존성 자동 설정
- 소스셋에 생성된 소스 디렉토리 추가
- ECJ와 함께 Lombok java agent를 사용하도록 GWT 컴파일러 설정
- `annotationProcessor` 설정에서 Lombok 자동 감지
- 기본적으로 UTF-8 인코딩 사용

## 설정

플러그인은 기본 GWT 플러그인 설정을 확장합니다. `gwt` 블록에서 GWT 설정을 구성하세요:

```kotlin
tasks {
    gwt {
        gwtVersion = "2.12.2"
        modules = listOf("com.example.App")
        war = file("src/main/webapp")
        devMode {
            modules = listOf(
                "com.example.Test"
            )
            war = file("src/test/webapp")
        }
    }
}
```

## 태스크

### `gwtTestCompile`

main과 test 소스를 모두 포함하여 GWT 테스트 모듈을 컴파일합니다.

```bash
./gradlew gwtTestCompile
```

### `gwtDevMode`

테스트 소스를 사용할 수 있는 GWT 개발 모드를 시작합니다.

```bash
./gradlew gwtDevMode
```

### `test`

테스트를 실행합니다 (자동으로 `gwtTestCompile`에 의존).

```bash
./gradlew test
```

## 사용 예시

### 기본 설정

```kotlin
plugins {
    kotlin("jvm") version "2.2.21"
    id("dev.sayaya.gwt") version "2.2.7"
    id("war")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.gwtproject:gwt-user:2.12.2")
    compileOnly("org.gwtproject:gwt-dev:2.12.2")

    // Lombok 지원
    implementation("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    // 테스트
    testImplementation("io.kotest:kotest-runner-junit5:6.0.4")
}

tasks {
    gwt {
        gwtVersion = "2.12.2"
        modules = listOf("com.example.App")
        war = file("src/main/webapp")
        devMode {
            modules = listOf(
                "com.example.Test"
            )
            war = file("src/test/webapp")
        }
    }
}
```

## 모듈 구조

테스트가 포함된 일반적인 GWT 모듈 구조:

```
src/
├── main/
│   ├── java/
│   │   └── com/example/
│   │       ├── App.gwt.xml          # 메인 모듈
│   │       └── client/
│   │           └── App.java
│   └── webapp/
│       └── index.html
└── test/
    ├── java/
    │   └── com/example/
    │       ├── Test.gwt.xml         # 테스트 모듈
    │       └── client/
    │           └── AppTest.java
    └── webapp/
        └── Test.html                # 없으면 자동 생성
```

### 모듈 XML 예제

**src/main/java/com/example/App.gwt.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<module rename-to="app">
    <inherits name="com.google.gwt.user.User"/>
    <entry-point class="com.example.client.App"/>
    <source path="client"/>
</module>
```

**src/test/java/com/example/Test.gwt.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<module rename-to="test">
    <inherits name="com.example.App"/>
    <source path="client"/>
</module>
```

## HTML 런처 자동 생성

플러그인은 테스트 모듈에 대한 HTML 런처 파일이 없으면 자동으로 생성합니다:

```html
<!DOCTYPE html>
<html>
<head>
    <title>TestModule Test</title>
    <script type="text/javascript" src="TestModule/TestModule.nocache.js"></script>
</head>
<body></body>
</html>
```

## 요구사항

- Gradle 8.0+
- Kotlin 1.9+ (Kotlin DSL용)
- Java 11+
- GWT 2.10.0+

## 배포

이 플러그인은 GitHub Packages에 배포됩니다:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/sayaya1090/maven")
        credentials {
            username = project.findProperty("github_username") as String?
            password = project.findProperty("github_password") as String?
        }
    }
}
```

**보안 참고사항:** 자격 증명을 버전 관리에 커밋하지 마세요. 다음을 사용하세요:
- `gradle.properties` (git에서 제외)
- 환경 변수
- `~/.gradle/gradle.properties`의 Gradle 속성 파일

## 아키텍처

### 플러그인 계층 구조

```
dev.sayaya.gwt (GwtPlugin)
├── dev.sayaya.gwt.lombok (GwtLombokPlugin)
│   └── Lombok 어노테이션 처리 설정
└── dev.sayaya.gwt.test (GwtTestPlugin)
    ├── org.docstr.gwt (기본 GWT 플러그인)
    └── GwtTestCompileTask 등록
```

### 태스크 의존성

```
war
└── gwtCompile
    └── mustRunAfter(test)
        └── gwtTestCompile
            └── processTestResources
```

## 문제 해결

### 모듈 XML을 찾을 수 없음

**오류:** `Cannot find GWT module XML file: com/example/Test.gwt.xml`

**해결책:** 모듈 XML 파일이 소스 디렉토리 중 하나에 존재하고 모듈 이름과 정확히 일치하는지 확인하세요.

### Lombok이 작동하지 않음

**오류:** GWT 컴파일에서 Lombok 어노테이션이 처리되지 않음

**해결책:** 이 플러그인은 `annotationProcessor` 설정에 Lombok 의존성이 추가되면 자동으로 GWT 컴파일러에 필요한 `-javaagent` 설정을 추가합니다. 따라서 수동으로 `jvmArgs`나 `extraJvmArgs`를 설정할 필요가 없습니다.

다음 사항을 확인하세요:
1. `build.gradle.kts`의 `dependencies` 블록에 Lombok이 `annotationProcessor`로 올바르게 추가되었는지 확인하세요.
   ```kotlin
   dependencies {
       // ...
       annotationProcessor("org.projectlombok:lombok:...")
   }
   ```
2. `dev.sayaya.gwt.lombok` 플러그인 또는 이를 포함하는 `dev.sayaya.gwt` 플러그인이 적용되었는지 확인하세요.

플러그인이 자동으로 모든 것을 처리하므로, 위 설정이 올바르다면 Lombok이 작동해야 합니다.

### 컴파일 중 메모리 부족

**오류:** `java.lang.OutOfMemoryError: Java heap space`

**해결책:** GWT 설정에서 힙 크기를 늘리세요:

```kotlin
tasks.gwt {
    minHeapSize = "2048M"
    maxHeapSize = "4096M"
}
```

## 라이선스

이 프로젝트는 프로젝트의 라이선스 파일에 명시된 조건에 따라 사용할 수 있습니다.

## 관련 프로젝트

- [gwt-gradle-plugin](https://github.com/docstr/gwt-gradle-plugin) - 기본 GWT Gradle 플러그인
- [GWT Project](https://www.gwtproject.org/) - Google Web Toolkit
- [Lombok](https://projectlombok.org/) - Java 어노테이션 프로세서

## 변경 이력

### 2.2.7
- Lombok 및 테스트 컴파일 지원과 함께 초기 릴리스
- 모든 public API에 대한 KDoc 문서화
- 포괄적인 테스트 커버리지
- 자동 HTML 런처 생성
- 헬퍼 메서드를 사용한 리팩토링된 설정