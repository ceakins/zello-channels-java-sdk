package io.github.ceakins.zello.model.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Represents the "stop_stream" command to end an audio transmission.
 */
@Getter
public class StopStreamCommand extends Command {

    /**
     * The ID of the stream to stop, as received from the start_stream response.
     */
    @JsonProperty("stream_id")
    private final int streamId;

    /**
     * The channel the stream was on.
     */
    @JsonProperty("channel")
    private final String channel;

    public StopStreamCommand(int streamId, String channel) {
        super("stop_stream");
        this.streamId = streamId;
        this.channel = channel;
    }

}