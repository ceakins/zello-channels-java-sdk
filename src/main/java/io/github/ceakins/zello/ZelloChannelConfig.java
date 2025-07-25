package io.github.ceakins.zello;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Configuration for connecting to a Zello channel.
 * Use the builder to construct a configuration object.
 */
@Getter
@Builder
@ToString(exclude = {"password", "authToken"}) // Exclude sensitive info from toString()
public class ZelloChannelConfig {

    /**
     * The WebSocket URL of the Zello server.
     * For Zello, use "wss://zello.io/ws".
     * For ZelloWork, use "wss://your-network-name.zellowork.com/ws".
     */
    private final String serverUrl;

    /**
     * The username for authentication.
     */
    private final String username;

    /**
     * The password for authentication.
     */
    private final String password;

    /**
     * The authentication token. If provided, username and password will be ignored.
     */
    private final String authToken;

    /**
     * The name of the channel to join.
     */
    private final String channel;

}