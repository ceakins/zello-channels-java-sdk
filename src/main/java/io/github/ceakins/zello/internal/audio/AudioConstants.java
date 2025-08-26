package io.github.ceakins.zello.internal.audio;

public final class AudioConstants {

    public static final int SAMPLE_RATE = 16000;
    public static final int CHANNELS = 1;
    public static final int FRAME_DURATION_MS = 20;
    public static final int SAMPLES_PER_FRAME = SAMPLE_RATE / (1000 / FRAME_DURATION_MS); // 320
    public static final int FRAME_SIZE_BYTES = SAMPLES_PER_FRAME * CHANNELS * 2; // 640

    // --- Constants for robust decoding ---
    public static final int MAX_FRAME_DURATION_MS = 120;
    public static final int MAX_SAMPLES_PER_PACKET = SAMPLE_RATE / (1000 / MAX_FRAME_DURATION_MS); // 1920
    public static final int MAX_DECODE_BUFFER_SIZE_BYTES = MAX_SAMPLES_PER_PACKET * CHANNELS * 2; // 3840 bytes

    // --- CORRECTED: Added the missing constant back for encoding ---
    /**
     * The maximum recommended size of a compressed Opus packet. A safe buffer size for encoding operations.
     */
    public static final int MAX_OPUS_PACKET_SIZE = 4000;

    private AudioConstants() {
        // This is a constants class and should not be instantiated.
    }

}