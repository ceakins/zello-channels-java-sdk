package io.github.ceakins.zello.model.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Represents the "start_stream" command to begin sending audio to the channel.
 * The values for codec, header, and packet duration are fixed according to the Zello API specification.
 */
@Getter
public class StartStreamCommand extends Command {

    /**
     * The type of stream. For voice, this is always "audio".
     */
    private final String type = "audio";

    /**
     * The audio codec being used. Zello requires "opus".
     */
    private final String codec = "opus";

    /**
     * The packet duration in milliseconds. Zello requires 20ms.
     */
    @JsonProperty("packet_duration")
    private final int packetDuration = 20;

    /**
     * A Base64-encoded header containing Opus codec information.
     * This specific header is for 16kHz sample rate.
     */
    @JsonProperty("codec_header")
    private final String codecHeader = "f/4D"; // Base64 for 16kHz Opus

    public StartStreamCommand() {
        super("start_stream");
    }

}