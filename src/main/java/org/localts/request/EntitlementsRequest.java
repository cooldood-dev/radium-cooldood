package org.localts.request;

import com.google.gson.JsonObject;
import okhttp3.Request;
import okhttp3.Response;
import org.localts.exception.AuthenticationException;
import org.localts.model.EntitlementsResponse;

import java.io.IOException;

import static org.localts.RefreshTokenAuthentication.CLIENT;
import static org.localts.RefreshTokenAuthentication.GSON;

public final class EntitlementsRequest {

    private static final String MINECRAFTSERVICES_PRODUCTS_URL = "https://api.minecraftservices.com/entitlements/license?requestId=auth";

    public static EntitlementsResponse getEntitlements(String accessToken) throws IOException, AuthenticationException {
        Request request = new Request.Builder()
                .url(MINECRAFTSERVICES_PRODUCTS_URL)
                .header("Authorization", String.format("Bearer %s", accessToken))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.code() >= 500) {
                throw new AuthenticationException("Minecraft services are unavailable");
            }

            if (response.code() != 200) {
                throw new AuthenticationException("Received code " + response.code() + " when trying to check game ownership");
            }

            if (response.body() == null) {
                throw new AuthenticationException("Empty body received when fetching entitlements");
            }

            return EntitlementsResponse.fromJson(GSON.fromJson(response.body().string(), JsonObject.class));
        }
    }

}
