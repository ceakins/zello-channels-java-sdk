package io.github.ceakins.zello.internal;

import java.net.URI;
import java.util.Map;

/**
 * A factory for creating instances of ZelloWebSocketClient.
 * This allows for dependency injection, so we can use a mock factory during testing.
 */
public interface WebSocketClientFactory {
    ZelloWebSocketClient create(URI serverUri, Map<String, String> httpHeaders, ZelloMessageHandler messageHandler);
}
