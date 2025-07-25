package io.github.ceakins.zello.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ceakins.zello.model.commands.Command;
import io.github.ceakins.zello.model.events.ServerCommand;

/**
 * Internal utility class for handling JSON serialization and deserialization.
 */
public class JsonUtils {

    // A single, reusable ObjectMapper is efficient.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Serializes a command object into a JSON string.
     *
     * @param command The command to serialize.
     * @return The JSON string representation of the command.
     * @throws JsonProcessingException if serialization fails.
     */
    public static String commandToJson(Command command) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(command);
    }

    /**
     * Deserializes a JSON string into a ServerCommand object.
     * <p>
     * This relies on the polymorphic type information configured on the ServerCommand class
     * to return the correct subclass (e.g., OnStreamStartEvent, OnTextMessageEvent).
     *
     * @param json The JSON string received from the server.
     * @return An instance of a ServerCommand subclass.
     * @throws JsonProcessingException if deserialization fails.
     */
    public static ServerCommand jsonToServerCommand(String json) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, ServerCommand.class);
    }

}