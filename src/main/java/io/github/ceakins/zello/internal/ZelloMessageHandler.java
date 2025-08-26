package io.github.ceakins.zello.internal;

import io.github.ceakins.zello.model.events.ServerCommand;
import org.json.JSONObject;

/**
 * An internal interface for handling events from the ZelloWebSocketClient.
 * This is implemented by ZelloChannel to process messages from the websocket layer.
 */
public interface ZelloMessageHandler {

    void onOpen();

    void onServerCommand(ServerCommand command);

    /**
     * Called when a generic JSON response (without a "command" field) is received from the server.
     * This is typically a direct reply to a command that was sent.
     *
     * @param response The parsed generic JSON response.
     */
    void onServerResponse(JSONObject response);

    void onAudioPacket(int streamId, byte[] audioData);

    void onClose(String reason);

    void onError(String errorMessage, Throwable t);

}