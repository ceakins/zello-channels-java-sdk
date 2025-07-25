package io.github.ceakins.zello.model.commands;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ceakins.zello.ZelloChannelConfig;
import lombok.Getter;

/**
 * Represents the "logon" command sent to the Zello server to authenticate and join a channel.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // Don't include null fields in JSON
public class LogonCommand extends Command {

    @JsonProperty("auth_token")
    private final String authToken;

    private final String username;
    private final String password;
    private final String channel;

    public LogonCommand(ZelloChannelConfig config) {
        super("logon");
        this.authToken = config.getAuthToken();
        this.username = config.getUsername();
        this.password = config.getPassword();
        this.channel = config.getChannel();
    }

}