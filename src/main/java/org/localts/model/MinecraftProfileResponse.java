package org.localts.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MinecraftProfileResponse {

    private String username;
    private UUID uuid;
    private Set<String> capes;

    public MinecraftProfileResponse() {
    }

    public static MinecraftProfileResponse fromJson(JsonObject json) {
        MinecraftProfileResponse profile = new MinecraftProfileResponse();

        profile.setUsername(json.get("name").getAsString());
        profile.setUuid(UUID.fromString(dashedUUID(json.get("id").getAsString())));

        if (json.has("capes") && json.get("capes").isJsonArray()) {
            Set<String> capeSet = new HashSet<>();
            JsonArray capesArray = json.get("capes").getAsJsonArray();
            for (JsonElement element : capesArray) {
                if (element.isJsonObject() && element.getAsJsonObject().has("alias")) {
                    capeSet.add(element.getAsJsonObject().get("alias").getAsString());
                }
            }
            profile.setCapes(capeSet);
        }

        return profile;
    }

    private static String dashedUUID(String input) {
        return input.replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Set<String> getCapes() {
        return capes;
    }

    public void setCapes(Set<String> capes) {
        this.capes = capes;
    }
}
