package com.kousenit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serializes and deserializes Opera objects to/from JSON.
 * Used by Claude Code team agents to pass the Opera between pipeline steps.
 */
public class OperaSerializer {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static void save(Opera opera, Path jsonPath) throws IOException {
        Files.createDirectories(jsonPath.getParent());
        Files.writeString(jsonPath, GSON.toJson(opera));
    }

    public static Opera load(Path jsonPath) throws IOException {
        String json = Files.readString(jsonPath);
        return GSON.fromJson(json, Opera.class);
    }
}
