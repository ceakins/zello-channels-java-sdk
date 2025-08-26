package io.github.ceakins.zello.model.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnImageEvent extends ServerCommand {

    @JsonProperty("from")
    private String from;

    @JsonProperty("thumbnail")
    private String thumbnail;

    /**
     * The unique identifier for the full-size image.
     * Changed from String to int to match server behavior.
     */
    @JsonProperty("image_id")
    private int imageId;

}