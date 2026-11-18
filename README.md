zjalt/# GWT Gradle Plugin

This project is a Gradle plugin designed to simplify GWT (Google Web Toolkit) development in a Gradle environment, with a special focus on testing and Lombok integration. It provides its own GWT compile/DevMode tasks (no dependency on any third-party GWT Gradle plugin) and automates repetitive setup and execution steps required for GWT tests.

## ✨ Key Features

### Gradle Plugin

- **Full Lombok Support**: Automatically configures the `-javaagent` option so that the GWT compiler can process Lombok annotations.
- **Automatic Test Web Server Management**: Uses **Gradle Build Service** to automatically manage the lifecycle of a Ktor-based embedded web server. It starts on demand when tests run and shuts down gracefully when the build finishes, ensuring efficient resource usage and no port conflicts.
- **Automatic HTML Host File Generation**: Automatically generates the HTML host file required for each GWT test module, including support for the `rename-to` attribute, so you don’t have to manage these files manually.
- **Seamless Task Integration**: Simply running the Gradle `test` task automatically takes care of GWT compilation, server startup, test execution, and server shutdown.

### kotest + playwright Test Library (`gwt-test`)

- **GWT-Specific Test Base**: Provides `GwtTestSpec`, which extends Kotest’s `BehaviorSpec`.
- **Automatic Playwright Setup**: Configures Playwright in headless mode with browser logging enabled by default.
- **Console Log Verification**: Convenient matchers such as `shouldContainLog` and `shouldNotContainLog`.
- **Automatic Resource Cleanup**: Automatically shuts down WebDriver when tests finish.

## 🚀 Getting Started

### 1. Configure the Gradle Plugin

#### Kotlin DSL

Add the plugin to the `plugins` block in your `build.gradle.kts`:

```kotlin
plugins {
    id("dev.sayaya.gwt") version "2.4.0"
}
```

#### Groovy DSL

```groovy
plugins {
    id 'dev.sayaya.gwt' version '2.4.0'
}
```

### 2. Add the kotest + playwright Test Library (Optional)

If you want to write browser tests using kotest + playwright:

```kotlin
dependencies {
    testImplementation("dev.sayaya:gwt-test:2.4.0")
}
```

## ⚙️ Configuration

> Since 2.3.0 this plugin no longer depends on `org.docstr.gwt` (or any other third-party GWT Gradle plugin). The `gwt { }` extension, the `gwtCompile` / `gwtDevMode` tasks and the GWT SDK dependencies are provided by the plugin itself. Do not apply `org.docstr.gwt` together with it.

Configure GWT in the `gwt` block. Options set at the top level apply to `gwtCompile`; options inside `devMode { }` apply to `gwtDevMode` and `gwtTestCompile`, and any option not set in `devMode` falls back to the top-level value:

```kotlin
gwt {
    gwtVersion = "2.13.0"
    modules = listOf("com.example.App")
    war = file("src/main/webapp")
    compiler {
        // GWT Compiler-only options, applied to gwtCompile and gwtTestCompile
        strict = true
    }
    devMode {
        modules = listOf("com.example.Test")
        // The web server will serve files from this directory during tests
        war = file("src/test/webapp") 
    }
}
```

## Tasks

### `gwtTestCompile`

Compiles GWT test modules including both `main` and `test` sources.

```shell script
./gradlew gwtTestCompile
```

### `gwtDevMode`

Starts GWT Dev Mode with access to test sources.

```shell script
./gradlew gwtDevMode
```

### `test`

Runs tests (automatically depends on `gwtTestCompile` and uses the Web Server Service).

```shell script
./gradlew test
```

## 📖 Usage Examples

### Basic Plugin Setup

```kotlin
plugins {
    kotlin("jvm") version "2.4.21"
    id("dev.sayaya.gwt") version "2.4.0"
    id("war")
}

repositories {
    mavenCentral()
}

dependencies {
    // GWT SDK (gwt-user, gwt-dev, gwt-codeserver, gwt-servlet) is added automatically
    // from gwt.gwtVersion — no need to declare it, and no third-party GWT plugin is required.

    // Lombok support
    implementation("org.projectlombok:lombok:1.18.48")
    annotationProcessor("org.projectlombok:lombok:1.18.48")

    // Tests
    testImplementation("io.kotest:kotest-runner-junit5:6.2.7")
}

gwt {
    gwtVersion = "2.13.0"
    modules = listOf("com.example.App")
    war = file("src/main/webapp")
    devMode {
        modules = listOf("com.example.Test")
    }
}
```

## Module Structure

A typical GWT module structure with tests:

```
src/
├── main/
│   ├── java/
│   │   └── com/example/
│   │       ├── App.gwt.xml          # Main module
│   │       └── client/
│   │           └── App.java
│   └── webapp/
│       └── index.html
└── test/
    ├── java/
    │   └── com/example/
    │       ├── Test.gwt.xml         # Test module
    │       └── client/
    │           └── AppTest.java
    └── resources/                    # or webapp/
        └── Test.html                # Auto-generated in war dir if missing
```

**Note:** HTML files are generated in the directory configured as `gwt.war`. By default, this is `src/main/webapp`, and it will be created automatically if it does not exist.

### Example Module XML

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

## Automatic HTML Launcher Generation

The `gwtTestCompile` task automatically generates an HTML file for each GWT module in the `war` directory if it does not already exist. The plugin reads the module’s `rename-to` attribute to determine the file name.

**Example:** If `Test.gwt.xml` has `rename-to="test"`:

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

**Generated location:** The directory configured as `gwt.war` (default: `src/main/webapp`).

## Writing kotest Tests

Using the `gwt-test` library, you can easily write browser tests with kotest + playwright.

### Basic Usage

```kotlin
import dev.sayaya.gwt.test.GwtTestSpec
import dev.sayaya.gwt.test.GwtHtml

@GwtHtml("test.html")  // Relative path from web server root
class MenuTest : GwtTestSpec({
    Given("the menu is loaded") {
        When("the menu button is clicked") {
            page.locator("#menu-button").click()

            Then("the menu should be visible") {
                page shouldContainLog "Menu opened"
            }
        }
    }
})
```

### Helper Methods

#### Console Log Verification

```kotlin
// Check if a specific text is contained in the logs (logs are automatically cleared afterward)
page shouldContainLog "Expected message"

// Check that a specific text is not present in the logs (logs are automatically cleared afterward)
page shouldNotContainLog "Error message"

// Retrieve all console logs
val logs: List<String> = page.getConsoleLogs()

// Manually clear console logs
page.clearConsoleLogs()
```

#### Configuration Options

```kotlin
// Multi-module project: assign specific ports to avoid conflicts
gwt {
    test {
        webPort.set(18080)
    }
}

@GwtHtml("test.html")  // HTML file name (relative path)
class MyTest : GwtTestSpec({
    // Test logic...
})
```

### Real-World Example

```kotlin
import dev.sayaya.gwt.test.GwtTestSpec
import dev.sayaya.gwt.test.GwtHtml

@GwtHtml("login.html")
class UserInterfaceTest : GwtTestSpec({
    Given("The page is loaded") {
        When("Login button is clicked") {
            page.locator("#login-btn").click()

            Then("Login dialog should be displayed") {
                page shouldContainLog "Login dialog opened"
            }
        }

        When("Username is entered") {
            page.locator("#username").fill("testuser")

            Then("A validation log should be printed") {
                page shouldContainLog "Username validated"
            }
        }
    }
})
```

## Requirements

### Gradle Plugin

- Gradle 9.3+ (verified with 9.3.0, 9.4.1, 9.8.2 and 9.9.1)
- Java 17+
- GWT 2.12+ (default `gwtVersion` 2.13.1; verified with 2.13.0 and 2.13.1)
- No third-party GWT Gradle plugin required (`org.docstr.gwt` is no longer used)

### kotest + playwright Test Library

- Kotest 6.0+
- Playwright (Java) 1.x
  
## 🏗️ Architecture

### Plugin Hierarchy

```
dev.sayaya.gwt (GwtPlugin)
├── dev.sayaya.gwt.lombok (GwtLombokPlugin)
│   └── Automatically sets -javaagent for Lombok annotation processing
└── dev.sayaya.gwt.test (GwtTestPlugin)
    ├── Applies GwtBasePlugin (gwt extension, gwtCompile/gwtDevMode tasks, GWT dependencies)
    ├── Registers GwtTestCompileTask
    └── Registers WebServerService (Build Service)
```

### Task Dependency Flow

```
test
├── uses: WebServerService (Build Service)
└── dependsOn: gwtTestCompile
    └── dependsOn: gwtGenerateTestHtml

gwtDevMode
└── dependsOn: gwtGenerateTestHtml

war
└── dependsOn: test
```


**Task Descriptions:**
- `gwtGenerateTestHtml`: Automatically generates HTML host files for GWT test modules.
- `gwtTestCompile`: Compiles GWT test modules (includes both main and test sources).
- `gwtDevMode`: Runs GWT Dev Mode including test sources.
- WebServerService: A shared build service that hosts the static files, registered once per project (`gwtWebServer-<name>` for the root project, `gwtWebServer:<path>` for subprojects) and shared by that project's `Test` tasks. It starts automatically when `test` begins, picks a free port unless `gwt { test { webPort } }` is set, and is closed (`close()`) when the build completes.

## Troubleshooting
### Cannot Find Module XML
**Error:** `Cannot find GWT module XML file: com/example/Test.gwt.xml`
**Solution:** Make sure the module XML file exists in one of the source directories and that its path exactly matches the module name.

### Lombok Not Working
**Symptom:** Lombok annotations are not processed during GWT compilation.
**Solution:** This plugin automatically adds the required `-javaagent` configuration to the GWT compiler when Lombok is present in the `annotationProcessor` configuration. You do not need to manually configure `jvmArgs` or `extraJvmArgs`.

Check the following:
1. Ensure Lombok is correctly added as an `annotationProcessor` dependency in `build.gradle.kts`:
```kotlin
dependencies {
       // ...
       annotationProcessor("org.projectlombok:lombok:...")
   }
```

2. Ensure that the `dev.sayaya.gwt.lombok` plugin, or the umbrella `dev.sayaya.gwt` plugin that includes it, is applied.
   If these settings are correct, Lombok should work without further configuration.

### Out of Memory During Compilation
**Error:** `java.lang.OutOfMemoryError: Java heap space`
**Solution:** Increase heap size in the GWT configuration:

```kotlin
gwt {
    minHeapSize = "2048M"
    maxHeapSize = "4096M"
}
```


## License

This project is available under the terms specified in the project’s license file.

## Related Projects

- [GWT Project](https://www.gwtproject.org/) – Google Web Toolkit
- [Lombok](https://projectlombok.org/) – Java annotation processor

## 📝 Changelog

### 2.4.0 (Latest)
- ⚠️ **Default GWT version**: The default `gwtVersion` is now `2.13.1` (was `2.12.1`). Set `gwt { gwtVersion = "2.12.1" }` to keep the previous SDK.
- ⬆️ **Dependency updates (`gwt-test`)**: Playwright `1.65.0`, Kotest `6.2.7`, Gson `2.15.0`. Test server: Ktor `3.6.1`.
- 🧊 **Configuration Cache**: `gwtGenerateTestHtml` no longer accesses `Task.project` at execution time, so `--configuration-cache` builds no longer fail with `Extension of type 'SourceSetContainer' does not exist`.
- 🧩 **Multi-module test servers**: The test web server build service is now keyed by project path (`gwtWebServer:<path>` for subprojects), so subprojects with the same name (e.g. `:a:client`, `:b:client`) no longer share one server and serve each other's `war`.
- 🚦 **Fail fast on missing pages (`gwt-test`)**: `GwtTestSpec` now fails immediately with the URL and HTTP status when the `@GwtHtml` page returns 4xx/5xx, instead of timing out later on an empty page.
- 🧹 Test tasks no longer get `-Dio.netty.leakDetection.level=PARANOID` (it did not affect the server, which runs in the Gradle daemon). Server start/stop messages now go through the Gradle logger (`lifecycle`).
- 🔒 **Test server path traversal fix**: The built-in test web server no longer serves files outside its content roots for requests like `/../secret.txt` (previously returned `200` with the file).
- 🔌 **No port race on startup**: With `webPort` unset or `0`, the test server now lets Netty pick a free port and reports the actually bound port, instead of probing a free port first and binding it again.
- ⚠️ **Missing `gwt.junit.remoteUrl` warning (`gwt-test`)**: `GwtTestSpec` now prints a warning to stderr when the property is missing and it falls back to `http://localhost:8080/`.
- 🐢 **No dependency resolution at configuration time (Lombok)**: `dev.sayaya.gwt.lombok` now locates the Lombok jar when a GWT task runs, instead of resolving `annotationProcessor` during `afterEvaluate`.
- 🧹 **No duplicate GWT classpath entries**: `gwtTestCompile` no longer modifies `gwt.devMode.extraSourceDirs` when it is created; the test plugin adds only the test source directories and the test runtime classpath once (main sources/outputs are already on the GWT classpath).
- 🛠️ **Late `Test` task creation fixed**: `<test>Implementation` now extends `gwtDevRuntimeClasspath` right after evaluation instead of when the `Test` task is created, so creating the task after the test classpath was resolved no longer fails with `Could not create task ':test'`.
- 🧩 **`jakarta = true` no longer excludes `jakarta.servlet-api`**: The `jakarta.servlet:jakarta.servlet-api` exclusion is now applied only when `gwt { jakarta }` is `false` (javax `gwt-servlet`). Previously it was applied to every configuration even with `jakarta = true`, so a directly declared `jakarta.servlet-api` was dropped.

### 2.3.0
- 🧩 **No third-party GWT plugin dependency**: Removed the dependency on `org.docstr.gwt`. The plugin now provides its own `gwt { }` extension, `gwtCompile` and `gwtDevMode` tasks, and adds GWT SDK dependencies (`org.gwtproject:gwt` BOM, `gwt-dev`, `gwt-user`, `gwt-codeserver`, `gwt-servlet`) automatically.
- ✅ **Compatible DSL**: Common options (`gwtVersion`, `modules`, `war`, `sourceLevel`, `minHeapSize`, `maxHeapSize`, `extraSourceDirs`, etc.), the `compiler { }` block (`strict`, `optimize`, `draftCompile`, ...; applied to both `gwtCompile` and `gwtTestCompile`), the `devMode { }` block, and the `gwtCompile` / `gwtDevMode` task names are unchanged.
- 🧪 **Test task defaults preserved**: Like `org.docstr.gwt`, every `Test` task gets the GWT heap size (`devMode` → `gwt` → 256M/512M), the `gwt.args` (`-war -deploy -extra`) and `gwt.persistentunitcachedir` system properties, main/test source directories on the test classpath, and `testImplementation` extending `gwtDevRuntimeClasspath`.
- ⚠️ **Breaking**: The `superDev { }` and `gwtTest { }` blocks and the `gwtSuperDev` task from `org.docstr.gwt` are no longer available. Default `gwtVersion` remains `2.12.1`.

### 2.2.9.5
- ✨ **Custom Web Port Support**: Added `gwt.test.webPort` property to allow manual port assignment in multi-module environments.
- 🔗 **Auto-Injection**: Port settings are automatically propagated to `gwt.junit.remoteUrl` system property.
- 📁 **Relative Path Support**: Refactored `GwtTestSpec` to use web server relative paths instead of absolute file system paths.
- 📟 **Console Log Fix**: Restored missing browser console log collection logic.
- 🔧 **Gradle 9.4+ Compatibility**: Confirmed support for the latest Gradle versions and Java 25.

### 2.2.9.1
