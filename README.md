# GWT Gradle 플러그인

이 프로젝트는 Gradle 환경에서 GWT(Google Web Toolkit) 개발, 특히 테스트와 Lombok 사용을 간소화하기 위한 플러그인입니다. 기존의 `org.docstr.gwt` 플러그인을 확장하여 GWT 테스트에 필요한 반복적인 설정과 실행 과정을 자동화합니다.

## ✨ 주요 기능

### Gradle 플러그인
- **Lombok 완벽 지원**: GWT 컴파일러가 Lombok 어노테이션을 처리할 수 있도록 `-javaagent`를 자동으로 설정합니다.
- **테스트용 웹 서버 자동 관리**: GWT 테스트 실행 시 Ktor 기반의 내장 웹 서버를 자동으로 시작하고, 테스트가 끝나면(성공/실패 무관) 반드시 종료하여 리소스를 안전하게 정리합니다.
- **HTML 호스트 파일 자동 생성**: 각 GWT 테스트 모듈에 필요한 HTML 파일을 자동으로 생성하여, 수동으로 파일을 관리할 필요가 없습니다. (`rename-to` 속성 포함)
- **간소화된 테스트 설정**: 테스트 태스크에 `gwt` 확장을 통해 웹서버 포트 등 GWT 테스트 관련 설정을 직관적으로 관리할 수 있습니다.
- **원활한 태스크 통합**: Gradle의 `test` 태스크를 실행하기만 하면 GWT 컴파일, 서버 실행, 테스트, 서버 종료까지 모든 과정이 자동으로 처리됩니다.

### kotest+selenium 테스트 라이브러리 (`gwt-test`)
- **GWT 전용 테스트 베이스**: Kotest BehaviorSpec을 확장한 `GwtTestSpec` 제공
- **자동 ChromeDriver 설정**: Headless 모드, 브라우저 로깅 자동 활성화
- **콘솔 로그 검증**: `shouldContainLog`, `shouldNotContainLog` 등 편리한 매처 제공
- **자동 리소스 정리**: 테스트 종료 시 WebDriver 자동 종료

## 🚀 시작하기

### 1. Gradle 플러그인 설정

#### Kotlin DSL

`build.gradle.kts` 파일의 `plugins` 블록에 플러그인을 추가합니다.

```kotlin
plugins {
    id("dev.sayaya.gwt") version "2.2.7"
}
```

#### Groovy DSL

```groovy
plugins {
    id 'dev.sayaya.gwt' version '2.2.7'
}
```

### 2. kotest+selenium 테스트 라이브러리 추가 (선택사항)

kotest+selenium을 사용한 브라우저 테스트가 필요한 경우:

```kotlin
dependencies {
    testImplementation("dev.sayaya:gwt-test:2.2.7")
}
```

## ⚙️ 설정

플러그인은 기본 GWT 플러그인 설정을 확장합니다. `gwt` 블록에서 GWT 설정을 구성하세요:

```kotlin
gwt {
    gwtVersion = "2.12.2"
    modules = listOf("com.example.App")
    war = file("src/main/webapp")
    devMode {
        modules = listOf("com.example.Test")
    }
}

tasks.withType<Test> {
    extensions.configure<GwtTestTaskExtension>("gwt") {
        webPort.set(9876) // 웹서버 포트 (기본값: 9876)
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

## 📖 사용 예시

### 기본 플러그인 설정

```kotlin
plugins {
    kotlin("jvm") version "2.1.0"
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
    implementation("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    // 테스트
    testImplementation("io.kotest:kotest-runner-junit5:6.0.4")
}

gwt {
    gwtVersion = "2.12.2"
    modules = listOf("com.example.App")
    war = file("src/main/webapp")
    devMode {
        modules = listOf("com.example.Test")
    }
}

tasks.test {
    extensions.configure<GwtTestTaskExtension>("gwt") {
        webPort.set(9876)
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
    └── resources/                    # 또는 webapp/
        └── Test.html                # 없으면 war 디렉토리에 자동 생성
```

**참고:** HTML 파일은 `gwt.war`로 설정된 디렉토리에 생성됩니다. 기본적으로 `src/main/webapp`이며, 없을 경우 자동으로 생성됩니다.

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

`gwtTestCompile` 태스크는 각 GWT 모듈에 대한 HTML 파일이 `war` 디렉토리에 없으면 자동으로 생성합니다. 모듈의 `rename-to` 속성을 읽어 파일명을 결정합니다.

**예시:** `Test.gwt.xml`에 `rename-to="test"` 속성이 있으면:

```html
<!DOCTYPE html>
<html>
<head>
    <title>test Test</title>
    <script type="text/javascript" src="test/test.nocache.js"></script>
</head>
<body></body>
</html>
```

**생성 위치:** `gwt.war` 디렉토리 (기본값: `src/main/webapp`)

## kotest 테스트 작성하기

`gwt-test` 라이브러리를 사용하면 kotest+selenium 기반 브라우저 테스트를 간편하게 작성할 수 있습니다.

### 기본 사용법

```kotlin
import dev.sayaya.gwt.test.GwtTestSpec

class MenuTest : GwtTestSpec({
    htmlPath = "src/test/webapp/test.html"  // 테스트할 HTML 파일
    headless = true                          // headless 모드 (기본값: true)

    Given("메뉴가 로드되면") {
        When("메뉴 버튼을 클릭하면") {
            driver.findElement(By.id("menu-button")).click()

            Then("메뉴가 표시되어야 한다") {
                driver shouldContainLog "Menu opened"
            }
        }
    }

    Given("잘못된 입력이 들어오면") {
        When("에러가 발생하면") {
            Then("에러 로그가 출력되지 않아야 한다") {
                driver shouldNotContainLog "ERROR"
            }
        }
    }
})
```

### 제공되는 헬퍼 메서드

#### 콘솔 로그 검증

```kotlin
// 로그에 특정 텍스트가 포함되어 있는지 확인 (검증 후 자동 클리어)
driver shouldContainLog "Expected message"

// 로그에 특정 텍스트가 없는지 확인 (검증 후 자동 클리어)
driver shouldNotContainLog "Error message"

// 모든 콘솔 로그 가져오기
val logs: List<String> = driver.getConsoleLogs()

// 콘솔 로그 수동 클리어
driver.clearConsoleLogs()
```

#### 설정 옵션

```kotlin
class MyTest : GwtTestSpec({
    htmlPath = "src/test/webapp/test.html"  // HTML 파일 경로
    headless = false                        // 브라우저 UI 표시

    // 테스트 로직...
})
```

### 실제 사용 예시

```kotlin
import dev.sayaya.gwt.test.GwtTestSpec
import org.openqa.selenium.By

class UserInterfaceTest : GwtTestSpec({
    htmlPath = "src/test/webapp/test.html"

    Given("사용자 인터페이스가 로드되면") {
        When("로그인 버튼을 클릭하면") {
            val loginButton = driver.findElement(By.id("login-btn"))
            loginButton.click()

            Then("로그인 다이얼로그가 표시되어야 한다") {
                driver shouldContainLog "Login dialog opened"
            }
        }

        When("사용자 이름을 입력하면") {
            driver.findElement(By.id("username")).sendKeys("testuser")

            Then("입력 검증 로그가 출력되어야 한다") {
                driver shouldContainLog "Username validated"
            }
        }
    }
})
```

## 요구사항

### Gradle 플러그인
- Gradle 8.0+
- Kotlin 1.9+ (Kotlin DSL용)
- Java 11+
- GWT 2.10.0+

### kotest+selenium 테스트 라이브러리
- ChromeDriver (자동 다운로드됨)
- Kotest 6.0+
- Selenium 4.27+

## 📦 배포

이 플러그인은 GitHub Packages에 배포됩니다. 플러그인을 사용하려면 저장소 설정이 필요합니다:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://maven.pkg.github.com/sayaya1090/maven")
            credentials {
                username = project.findProperty("github_username") as String? ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("github_password") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

**보안 참고사항:** 자격 증명을 버전 관리에 커밋하지 마세요. 다음 방법 중 하나를 사용하세요:
- `~/.gradle/gradle.properties`에 저장 (권장)
- 환경 변수 사용
- 프로젝트 루트의 `gradle.properties` (반드시 `.gitignore`에 추가)

## 🏗️ 아키텍처

### 플러그인 계층 구조

```
dev.sayaya.gwt (GwtPlugin)
├── dev.sayaya.gwt.lombok (GwtLombokPlugin)
│   └── Lombok 어노테이션 처리를 위한 -javaagent 자동 설정
└── dev.sayaya.gwt.test (GwtTestPlugin)
    ├── org.docstr.gwt (기본 GWT 플러그인 적용)
    ├── GwtTestCompileTask 등록
    └── 웹 서버 자동 관리 (테스트 시)
```

### 태스크 의존성 흐름

```
test
├── dependsOn: gwtTest
│   ├── dependsOn: gwtTestCompile
│   │   └── dependsOn: processTestResources
│   ├── dependsOn: openWebServer
│   └── finalizedBy: closeWebServer

war
└── dependsOn: test
```

**태스크 설명:**
- `gwtTestCompile`: GWT 테스트 모듈 컴파일 및 HTML 파일 생성
- `openWebServer`: Ktor 기반 정적 파일 웹서버 시작
- `closeWebServer`: 웹서버 종료
- `gwtTest`: 웹서버 시작, 테스트 컴파일, 종료를 통합한 태스크

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
gwt {
    minHeapSize = "2048M"
    maxHeapSize = "4096M"
}
```

### 웹 서버가 종료되지 않음

**증상:** 테스트 후에도 포트가 계속 사용 중

**해결책:**
1. 플러그인이 자동으로 서버를 관리하므로 수동으로 시작/종료하지 마세요
2. 테스트가 실패해도 `finalizedBy`로 서버가 종료됩니다
3. 수동으로 종료하려면: `./gradlew closeWebServer`

### 웹서버 포트 변경

**기본 포트:** 9876

**변경 방법:**
```kotlin

tasks.test {
    gwt {
        webPort.set(8080) // 원하는 포트로 변경
        codePort.set(8081)
    }
}
```

## 라이선스

이 프로젝트는 프로젝트의 라이선스 파일에 명시된 조건에 따라 사용할 수 있습니다.

## 관련 프로젝트

- [gwt-gradle-plugin](https://github.com/docstr/gwt-gradle-plugin) - 기본 GWT Gradle 플러그인
- [GWT Project](https://www.gwtproject.org/) - Google Web Toolkit
- [Lombok](https://projectlombok.org/) - Java 어노테이션 프로세서

## 📝 변경 이력

### 2.2.7 (최신)
- ✨ GWT 테스트를 위한 내장 웹 서버 자동 관리 기능 추가
- ✨ 테스트용 HTML 호스트 파일 자동 생성 (`rename-to` 속성 지원)
- ✨ Lombok Java Agent 자동 설정 기능 추가
- 📚 모든 public API에 대한 KDoc 문서화 완료
- ✅ 포괄적인 테스트 커버리지 달성
- 🔧 설정 헬퍼 메서드를 사용한 리팩토링