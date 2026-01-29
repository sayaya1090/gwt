# GWT Gradle Plugin

This project is a Gradle plugin designed to simplify GWT (Google Web Toolkit) development in a Gradle environment, with a special focus on testing and Lombok integration. It extends the existing `org.docstr.gwt` plugin and automates repetitive setup and execution steps required for GWT tests.

## ✨ Key Features

### Gradle Plugin

- **Full Lombok Support**: Automatically configures the `-javaagent` option so that the GWT compiler can process Lombok annotations.
- **Automatic Test Web Server Management**: Automatically starts a Ktor-based embedded web server when running GWT tests and ensures that it is always shut down after tests complete (regardless of success or failure) to safely clean up resources.
- **Automatic HTML Host File Generation**: Automatically generates the HTML host file required for each GWT test module, including support for the `rename-to` attribute, so you don’t have to manage these files manually.
- **Simplified Test Configuration**: Manage GWT test–related settings (like web server ports) intuitively through a `gwt` extension on test tasks.
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
    id("dev.sayaya.gwt") version "2.2.7"
}
```

#### Groovy DSL

```groovy
plugins {
    id 'dev.sayaya.gwt' version '2.2.7'
}
```

### 2. Add the kotest + playwright Test Library (Optional)

If you want to write browser tests using kotest + playwright:

```kotlin
dependencies {
    testImplementation("dev.sayaya:gwt-test:2.2.7")
}
```

## ⚙️ Configuration

The plugin extends the base GWT plugin configuration. Configure GWT in the `gwt` block:

```kotlin
gwt {
    gwtVersion = "2.12.2"
    modules = listOf("com.example.App")
    war = file("src/main/webapp")
    devMode {
        modules = listOf("com.example.Test")
    }
}

```

## Tasks

### `gwtTestCompile`

Compiles GWT test modules including both `main` and `test` sources.

```bash
./gradlew gwtTestCompile
```

### `gwtDevMode`

Starts GWT Dev Mode with access to test sources.

```bash
./gradlew gwtDevMode
```

### `test`

Runs tests (automatically depends on `gwtTestCompile`).

```bash
./gradlew test
```

## 📖 Usage Examples

### Basic Plugin Setup

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

    // Lombok support
    implementation("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    // Tests
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

@GwtHtml("src/test/webapp/test.html")  // HTML file under test
class MenuTest : GwtTestSpec({
    Given("the menu is loaded") {
        When("the menu button is clicked") {
            document.locator("#menu-button").click()

            Then("the menu should be visible") {
                document shouldContainLog "Menu opened"
            }
        }
    }
})
```

### Helper Methods

#### Console Log Verification

```kotlin
// Check if a specific text is contained in the logs (logs are automatically cleared afterward)
document shouldContainLog "Expected message"

// Check that a specific text is not present in the logs (logs are automatically cleared afterward)
document shouldNotContainLog "Error message"

// Retrieve all console logs
val logs: List<String> = document.getConsoleLogs()

// Manually clear console logs
document.clearConsoleLogs()
```

#### Configuration Options

```kotlin
@GwtHtml("src/test/webapp/test.html")  // HTML file path
class MyTest : GwtTestSpec({
    // Test logic...
})
```

### Real-World Example

```kotlin
import dev.sayaya.gwt.test.GwtTestSpec
import dev.sayaya.gwt.test.GwtHtml

@GwtHtml("src/test/webapp/test.html")
class UserInterfaceTest : GwtTestSpec({
    Given("The page is loaded") {
        When("Login button is clicked") {
            document.locator("#login-btn").click()

            Then("Login dialog should be displayed") {
                document shouldContainLog "Login dialog opened"
            }
        }

        When("Username is entered") {
            document.locator("#username").fill("testuser")

            Then("A validation log should be printed") {
                document shouldContainLog "Username validated"
            }
        }
    }
})
```

## Requirements

### Gradle Plugin

- Gradle 8.0+
- Kotlin 1.9+ (for Kotlin DSL)
- Java 11+
- GWT 2.10.0+

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
    ├── Applies org.docstr.gwt (base GWT plugin)
    ├── Registers GwtTestCompileTask
    └── Manages web server automatically during tests
```

### Task Dependency Flow

```
test
├── dependsOn: openWebServer
│   └── dependsOn: gwtTestCompile
│       └── dependsOn: gwtGenerateTestHtml
└── finalizedBy: closeWebServer

gwtDevMode
└── dependsOn: gwtGenerateTestHtml

war
└── dependsOn: test
```


**Task Descriptions:**
- `gwtGenerateTestHtml`: Automatically generates HTML host files for GWT test modules.
- `gwtTestCompile`: Compiles GWT test modules (includes both main and test sources).
- `openWebServer`: Starts a Ktor-based static file web server.
- `closeWebServer`: Stops the web server.
- `gwtDevMode`: Runs GWT Dev Mode including test sources.

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

### Web Server Does Not Shut Down
**Symptom:** The port remains in use even after tests finish.
**Solution:**
1. Do not start or stop the server manually; the plugin manages it automatically.
2. The server is shut down via `finalizedBy` even if tests fail.
3. To shut it down manually, run: `./gradlew closeWebServer`


## License

This project is available under the terms specified in the project’s license file.

## Related Projects

- [gwt-gradle-plugin](https://github.com/docstr/gwt-gradle-plugin) – Base GWT Gradle plugin
- [GWT Project](https://www.gwtproject.org/) – Google Web Toolkit
- [Lombok](https://projectlombok.org/) – Java annotation processor

## 📝 Changelog

### 2.2.7 (Latest)

- ✨ Added automatic built-in web server management for GWT tests
- ✨ Added automatic HTML host file generation (with `rename-to` support)
- ✨ Added Lombok Java Agent auto-configuration
- 📚 Completed KDoc documentation for all public APIs
- ✅ Achieved comprehensive test coverage
- 🔧 Refactored using configuration helper methods
