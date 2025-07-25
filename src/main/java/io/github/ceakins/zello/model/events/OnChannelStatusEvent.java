package io.github.ceakins.zello.model.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Fired when the channel status changes (e.g., you go online or offline)
 * or as part of a successful logon sequence.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnChannelStatusEvent extends ServerCommand {

    /**
     * The current status of the channel, e.g., "online".
     */
    private String status;

    /**
     * A list of users currently in the channel.
     */
    private List<ChannelUser> users;

}