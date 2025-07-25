package io.github.ceakins.zello.internal.audio;

/**
 * Defines the constant audio parameters required by the Zello protocol.
 */
public final class AudioConstants {

    /**
     * Zello requires a 16kHz sample rate.
     */
    public static final int SAMPLE_RATE = 16000;

    /**
     * Zello uses single-channel (mono) audio.
     */
    public static final int CHANNELS = 1;

    /**
     * Zello requires 20ms audio packets.
     */
    public static final int FRAME_DURATION_MS = 20;

    /**
     * The number of samples per audio frame.
     * Calculated as: (16000 samples/sec) * (0.020 sec/frame) = 320 samples/frame.
     */
    public static final int SAMPLES_PER_FRAME = SAMPLE_RATE / (1000 / FRAME_DURATION_MS);

    /**
     * The size of a raw PCM audio frame in bytes.
     * Calculated as: 320 samples * 1 channel * 2 bytes/sample (16-bit) = 640 bytes.
     */
    public static final int FRAME_SIZE_BYTES = SAMPLES_PER_FRAME * CHANNELS * 2;

    /**
     * The maximum size of a compressed Opus packet. A safe buffer size.
     */
    public static final int MAX_OPUS_PACKET_SIZE = 4000;


    private AudioConstants() {
        // This is a constants class and should not be instantiated.
    }

}