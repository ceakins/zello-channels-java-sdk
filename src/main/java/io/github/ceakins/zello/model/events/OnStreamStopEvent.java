package io.github.ceakins.zello.model.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Fired when a user in the channel stops their voice stream.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnStreamStopEvent extends ServerCommand {

    /**
     * The unique identifier for the stream that has now ended.
     */
    @JsonProperty("stream_id")
    private int streamId;

    /**
     * The username of the user who was speaking.
     */
    private String from;

}