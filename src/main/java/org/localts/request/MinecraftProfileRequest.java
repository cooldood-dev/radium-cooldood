package org.localts.request;

import com.google.gson.JsonObject;
import okhttp3.Request;
import okhttp3.Response;
import org.localts.exception.AuthenticationException;
import org.localts.model.MinecraftProfileResponse;

import java.io.IOException;

import static org.localts.RefreshTokenAuthentication.CLIENT;
import static org.localts.RefreshTokenAuthentication.GSON;

public final class MinecraftProfileRequest {

    private static final String API_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    public static MinecraftProfileResponse getMinecraftProfile(String accessToken) throws IOException, AuthenticationException {
        Request request = new Request.Builder()
                .url(API_PROFILE_URL)
                .header("Authorization", String.format("Bearer %s", accessToken))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.code() == 404 || response.code() == 400) {
                throw new AuthenticationException("Profile not found! (The username is most likely unset)");
            }

            if (response.code() >= 500) {
                throw new AuthenticationException("Minecraft services are unavailable");
            }

            if (response.code() != 200) {
                throw new AuthenticationException("Invalid response from Minecraft services (code: " + response.code() + ")");
            }

            if (response.body() == null) {
                throw new AuthenticationException("Empty body received when fetching Minecraft profile");
            }

            return MinecraftProfileResponse.fromJson(GSON.fromJson(response.body().string(), JsonObject.class));
        }
    }

}
