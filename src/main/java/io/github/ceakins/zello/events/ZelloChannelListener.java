package io.github.ceakins.zello.events;

import io.github.ceakins.zello.model.events.OnImageEvent;

/**
 * An interface for receiving events from a Zello channel.
 * Implement this interface to handle incoming messages, stream status changes,
 * and connection lifecycle events.
 */
public interface ZelloChannelListener {

    void onConnected();

    void onDisconnected(String reason);

    void onError(String errorMessage, Throwable t);

    void onTextMessage(String from, String message);

    void onStreamStarted(int streamId, String from);

    void onStreamStopped(int streamId, String from);

    void onAudioData(int streamId, byte[] audioData);

    /**
     * Called when an image is received from a user in the channel.
     *
     * @param event The event object containing details about the image.
     */
    void onImageEvent(OnImageEvent event);

}