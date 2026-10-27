package org.localts.model;

import com.google.gson.JsonObject;
import org.localts.exception.AuthenticationException;

public class MicrosoftTokenResponse {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String error;
    private String description;

    public MicrosoftTokenResponse() {
    }

    public static MicrosoftTokenResponse fromJson(JsonObject json) {
        MicrosoftTokenResponse response = new MicrosoftTokenResponse();

        if (json.has("error")) {
            response.setError(json.get("error").getAsString());

            if (json.has("description")) {
                response.setDescription(json.get("description").getAsString());
            }
        } else if (json.has("access_token") && json.has("refresh_token") && json.has("expires_in")) {
            response.setAccessToken(json.get("access_token").getAsString());
            response.setRefreshToken(json.get("refresh_token").getAsString());
            response.setExpiresIn(json.get("expires_in").getAsLong());
        } else {
            throw new AuthenticationException("Received invalid JSON object while trying to refresh oauth tokens");
        }

        return response;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getError() {
        if (description != null) {
            return String.format("%s (%s)", error, description);
        }

        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSuccessful() {
        return error == null;
    }
}
