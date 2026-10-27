package org.localts;

import com.google.gson.Gson;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.localts.exception.AuthenticationException;
import org.localts.model.EntitlementsResponse;
import org.localts.model.MinecraftProfileResponse;
import org.localts.model.MinecraftTokenResponse;
import org.localts.model.XboxLiveTokenResponse;
import org.localts.request.EntitlementsRequest;
import org.localts.request.MinecraftProfileRequest;
import org.localts.request.MinecraftTokenRequest;
import org.localts.request.RefreshTokenRequest;
import org.localts.request.XboxLiveTokenRequest;
import org.localts.request.XstsTokenRequest;

import java.io.IOException;

public class RefreshTokenAuthentication {

    public static final Gson GSON = new Gson();
    public static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .followSslRedirects(false)
            .followRedirects(false)
            .addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Response response = chain.proceed(chain.request());

                    if (response.code() == 429) {
                        response.close();
                        throw new AuthenticationException("You are rate limited, try again in a moment!");
                    }

                    return response;
                }
            })
            .build();

    /* OAuth refresh -> OAuth access -> XBL -> XSTS -> MC */
    public static MinecraftTokenResponse authenticateWithRefreshToken(String refreshToken) throws IOException, AuthenticationException {
        XboxLiveTokenResponse xboxLiveResponse = XboxLiveTokenRequest.getXboxLiveToken(
                RefreshTokenRequest.refreshToken(refreshToken).getAccessToken()
        );

        return MinecraftTokenRequest.getMinecraftAccessToken(
                XstsTokenRequest.getXstsToken(xboxLiveResponse.getToken()).getToken(),
                xboxLiveResponse.getUserHash()
        );
    }

    /* MC -> Check game ownership -> Fetch profile */
    public static MinecraftProfileResponse getMinecraftProfile(MinecraftTokenResponse minecraftTokenResponse) throws IOException, AuthenticationException {
        EntitlementsResponse entitlements = EntitlementsRequest.getEntitlements(minecraftTokenResponse.getAccessToken());

        if (!entitlements.checkOwnership()) {
            throw new AuthenticationException("Account doesn't own Minecraft!");
        }

        return MinecraftProfileRequest.getMinecraftProfile(minecraftTokenResponse.getAccessToken());
    }

}
