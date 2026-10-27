package org.localts.request;

import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.localts.exception.AuthenticationException;
import org.localts.model.XboxLiveTokenResponse;

import java.io.IOException;

import static org.localts.RefreshTokenAuthentication.CLIENT;
import static org.localts.RefreshTokenAuthentication.GSON;

public final class XboxLiveTokenRequest {

    private static final String GET_XBL_TOKEN_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XBL_RP = "http://auth.xboxlive.com";

    public static XboxLiveTokenResponse getXboxLiveToken(String accessToken) throws IOException, AuthenticationException {
        JsonObject payload = new JsonObject();
        JsonObject properties = new JsonObject();

        payload.add("Properties", properties);

        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", String.format("t=%s", accessToken));

        payload.addProperty("RelyingParty", XBL_RP);
        payload.addProperty("TokenType", "JWT");

        Request request = new Request.Builder()
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), payload.toString()))
                .url(GET_XBL_TOKEN_URL)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.code() >= 500) {
                throw new AuthenticationException("Xbox services are unavailable (XBL)");
            }

            if (response.code() == 401) {
                throw new AuthenticationException("OAuth access token is invalid");
            }

            if (response.body() == null) {
                throw new AuthenticationException("Empty body received when fetching Xbox live token");
            }

            return XboxLiveTokenResponse.fromJson(GSON.fromJson(response.body().string(), JsonObject.class));
        }
    }

}
