package io.github.ceakins.zello.model.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * Base class for all commands received from the Zello server.
 * This class uses Jackson's polymorphic deserialization to automatically
 * map incoming JSON messages to the correct subclass based on the "command" field.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "command", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LogonResultEvent.class, name = "logon"),
        @JsonSubTypes.Type(value = OnChannelStatusEvent.class, name = "on_channel_status"),
        @JsonSubTypes.Type(value = OnStreamStartEvent.class, name = "on_stream_start"),
        @JsonSubTypes.Type(value = OnStreamStopEvent.class, name = "on_stream_stop"),
        @JsonSubTypes.Type(value = OnTextMessageEvent.class, name = "on_text_message")
})
@Data // Lombok annotation for getters, setters, toString, etc.
public abstract class ServerCommand {

    /**
     * The name of the command, which determines the type of event.
     */
    private String command;

    /**
     * A sequence number from the original request, if this message is a direct response.
     */
    private int seq;

}