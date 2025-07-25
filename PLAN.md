# Zello Channels Java SDK - Final Project Plan

## I. Zello SDK for Java

This SDK will provide a modern, high-performance Java solution for integrating with the Zello Channels API, supporting both Zello and ZelloWork channels.

### 1. Core Concepts
- **Targeting Java 21:** The SDK will be built and compiled using JDK 21 to leverage the latest language features and performance enhancements.
- **WebSocket Communication:** All commands and data are transmitted over a secure WebSocket connection to the Zello server.
- **Event-Driven Architecture:** A listener-based model will be used to notify the client application of channel events (e.g., incoming audio, text messages, user status).
- **Opus Audio Requirement:** All voice communication must be encoded and decoded using the Opus audio codec as required by the Zello API.

### 2. Key Features
- **Reliable Opus Codec via LWJGL:** The project will use the industry-standard **Lightweight Java Game Library (LWJGL) 3** bindings for Opus. While this involves native libraries, LWJGL's Maven integration handles their management automatically, ensuring robust and high-performance audio processing across Windows, macOS, and Linux.
- **Reduced Boilerplate with Lombok:** **Project Lombok** will be used to generate getters, setters, constructors, and builders, keeping model classes clean and concise.
- **Fluent Builder Pattern:** The main `ZelloChannel` configuration will be constructed via a fluent **Builder design pattern** (`ZelloChannelConfig.builder()`), providing a readable and extensible API.
- **Flexible Logging with SLF4J:** The SDK will use the **SLF4J API**, allowing the end-user to choose their own logging implementation (e.g., Logback, Log4j2).
- **Custom Audio Control:** The SDK will provide clear methods for starting and stopping audio transmission, enabling custom Push-to-Talk (PTT) and Voice Activity Detection (VOX) implementations.

---

## II. High-Level Architecture

### 1. Package Structure
All SDK classes will reside under the base package `io.github.ceakins.zello`, which aligns with modern Maven Central publishing conventions.
- **Example:** `io.github.ceakins.zello.ZelloChannel`
- **Example:** `io.github.ceakins.zello.events.ZelloChannelListener`

### 2. Main Components
- **`ZelloChannelConfig`:** A data class (using Lombok's `@Builder`) to hold all configuration parameters like username, password, channel name, and server URL.
- **`ZelloChannel`:** The primary class for managing the WebSocket connection, handling authentication, and providing public methods for interaction (e.g., `connect()`, `disconnect()`, `sendTextMessage()`).
- **`ZelloChannelListener`:** An interface the client application will implement to receive callbacks for events like `onConnected`, `onTextMessage`, and `onStreamStarted`.
- **`AudioEngine`:** An internal component responsible for using the LWJGL Opus bindings to encode and decode all audio streams.
- **Data Models:** A series of plain old Java objects (POJOs) using Lombok to represent the JSON commands and events defined in the Zello API documentation.

---

## III. Maven Build Files (`pom.xml`)

### 1. Maven Coordinates
The project will be identified with the following coordinates:
- **groupId:** `io.github.ceakins`
- **artifactId:** `zello-channels-java-sdk`
- **version:** `1.0.0-SNAPSHOT`

### 2. Core Dependencies
- `org.projectlombok:lombok`
- `org.java-websocket:Java-WebSocket`
- `org.json:json`
- `org.slf4j:slf4j-api`
- `org.lwjgl:lwjgl-bom` (Bill of Materials to manage all LWJGL versions)
- `org.lwjgl:lwjgl`
- `org.lwjgl:lwjgl-opus` (along with native classifiers for Windows, Linux, and macOS)

### 3. Testing Dependencies
- `org.testng:testng`
- `org.mockito:mockito-core`
- `ch.qos.logback:logback-classic`

### 4. Build Plugins
- **`maven-compiler-plugin`:** Configured for Java 21 and integration with Lombok's annotation processor.
- **`maven-surefire-plugin`:** Configured to run the TestNG test suite.
- **`maven-jar-plugin`:** To package the core library.
- **`maven-source-plugin`:** To package the source code for distribution.
- **`maven-javadoc-plugin`:** To generate API documentation.
- **`maven-assembly-plugin`:** To create a standalone JAR with all dependencies for specific applications.

---

## IV. Testing Strategy
- The project will use **TestNG** as its primary testing framework.
- **Unit Tests:** Will cover individual classes and methods in isolation, using **Mockito** to mock dependencies.
- **Integration Tests:** Will be developed to test the full connection lifecycle by connecting to a live Zello channel, verifying authentication, and testing message/audio transmission. TestNG's suite XML files will be used to manage these tests.

---

## V. GitHub and Deployment
### 1. Repository
- **Name:** `zello-channels-java-sdk`
- **License:** **Apache License 2.0**. A `LICENSE` file will be included in the repository root.
- **`README.md`:** A comprehensive README will serve as the project's front page, detailing features, installation instructions, and a quick-start guide.

### 2. Build Automation
- A **GitHub Actions** workflow will be created to:
  - Trigger on pushes and pull requests to the `main` branch.
  - Set up a **JDK 21** environment.
  - Build the project using Maven (`mvn clean install`).
  - Run the TestNG test suite.

### 3. Maven Central Deployment
- **Prerequisites:**
  - A Sonatype (OSSRH) JIRA account will be needed to request publishing rights for the `io.github.ceakins` groupId.
  - A GPG key will be generated and used to sign the artifacts.
- **Process:** The GitHub Actions workflow will be extended to automate deployment to Maven Central upon the creation of a new release tag on GitHub. Secure credentials (Sonatype tokens, GPG keys) will be stored in GitHub Secrets.
```