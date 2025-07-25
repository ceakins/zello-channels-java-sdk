package io.github.ceakins.zello.model.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Fired when a user in the channel starts a voice stream.
 * After this event, the server will begin sending audio data packets.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnStreamStartEvent extends ServerCommand {

    /**
     * The unique identifier for this voice stream. All subsequent audio packets
     * for this transmission will use this ID.
     */
    @JsonProperty("stream_id")
    private int streamId;

    /**
     * The username of the user who is speaking.
     */
    private String from;

}