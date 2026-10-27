package org.localts.request;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.localts.exception.AuthenticationException;
import org.localts.model.XstsTokenResponse;

import java.io.IOException;

import static org.localts.RefreshTokenAuthentication.CLIENT;
import static org.localts.RefreshTokenAuthentication.GSON;

public final class XstsTokenRequest {

    private static final String GET_XSTS_TOKEN_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";

    public static XstsTokenResponse getXstsToken(String accessToken) throws IOException, AuthenticationException {
        JsonObject payload = new JsonObject();
        JsonObject properties = new JsonObject();
        JsonArray userTokens = new JsonArray();

        properties.addProperty("SandboxId", "RETAIL");
        userTokens.add(new com.google.gson.JsonPrimitive(accessToken));
        properties.add("UserTokens", userTokens);

        payload.add("Properties", properties);
        payload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        payload.addProperty("TokenType", "JWT");

        Request request = new Request.Builder()
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), payload.toString()))
                .url(GET_XSTS_TOKEN_URL)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.code() >= 500) {
                throw new AuthenticationException("Xbox services are unavailable (XSTS)");
            }

            if (response.body() == null) {
                throw new AuthenticationException("Empty body received when fetching XSTS token");
            }

            XstsTokenResponse xstsResponse = XstsTokenResponse.fromJson(GSON.fromJson(response.body().string(), JsonObject.class));

            if (!xstsResponse.isSuccessful()) {
                throw new AuthenticationException("Received an error while getting XSTS Token: " + xstsResponse.getError());
            }

            return xstsResponse;
        }
    }

}
