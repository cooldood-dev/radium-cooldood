package org.localts.request;

import com.google.gson.JsonObject;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;
import org.localts.exception.AuthenticationException;
import org.localts.model.MicrosoftTokenResponse;

import java.io.IOException;

import static org.localts.RefreshTokenAuthentication.CLIENT;
import static org.localts.RefreshTokenAuthentication.GSON;

public final class RefreshTokenRequest {

    private static final String REFRESH_TOKENS_URL = "https://login.live.com/oauth20_token.srf";

    private static final String CLIENT_ID = "00000000402b5328";
    private static final String REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";
    private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";

    public static MicrosoftTokenResponse refreshToken(String refreshToken) throws IOException, AuthenticationException {
        FormBody payload = new FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("grant_type", "refresh_token")
                .add("redirect_uri", REDIRECT_URI)
                .add("refresh_token", refreshToken)
                .add("scope", SCOPE)
                .build();

        Request request = new Request.Builder()
                .post(payload)
                .url(REFRESH_TOKENS_URL)
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.code() >= 500) {
                throw new AuthenticationException("Microsoft services are unavailable");
            }

            if (response.body() == null) {
                throw new AuthenticationException("Received empty response body when trying to refresh oauth tokens");
            }

            JsonObject json = GSON.fromJson(response.body().string(), JsonObject.class);

            if (json == null) {
                throw new AuthenticationException(String.format("Received no response when trying to refresh oauth tokens (code %s)", response.code()));
            }

            MicrosoftTokenResponse microsoftResponse = MicrosoftTokenResponse.fromJson(json);

            if (!microsoftResponse.isSuccessful()) {
                throw new AuthenticationException("Received an error while refreshing oauth tokens: " + microsoftResponse.getError());
            }

            return microsoftResponse;
        }
    }

}
