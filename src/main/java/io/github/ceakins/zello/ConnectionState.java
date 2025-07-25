package io.github.ceakins.zello;

/**
 * Represents the various states of the connection to the Zello channel.
 */
public enum ConnectionState {
    /**
     * Not connected to the server.
     */
    DISCONNECTED,
    /**
     * Actively trying to establish a WebSocket connection.
     */
    CONNECTING,
    /**
     * WebSocket connection is open, but logon has not yet completed.
     */
    LOGGING_IN,
    /**
     * Fully connected and authenticated. Ready to send and receive data.
     */
    CONNECTED,
    /**
     * Actively trying to close the connection.
     */
    DISCONNECTING
}