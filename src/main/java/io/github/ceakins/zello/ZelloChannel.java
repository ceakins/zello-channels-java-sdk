package io.github.ceakins.zello;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.ceakins.zello.internal.JsonUtils;
import io.github.ceakins.zello.internal.ZelloMessageHandler;
import io.github.ceakins.zello.internal.ZelloWebSocketClient;
import io.github.ceakins.zello.internal.audio.AudioEngine;
import io.github.ceakins.zello.model.commands.Command;
import io.github.ceakins.zello.model.commands.LogonCommand;
import io.github.ceakins.zello.model.commands.StartStreamCommand;
import io.github.ceakins.zello.model.commands.StopStreamCommand;
import io.github.ceakins.zello.model.events.*;
import io.github.ceakins.zello.events.ZelloChannelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The main class for interacting with a Zello channel.
 * This class manages the connection lifecycle, sends commands, and dispatches events.
 */
public class ZelloChannel implements ZelloMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ZelloChannel.class);

    private final ZelloChannelConfig config;
    private ZelloChannelListener listener;
    private ZelloWebSocketClient webSocketClient;

    // --- NEW: AudioEngine instance ---
    private final AudioEngine audioEngine;

    private final AtomicInteger sequence = new AtomicInteger(1);
    private volatile ConnectionState state = ConnectionState.DISCONNECTED;

    /**
     * Constructs a new ZelloChannel with the specified configuration.
     *
     * @param config The configuration object containing connection details.
     */
    public ZelloChannel(ZelloChannelConfig config) {
        this.config = config;
        // --- NEW: Initialize the AudioEngine ---
        this.audioEngine = new AudioEngine();
    }

    /**
     * Sets the listener that will receive all channel events.
     *
     * @param listener An implementation of ZelloChannelListener.
     */
    public void setListener(ZelloChannelListener listener) {
        this.listener = listener;
    }

    /**
     * Connects to the Zello channel. This method is asynchronous.
     * Connection status will be reported through the {@link ZelloChannelListener}.
     *
     * @throws IllegalStateException if already connected or connecting.
     * @throws URISyntaxException if the server URL in the config is invalid.
     */
    public void connect() throws URISyntaxException {
        if (state != ConnectionState.DISCONNECTED) {
            throw new IllegalStateException("Cannot connect when not in DISCONNECTED state. Current state: " + state);
        }

        logger.info("Connecting to channel '{}' at {}", config.getChannel(), config.getServerUrl());
        state = ConnectionState.CONNECTING;

        webSocketClient = new ZelloWebSocketClient(new URI(config.getServerUrl()), this);
        webSocketClient.connect();
    }

    /**
     * Disconnects from the Zello channel and releases resources.
     */
    public void disconnect() {
        if (webSocketClient != null) {
            state = ConnectionState.DISCONNECTING;
            webSocketClient.close();
        }
        // --- NEW: Clean up the AudioEngine to prevent memory leaks ---
        audioEngine.close();
    }

    //region Public API for Sending Audio

    /**
     * Starts a voice stream to the channel.
     * After calling this, you can begin sending audio data with {@link #sendVoiceData(byte[])}.
     */
    public void startVoiceStream() {
        if (state != ConnectionState.CONNECTED) {
            logger.warn("Cannot start voice stream while not connected.");
            return;
        }
        logger.debug("Sending start_stream command...");
        sendCommand(new StartStreamCommand());
    }

    /**
     * Stops the current voice stream.
     */
    public void stopVoiceStream() {
        if (state != ConnectionState.CONNECTED) {
            return; // No need to stop if not connected
        }
        logger.debug("Sending stop_stream command...");
        sendCommand(new StopStreamCommand());
    }

    /**
     * Encodes a frame of raw PCM data and sends it to the channel.
     * You must call {@link #startVoiceStream()} before using this method.
     *
     * @param pcmData A byte array of raw 16-bit, 16kHz, mono PCM audio.
     *                The array size should match {@link io.github.ceakins.zello.internal.audio.AudioConstants#FRAME_SIZE_BYTES}.
     */
    public void sendVoiceData(byte[] pcmData) {
        if (state != ConnectionState.CONNECTED) {
            return; // Can't send data if not connected
        }
        byte[] opusData = audioEngine.encode(pcmData);
        if (opusData != null && webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(opusData);
        }
    }

    //endregion

    //region ZelloMessageHandler Implementation

    @Override
    public void onOpen() {
        logger.info("WebSocket connection established. Sending logon command...");
        state = ConnectionState.LOGGING_IN;
        sendCommand(new LogonCommand(config));
    }

    @Override
    public void onServerCommand(ServerCommand command) {
        if (command instanceof LogonResultEvent event) {
            if (!event.isSuccess()) {
                logger.error("Logon failed: {}", event.getError());
                if (listener != null) listener.onError("Logon failed: " + event.getError(), null);
                disconnect();
            }
        } else if (command instanceof OnChannelStatusEvent event) {
            if ("online".equals(event.getStatus())) {
                logger.info("Logon successful. Channel is online.");
                state = ConnectionState.CONNECTED;
                if (listener != null) listener.onConnected();
            }
        } else if (command instanceof OnTextMessageEvent event) {
            if (listener != null) listener.onTextMessage(event.getFrom(), event.getMessage());
        } else if (command instanceof OnStreamStartEvent event) {
            // --- IMPLEMENTED ---
            audioEngine.startDecodingSession(event.getStreamId());
            if (listener != null) listener.onStreamStarted(event.getStreamId(), event.getFrom());
        } else if (command instanceof OnStreamStopEvent event) {
            // --- IMPLEMENTED ---
            audioEngine.stopDecodingSession(event.getStreamId());
            if (listener != null) listener.onStreamStopped(event.getStreamId(), event.getFrom());
        }
    }

    @Override
    public void onAudioPacket(int streamId, byte[] audioData) {
        // --- IMPLEMENTED ---
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

    //endregion

    // --- NEW: Helper method to send commands ---
    private void sendCommand(Command command) {
        if (webSocketClient == null || !webSocketClient.isOpen()) {
            logger.error("Cannot send command while websocket is closed.");
            return;
        }
        command.setSequence(sequence.getAndIncrement());
        try {
            webSocketClient.send(JsonUtils.commandToJson(command));
        } catch (JsonProcessingException e) {
            String errorMsg = "Failed to serialize command: " + command.getCommand();
            logger.error(errorMsg, e);
            onError(errorMsg, e);
        }
    }

}