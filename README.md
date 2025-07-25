
# Zello Channels Java SDK

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase/21-relnote-issues.html)
[![Build Status](https://github.com/ceakins/zello-channels-java-sdk/actions/workflows/maven.yml/badge.svg)](https://github.com/YOUR_USERNAME/zello-channels-java-sdk/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.charles.eakins/zello-channels-java-sdk.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:com.charles.eakins%20AND%20a:zello-channels-java-sdk)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A modern, high-performance Java SDK for the [Zello Channels API](https://github.com/zelloptt/zello-channel-api/blob/master/API.md). This library provides a robust, pure Java solution for integrating with both Zello and ZelloWork channels, making it ideal for building custom clients, bots, and audio bridges (e.g., between a physical radio and a Zello channel).

This SDK is built on **Java 21** and leverages modern libraries to provide a clean and efficient developer experience.

## Features

*   **Built for Java 21:** Takes advantage of the latest language features and performance enhancements.
*   **Zello & ZelloWork Support:** Connect to any public or private channel on either service.
*   **Reliable Opus Codec:** Uses the industry-standard **LWJGL 3** bindings for robust Opus audio encoding and decoding.
*   **Reduced Boilerplate:** Leverages **Project Lombok** for clean, concise data models and builders.
*   **Event-Driven Architecture:** Provides a simple `ZelloChannelListener` interface to react to channel events like incoming voice, text messages, and status changes.
*   **Flexible Logging:** Uses the **SLF4J** facade, allowing you to plug in your favorite logging framework (Logback, Log4j2, etc.).
*   **Custom PTT & VOX:** Provides direct control over starting and stopping audio streams, perfect for building custom physical interfaces or voice-activated logic.
*   **Standalone Ready:** Designed to be easily packaged into a standalone JAR with all dependencies.

## Requirements

*   Java Development Kit (JDK) 21 or later
*   Apache Maven 3.6+ (for building from source)

## Installation

This package will be available on Maven Central. To add it to your project, include the following dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>com.charles.eakins</groupId>
    <artifactId>zello-channels-java-sdk</artifactId>
    <version>1.0.0</version> <!-- Replace with the latest version from Maven Central -->
</dependency>
```

## Quick Start

Here is a simple example of how to connect to a channel and listen for events.

```java
package com.charles.eakins.examples;

import com.charles.eakins.zello.sdk.ZelloChannel;
import com.charles.eakins.zello.sdk.ZelloChannelConfig;
import com.charles.eakins.zello.sdk.events.ZelloChannelListener;

public class MyZelloBot {

    public static void main(String[] args) {
        // 1. Configure the connection using the builder
        ZelloChannelConfig config = ZelloChannelConfig.builder()
                .username("mybot_username")
                .password("mybot_password")
                .channel("my-test-channel")
                .authToken("[your_auth_token_here]") // Or use username/password
                .serverUrl("wss://zello.io/ws") // or wss://mycompany.zellowork.com/ws for ZelloWork
                .build();
        
        // 2. Create the channel instance
        ZelloChannel channel = new ZelloChannel(config);

        // 3. Add a listener to handle events
        channel.setListener(new ZelloChannelListener() {
            @Override
            public void onConnected() {
                System.out.println("Successfully connected to channel!");
                channel.sendTextMessage("Hello, world! The bot is online.");
            }

            @Override
            public void onDisconnected() {
                System.out.println("Disconnected from channel.");
            }

            @Override
            public void onTextMessage(String from, String message) {
                System.out.println("Received text from " + from + ": " + message);
                if (message.equalsIgnoreCase("!ping")) {
                    channel.sendTextMessage("Pong!");
                }
            }
            
            @Override
            public void onStreamStarted(int streamId, String from) {
                System.out.println("Incoming voice stream started from " + from);
                // The SDK automatically handles decoding and playing the audio.
            }
            
            @Override
            public void onStreamStopped(int streamId, String from) {
                System.out.println("Voice stream from " + from + " has ended.");
            }
        });

        // 4. Connect to the channel
        try {
            channel.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Keep the application running
        // In a real application, you would manage the lifecycle differently.
        Runtime.getRuntime().addShutdownHook(new Thread(channel::disconnect));
    }
}
```

## Logging

This SDK uses SLF4J for logging. To see the log output, you must add an SLF4J-compatible logging implementation to your project's dependencies. For example, to use Logback:

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.6</version>
</dependency>
```

## Building From Source

To build the project yourself, clone the repository and run the Maven `install` command.

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/zello-channels-java-sdk.git
cd zello-channels-java-sdk

# Build the project and run tests
mvn clean install```

The compiled JAR file will be located in the `target` directory.

## Contributing

Contributions are welcome! If you have a feature request, bug report, or pull request, please feel free to open an issue or submit a PR.

1.  Fork the repository.
2.  Create your feature branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

## License

This project is licensed under the Apache License, Version 2.0. See the `LICENSE` file for details.