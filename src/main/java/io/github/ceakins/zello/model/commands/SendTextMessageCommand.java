package io.github.ceakins.zello.model.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Represents the command to send a text message to a channel.
 */
@Getter
public class SendTextMessageCommand extends Command {

    /**
     * The channel to send the message to.
     */
    @JsonProperty("channel")
    private final String channel;

    /**
     * The content of the text message.
     */
    @JsonProperty("text")
    private final String text;

    public SendTextMessageCommand(String channel, String text) {
        super("send_text_message");
        this.channel = channel;
        this.text = text;
    }

}