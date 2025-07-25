package io.github.ceakins.zello.internal;

import io.github.ceakins.zello.model.events.ServerCommand;

/**
 * An internal interface for handling events from the ZelloWebSocketClient.
 * This is implemented by ZelloChannel to process messages from the websocket layer.
 */
public interface ZelloMessageHandler {

    /**
     * Called when the WebSocket connection is successfully opened.
     */
    void onOpen();

    /**
     * Called when a text message from the server has been successfully parsed into a command object.
     *
     * @param command The parsed command from the server.
     */
    void onServerCommand(ServerCommand command);

    /**
     * Called when a binary audio packet is received.
     *
     * @param streamId The ID of the stream this audio belongs to.
     * @param audioData The raw Opus audio data.
     */
    void onAudioPacket(int streamId, byte[] audioData);

    /**
     * Called when the WebSocket connection is closed.
     *
     * @param reason The reason for the closure.
     */
    void onClose(String reason);

    /**
     * Called when any error occurs at the WebSocket level.
     *
     * @param errorMessage A description of the error.
     * @param t The throwable exception, if available.
     */
    void onError(String errorMessage, Throwable t);

}