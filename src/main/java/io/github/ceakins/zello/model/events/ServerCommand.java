package io.github.ceakins.zello.model.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "command", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OnChannelStatusEvent.class, name = "on_channel_status"),
        @JsonSubTypes.Type(value = OnStreamStartEvent.class, name = "on_stream_start"),
        @JsonSubTypes.Type(value = OnStreamStopEvent.class, name = "on_stream_stop"),
        @JsonSubTypes.Type(value = OnTextMessageEvent.class, name = "on_text_message"),
        @JsonSubTypes.Type(value = OnErrorEvent.class, name = "on_error"),
        @JsonSubTypes.Type(value = OnImageEvent.class, name = "on_image")
})
@Data
public abstract class ServerCommand {

    private String command;
    private int seq;

}