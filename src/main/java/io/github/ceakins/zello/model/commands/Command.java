package io.github.ceakins.zello.model.commands;

import lombok.Getter;

/**
 * Base class for all commands sent to the Zello server.
 */
@Getter
public abstract class Command {

    /**
     * The name of the command, e.g., "logon", "start_stream".
     * This field is required in every command's JSON output.
     */
    private final String command;

    /**
     * A sequence number for the command. This helps in matching responses to requests.
     */
    private int seq;

    protected Command(String command) {
        this.command = command;
    }

    public void setSequence(int seq) {
        this.seq = seq;
    }

}