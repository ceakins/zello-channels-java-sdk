package io.github.ceakins.zello;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for the ZelloRadioBridge, allowing customization of VOX parameters.
 */
@Getter
@Builder
public class ZelloRadioBridgeConfig {

    /**
     * The RMS audio level required to trigger VOX. Range is 0-1.0.
     * A good starting point is 0.05. Lower values are more sensitive.
     */
    @Builder.Default
    private double voxOpenThreshold = 0.05;

    /**
     * The RMS audio level required to close the VOX gate. Should be slightly lower than open to prevent flapping.
     */
    @Builder.Default
    private double voxCloseThreshold = 0.03;

    /**
     * How long (in milliseconds) of silence to wait before closing the stream.
     * This prevents the stream from cutting out between words.
     */
    @Builder.Default
    private long voxHangTimeMs = 10000;

    /**
     * The number of audio frames to buffer before the VOX gate opens.
     * This ensures the beginning of speech is not clipped.
     * Example: 10 frames * 20ms/frame = 200ms of pre-roll audio.
     */
    @Builder.Default
    private int preRollFrameCount = 10;

}