package io.github.ceakins.zello;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.ceakins.zello.events.ZelloChannelListener;
import io.github.ceakins.zello.internal.JsonUtils;
import io.github.ceakins.zello.internal.ZelloMessageHandler;
import io.github.ceakins.zello.internal.ZelloWebSocketClient;
import io.github.ceakins.zello.internal.audio.AudioEngine;
import io.github.ceakins.zello.model.commands.*;
import io.github.ceakins.zello.model.events.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ZelloChannel implements ZelloMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ZelloChannel.class);
    private static final String USER_AGENT = "zello-channels-java-sdk/1.0.0";
    private static final int MAX_IMAGE_PACKET_SIZE = 16384; // 16KB chunk size for images

    @Getter
    @AllArgsConstructor
    private static class PendingImage {
        private final byte[] thumbnailData;
        private final byte[] fullImageData;
    }

    private final Map<Integer, PendingImage> pendingImages = new ConcurrentHashMap<>();
    private final Map<Integer, Consumer<JSONObject>> commandCallbacks = new ConcurrentHashMap<>();
    private final Map<Integer, String> activeIncomingStreams = new ConcurrentHashMap<>();

    private final ZelloChannelConfig config;
    private ZelloChannelListener listener;
    private ZelloWebSocketClient webSocketClient;
    private final AudioEngine audioEngine;
    private final AtomicInteger sequence = new AtomicInteger(1);
    private volatile ConnectionState state = ConnectionState.DISCONNECTED;
    private volatile int outgoingStreamId = -1;
    private volatile int outgoingPacketId = 0;

    public ZelloChannel(ZelloChannelConfig config) {
        this.config = config;
        this.audioEngine = new AudioEngine();
    }

    public void setListener(ZelloChannelListener listener) {
        this.listener = listener;
    }

    public ConnectionState getState() {
        return this.state;
    }

    public void connect() throws URISyntaxException {
        if (state != ConnectionState.DISCONNECTED) {
            throw new IllegalStateException("Cannot connect when not in DISCONNECTED state. Current state: " + state);
        }
        logger.info("Connecting to channel '{}' at {}", config.getChannel(), config.getServerUrl());
        state = ConnectionState.CONNECTING;
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", USER_AGENT);
        headers.put("Pragma", "zello-api-v1");
        logger.debug("--- WebSocket Handshake Request ---");
        headers.forEach((key, value) -> logger.debug("Header: {} = {}", key, value));
        logger.debug("-----------------------------------");
        webSocketClient = new ZelloWebSocketClient(new URI(config.getServerUrl()), headers, this);
        webSocketClient.connect();
    }

    public void disconnect() {
        if (webSocketClient != null) {
            state = ConnectionState.DISCONNECTING;
            webSocketClient.close();
        }
        audioEngine.close();
    }

    public void startVoiceStream() {
        if (state != ConnectionState.CONNECTED) {
            logger.warn("Cannot start voice stream while not connected.");
            return;
        }
        logger.debug("Sending start_stream command...");
        this.outgoingPacketId = 0;
        sendCommand(new StartStreamCommand());
    }

    public void stopVoiceStream() {
        if (state != ConnectionState.CONNECTED || outgoingStreamId == -1) {
            return;
        }
        logger.debug("Sending stop_stream command for stream ID {}", outgoingStreamId);
        sendCommand(new StopStreamCommand(outgoingStreamId, config.getChannel()));
        this.outgoingStreamId = -1;
    }

    public void sendVoiceData(byte[] pcmData) {
        if (state != ConnectionState.CONNECTED || outgoingStreamId == -1) {
            logger.warn("Cannot send voice data: not connected or stream not started.");
            return;
        }
        byte[] opusData = audioEngine.encode(pcmData);
        if (opusData != null && webSocketClient != null && webSocketClient.isOpen()) {
            ByteBuffer packet = ByteBuffer.allocate(9 + opusData.length);
            packet.order(ByteOrder.BIG_ENDIAN);
            packet.put((byte) 0x01);
            packet.putInt(outgoingStreamId);
            packet.putInt(outgoingPacketId);
            packet.put(opusData);
            logger.trace("Sending audio packet: streamId={}, packetId={}, opusSize={}", outgoingStreamId, outgoingPacketId, opusData.length);
            webSocketClient.send(packet.array());
            outgoingPacketId++;
        }
    }

    public void sendTextMessage(String message) {
        sendTextMessage(message, null);
    }

    public void sendTextMessage(String message, Consumer<JSONObject> ackCallback) {
        if (state != ConnectionState.CONNECTED) {
            logger.warn("Cannot send text message while not connected.");
            return;
        }
        logger.debug("Sending text message: '{}'", message);
        sendCommand(new SendTextMessageCommand(config.getChannel(), message), ackCallback);
    }

    public void sendImage(byte[] jpegData) {
        if (state != ConnectionState.CONNECTED) {
            logger.warn("Cannot send image while not connected.");
            return;
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(jpegData));
            if (image == null) {
                throw new IOException("Could not decode provided image data.");
            }
            int width = image.getWidth();
            int height = image.getHeight();
            byte[] thumbnailData = createThumbnail(image, 100);
            SendImageCommand command = SendImageCommand.builder().channel(config.getChannel()).width(width).height(height).contentLength(jpegData.length).thumbnailContentLength(thumbnailData.length).build();
            int seq = sequence.getAndIncrement();
            command.setSequence(seq);
            pendingImages.put(seq, new PendingImage(thumbnailData, jpegData));
            sendCommand(command);
        } catch (IOException e) {
            logger.error("Failed to process image for sending", e);
            if (listener != null) listener.onError("Failed to process image for sending", e);
        }
    }

    @Override
    public void onOpen() {
        logger.info("WebSocket connection established. Sending logon command...");
        state = ConnectionState.LOGGING_IN;
        sendCommand(new LogonCommand(config));
    }

    @Override
    public void onServerResponse(JSONObject response) {
        logger.debug("Received server response: {}", response);
        int seq = response.optInt("seq", -1);

        if (seq != -1 && commandCallbacks.containsKey(seq)) {
            commandCallbacks.remove(seq).accept(response);
            return;
        }

        boolean isError = response.has("error") || (response.has("success") && !response.getBoolean("success"));
        if (isError) {
            String error = response.optString("error", "An unknown error occurred.");
            logger.error("A command failed (seq={}): {}", seq, error);
            if (listener != null) listener.onError("A command failed: " + error, null);
            pendingImages.remove(seq);
            return;
        }
        if (response.has("stream_id")) {
            this.outgoingStreamId = response.getInt("stream_id");
            logger.info("Outgoing stream started successfully with ID: {}", outgoingStreamId);
        }
        if (response.has("image_id") && seq != -1) {
            int imageId = response.getInt("image_id");
            PendingImage image = pendingImages.remove(seq);
            if (image != null) {
                logger.info("Received image_id '{}' for seq {}. Starting binary upload.", imageId, seq);
                sendBinaryImageData(imageId, image.getThumbnailData(), image.getFullImageData());
            }
        }
    }

    @Override
    public void onServerCommand(ServerCommand command) {
        if (command instanceof OnChannelStatusEvent event) {
            if ("online".equals(event.getStatus())) {
                logger.info("Logon successful. Channel is online.");
                state = ConnectionState.CONNECTED;
                if (listener != null) listener.onConnected();
            }
        } else if (command instanceof OnTextMessageEvent event) {
            if (listener != null) listener.onTextMessage(event.getFrom(), event.getMessage());
        } else if (command instanceof OnStreamStartEvent event) {
            activeIncomingStreams.put(event.getStreamId(), event.getFrom());
            audioEngine.startDecodingSession(event.getStreamId());
            if (listener != null) listener.onStreamStarted(event.getStreamId(), event.getFrom());
        } else if (command instanceof OnStreamStopEvent event) {
            String from = activeIncomingStreams.remove(event.getStreamId());
            audioEngine.stopDecodingSession(event.getStreamId());
            if (listener != null) listener.onStreamStopped(event.getStreamId(), from);
        } else if (command instanceof OnErrorEvent event) {
            logger.error("Received an error event from the server: {}", event.getError());
            if (listener != null) listener.onError("Server error: " + event.getError(), null);
        } else if (command instanceof OnImageEvent event) {
            logger.info("Received image from {}", event.getFrom());
            if (listener != null) listener.onImageEvent(event);
        }
    }

    @Override
    public void onAudioPacket(int streamId, byte[] audioData) {
        byte[] pcmData = audioEngine.decode(streamId, audioData);
        if (listener != null && pcmData != null) {
            listener.onAudioData(streamId, pcmData);
        }
    }

    @Override
    public void onClose(String reason) {
        state = ConnectionState.DISCONNECTED;
        if (listener != null) {
            listener.onDisconnected(reason);
        }
    }

    @Override
    public void onError(String errorMessage, Throwable t) {
        if (listener != null) {
            listener.onError(errorMessage, t);
        }
    }

    private void sendCommand(Command command) {
        sendCommand(command, null);
    }

    private void sendCommand(Command command, Consumer<JSONObject> ackCallback) {
        if (webSocketClient == null || !webSocketClient.isOpen()) {
            logger.error("Cannot send command while websocket is closed.");
            return;
        }
        int seq = sequence.getAndIncrement();
        command.setSequence(seq);
        if (ackCallback != null) {
            commandCallbacks.put(seq, ackCallback);
        }
        try {
            String jsonToSend = JsonUtils.commandToJson(command);
            logger.debug("Sending command: {}", jsonToSend);
            webSocketClient.send(jsonToSend);
        } catch (JsonProcessingException e) {
            String errorMsg = "Failed to serialize command: " + command.getCommand();
            logger.error(errorMsg, e);
            onError(errorMsg, e);
        }
    }

    private void sendBinaryImageData(int imageId, byte[] thumbnailData, byte[] fullImageData) {
        if (webSocketClient == null || !webSocketClient.isOpen()) return;

        final int IMAGE_TYPE_FULL = 0x01;
        final int IMAGE_TYPE_THUMBNAIL = 0x02;

        logger.debug("Sending thumbnail ({} bytes) for imageId {}", thumbnailData.length, imageId);
        webSocketClient.send(createImagePacket(imageId, IMAGE_TYPE_THUMBNAIL, thumbnailData));

        logger.debug("Sending full image ({} bytes) for imageId {}", fullImageData.length, imageId);
        webSocketClient.send(createImagePacket(imageId, IMAGE_TYPE_FULL, fullImageData));
    }

    private byte[] createImagePacket(int imageId, int imageType, byte[] data) {
        ByteBuffer packet = ByteBuffer.allocate(9 + data.length);
        packet.order(ByteOrder.BIG_ENDIAN);
        packet.put((byte) 0x02);
        packet.putInt(imageId);
        packet.putInt(imageType);
        packet.put(data);
        return packet.array();
    }

    private byte[] createThumbnail(BufferedImage originalImage, int targetWidth) throws IOException {
        int targetHeight = (int) ((double) originalImage.getHeight() / (double) originalImage.getWidth() * targetWidth);
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(outputImage, "jpeg", os);
            return os.toByteArray();
        }
    }
}