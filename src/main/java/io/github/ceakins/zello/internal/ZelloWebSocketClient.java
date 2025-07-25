package io.github.ceakins.zello.internal;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.ByteBuffer;

/**
 * Internal WebSocket client for handling the connection to the Zello server.
 * It processes raw websocket events and forwards them to a MessageHandler.
 */
public class ZelloWebSocketClient extends WebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(ZelloWebSocketClient.class);

    private final ZelloMessageHandler messageHandler;

    public ZelloWebSocketClient(URI serverUri, ZelloMessageHandler messageHandler) {
        super(serverUri);
        this.messageHandler = messageHandler;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        logger.info("WebSocket connection opened to {}", uri);
        messageHandler.onOpen();
    }

    @Override
    public void onMessage(String message) {
        logger.debug("Received text message: {}", message);
        try {
            messageHandler.onServerCommand(JsonUtils.jsonToServerCommand(message));
        } catch (Exception e) {
            logger.error("Failed to parse server command JSON: {}", message, e);
            messageHandler.onError("Failed to parse server command", e);
        }
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        logger.debug("Received binary message of size: {}", bytes.remaining());
        // The first byte of an audio packet is the stream ID.
        int streamId = bytes.get();
        byte[] audioData = new byte[bytes.remaining()];
        bytes.get(audioData);
        messageHandler.onAudioPacket(streamId, audioData);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        String logMessage = String.format("WebSocket connection closed. Code: %d, Reason: %s, Remote: %b", code, reason, remote);
        logger.info(logMessage);
        messageHandler.onClose(reason);
    }

    @Override
    public void onError(Exception ex) {
        logger.error("WebSocket error occurred", ex);
        messageHandler.onError("A WebSocket error occurred", ex);
    }

}