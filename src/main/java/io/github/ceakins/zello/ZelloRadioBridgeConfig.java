package io.github.ceakins.zello;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for the ZelloRadioBridge.
 * Use the builder to construct a configuration object with custom VOX settings.
 */
@Getter
@Builder
public class ZelloRadioBridgeConfig {

    /**
     * The RMS audio level (0.0 to 1.0) required to trigger the start of a transmission.
     * Lower values are more sensitive. A typical value is between 0.02 and 0.1.
     */
    @Builder.Default
    private final double voxOpenThreshold = 0.05;

    /**
     * The RMS audio level (0.0 to 1.0) below which the audio is considered silence.
     * This should be slightly lower than the open threshold to prevent the stream from flapping
     * on and off with background noise.
     */
    @Builder.Default
    private final double voxCloseThreshold = 0.03;

    /**
     * The duration in milliseconds of silence to wait after the last sound before closing the stream.
     * This prevents the stream from cutting out between words or during short pauses.
     */
    @Builder.Default
    private final long voxHangTimeMs = 1000;

}