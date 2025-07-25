package io.github.ceakins.zello.model.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents the server's response to a "logon" command. This is typically only
 * received if the logon fails. A successful logon triggers an OnChannelStatusEvent.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogonResultEvent extends ServerCommand {

    /**
     * Indicates whether the logon was successful. Will be `false` for an error.
     */
    private boolean success;

    /**
     * A descriptive error message if the logon failed.
     */
    private String error;

}