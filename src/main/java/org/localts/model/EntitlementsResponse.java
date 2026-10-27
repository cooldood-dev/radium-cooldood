package org.localts.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.localts.exception.AuthenticationException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EntitlementsResponse {

    private static final Set<String> SOURCES = new HashSet<>(Arrays.asList("GAMEPASS", "PURCHASE", "MC_PURCHASE"));
    private Map<String, String> entitlements;

    public EntitlementsResponse() {
    }

    public static EntitlementsResponse fromJson(JsonObject json) {
        EntitlementsResponse entitlementsResponse = new EntitlementsResponse();

        if (!json.has("items")) {
            throw new AuthenticationException("Couldn't receive entitlements");
        }

        Map<String, String> map = new HashMap<>();
        JsonArray items = json.getAsJsonArray("items");
        for (JsonElement element : items) {
            JsonObject obj = element.getAsJsonObject();
            map.put(obj.get("name").getAsString(), obj.get("source").getAsString());
        }
        entitlementsResponse.setEntitlements(map);

        return entitlementsResponse;
    }

    public boolean checkOwnership() {
        if (entitlements == null) return false;
        for (Map.Entry<String, String> entry : entitlements.entrySet()) {
            if (entry.getKey().contains("minecraft") && SOURCES.contains(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    public Map<String, String> getEntitlements() {
        return entitlements;
    }

    public void setEntitlements(Map<String, String> entitlements) {
        this.entitlements = entitlements;
    }
}
