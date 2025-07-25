package io.github.ceakins.zello.events;

/**
 * An interface for receiving events from a Zello channel.
 * Implement this interface to handle incoming messages, stream status changes,
 * and connection lifecycle events.
 */
public interface ZelloChannelListener {

    /**
     * Called when the WebSocket connection to the Zello server has been successfully established
     * and the channel logon process is complete.
     */
    void onConnected();

    /**
     * Called when the connection to the Zello server is lost or fails to establish.
     *
     * @param reason A description of why the disconnection occurred.
     */
    void onDisconnected(String reason);

    /**
     * Called when an error occurs within the SDK, either from the WebSocket
     * connection or during event processing.
     *
     * @param errorMessage A descriptive error message.
     * @param t The throwable exception, if one was caught. Can be null.
     */
    void onError(String errorMessage, Throwable t);

    /**
     * Called when a text message is received from a user in the channel.
     *
     * @param from The username of the sender.
     * @param message The content of the text message.
     */
    void onTextMessage(String from, String message);

    /**
     * Called when a user starts transmitting a voice stream.
     * <p>
     * You will subsequently receive one or more {@code onAudioData} calls for this stream.
     *
     * @param streamId The unique identifier for this voice stream.
     * @param from The username of the user who is speaking.
     */
    void onStreamStarted(int streamId, String from);

    /**
     * Called when a user stops transmitting a voice stream.
     * <p>
     * No more audio data will be sent for this stream ID.
     *
     * @param streamId The unique identifier for the stream that has ended.
     * @param from The username of the user who was speaking.
     */
    void onStreamStopped(int streamId, String from);

    /**
     * Called when decoded audio data from a voice stream is available.
     * <p>
     * The audio data is provided as raw PCM, 16-bit, 16kHz, single-channel.
     *
     * @param streamId The unique identifier for the voice stream this data belongs to.
     * @param audioData A byte array containing the raw PCM audio data.
     */
    void onAudioData(int streamId, byte[] audioData);

}