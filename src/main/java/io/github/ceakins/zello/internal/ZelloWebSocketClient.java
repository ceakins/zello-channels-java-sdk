package io.github.ceakins.zello.internal;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.Map;

public class ZelloWebSocketClient extends WebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(ZelloWebSocketClient.class);
    private final ZelloMessageHandler messageHandler;

    public ZelloWebSocketClient(URI serverUri, Map<String, String> httpHeaders, ZelloMessageHandler messageHandler) {
        super(serverUri, httpHeaders);
        this.messageHandler = messageHandler;
    }

    @Override
    public void onWebsocketHandshakeReceivedAsClient(WebSocket conn, ClientHandshake request, ServerHandshake response) {
        logger.debug("--- WebSocket Handshake Response ---");
        logger.debug("Status: {} {}", response.getHttpStatus(), response.getHttpStatusMessage());
        Iterator<String> iterate = response.iterateHttpFields();
        while (iterate.hasNext()) {
            String name = iterate.next();
            logger.debug("Header: {} = {}", name, response.getFieldValue(name));
        }
        logger.debug("------------------------------------");
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
            JSONObject jsonObject = new JSONObject(message);
            if (jsonObject.has("command")) {
                messageHandler.onServerCommand(JsonUtils.jsonToServerCommand(message));
            } else {
                messageHandler.onServerResponse(jsonObject);
            }
        } catch (Exception e) {
            logger.error("Failed to parse server message: {}", message, e);
            messageHandler.onError("Failed to parse server message", e);
        }
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        // --- FINAL CORRECTED HEADER PARSING ---
        if (bytes.remaining() < 9) { // Header is now 9 bytes
            logger.warn("Received a binary message smaller than the required 9-byte header.");
            return;
        }
        // Ensure we read in Big-Endian (Network Byte Order)
        bytes.order(ByteOrder.BIG_ENDIAN);

        byte type = bytes.get();
        int streamId = bytes.getInt();
        int packetId = bytes.getInt();
        logger.trace("Received audio packet: type={}, streamId={}, packetId={}, opusSize={}", type, streamId, packetId, bytes.remaining());

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