package io.github.ceakins.zello.model.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Fired when the server reports an error, such as an invalid request.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnErrorEvent extends ServerCommand {

    /**
     * A description of the error that occurred.
     */
    private String error;

}