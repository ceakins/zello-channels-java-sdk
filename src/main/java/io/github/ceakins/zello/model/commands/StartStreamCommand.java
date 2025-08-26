package io.github.ceakins.zello.model.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Represents the "start_stream" command to begin sending audio to the channel.
 * The values for codec and packet duration are fixed according to the Zello API specification.
 */
@Getter
public class StartStreamCommand extends Command {

    @JsonProperty("type")
    private final String type = "audio";

    @JsonProperty("codec")
    private final String codec = "opus";

    @JsonProperty("packet_duration")
    private final int packetDuration = 20;

    /**
     * A Base64-encoded 4-byte header containing Opus codec information.
     * This value represents: {16000 Hz (LE), 1 frame/packet, 20ms frame size}
     * as required by the API documentation.
     */
    @JsonProperty("codec_header")
    private final String codecHeader = "gD4BFA=="; // This is the corrected value

    public StartStreamCommand() {
        super("start_stream");
    }

}