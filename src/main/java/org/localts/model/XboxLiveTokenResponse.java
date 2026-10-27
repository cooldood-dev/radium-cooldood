package org.localts.model;

import com.google.gson.JsonObject;
import org.localts.exception.AuthenticationException;

public class XboxLiveTokenResponse {

    private String token;
    private String userHash;

    public XboxLiveTokenResponse() {
    }

    public static XboxLiveTokenResponse fromJson(JsonObject json) {
        XboxLiveTokenResponse xboxLiveResponse = new XboxLiveTokenResponse();

        if (!json.has("Token") || !json.has("DisplayClaims")) {
            throw new AuthenticationException("Missing Token or DisplayClaims when trying to get Xbox live token");
        }

        xboxLiveResponse.setToken(json.get("Token").getAsString());
        xboxLiveResponse.setUserHash(
                json.get("DisplayClaims").getAsJsonObject()
                        .get("xui").getAsJsonArray()
                        .get(0).getAsJsonObject()
                        .get("uhs").getAsString()
        );

        return xboxLiveResponse;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserHash() {
        return userHash;
    }

    public void setUserHash(String userHash) {
        this.userHash = userHash;
    }
}
