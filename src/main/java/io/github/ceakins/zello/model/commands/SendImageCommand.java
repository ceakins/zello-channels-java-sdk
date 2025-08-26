package io.github.ceakins.zello.model.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Represents the command to initiate sending an image to the channel.
 * This command sends only the image metadata. The binary data is sent in subsequent packets.
 */
@Getter
@Builder
public class SendImageCommand extends Command {

    @JsonProperty("channel")
    private final String channel;

    @JsonProperty("type")
    private final String type = "jpeg";

    @JsonProperty("source")
    @Builder.Default
    private final String source = "library"; // "camera" or "library"

    @JsonProperty("width")
    private final int width;

    @JsonProperty("height")
    private final int height;

    @JsonProperty("thumbnail_content_length")
    private final int thumbnailContentLength;

    @JsonProperty("content_length")
    private final int contentLength;

    public SendImageCommand(String channel, String source, int width, int height, int thumbnailContentLength, int contentLength) {
        super("send_image");
        this.channel = channel;
        this.source = source;
        this.width = width;
        this.height = height;
        this.thumbnailContentLength = thumbnailContentLength;
        this.contentLength = contentLength;
    }

}