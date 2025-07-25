package io.github.ceakins.zello.model.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Fired when a text message is received from a user in the channel.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnTextMessageEvent extends ServerCommand {

    /**
     * The username of the sender.
     */
    private String from;

    /**
     * The content of the text message.
     */
    private String message;

}