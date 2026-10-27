package org.localts.request;

import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.localts.exception.AuthenticationException;
import org.localts.model.MinecraftTokenResponse;

import java.io.IOException;

import static org.localts.RefreshTokenAuthentication.CLIENT;
import static org.localts.RefreshTokenAuthentication.GSON;

public final class MinecraftTokenRequest {

    private static final String LOGIN_WITH_XBOX_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";

    public static MinecraftTokenResponse getMinecraftAccessToken(String xstsToken, String userHash) throws IOException, AuthenticationException {
        JsonObject payload = new JsonObject();

        payload.addProperty("identityToken", String.format("XBL3.0 x=%s;%s", userHash, xstsToken));

        Request request = new Request.Builder()
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), payload.toString()))
                .url(LOGIN_WITH_XBOX_URL)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.code() >= 500) {
                throw new AuthenticationException("Xbox services are unavailable (login_with_xbox)");
            }

            if (response.body() == null) {
                throw new AuthenticationException("Empty body received when fetching Minecraft access token");
            }

            MinecraftTokenResponse minecraftResponse = MinecraftTokenResponse.fromJson(GSON.fromJson(response.body().string(), JsonObject.class));

            if (!minecraftResponse.isSuccessful()) {
                throw new AuthenticationException("Received an error while trying to get Minecraft access token: " + minecraftResponse.getError());
            }

            return minecraftResponse;
        }
    }

}
