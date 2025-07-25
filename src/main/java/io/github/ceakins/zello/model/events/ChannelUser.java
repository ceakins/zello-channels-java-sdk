package io.github.ceakins.zello.model.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a single user in the channel roster.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChannelUser {

    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private String status;

}