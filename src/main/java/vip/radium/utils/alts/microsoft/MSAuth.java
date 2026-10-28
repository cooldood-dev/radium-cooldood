package vip.radium.utils.alts.microsoft;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.Session;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

public class MSAuth {
    private static final Gson gson = new Gson();

    public static final String clientId = "54fd49e4-2103-4044-9603-2b028c814ec3";
    public static final String port = "59125";
    private static final String scope = "XboxLive.signin XboxLive.offline_access";

    public static AccessRefreshToken authCode(String code) throws Exception {
        HttpPost req = new HttpPost("https://login.live.com/oauth20_token.srf");
        req.addHeader("Content-Type", "application/x-www-form-urlencoded");

        ArrayList<NameValuePair> payload = new ArrayList<>();
        payload.add(new BasicNameValuePair("client_id", clientId));
        payload.add(new BasicNameValuePair("code", code));
        payload.add(new BasicNameValuePair("grant_type", "authorization_code"));
        payload.add(new BasicNameValuePair("redirect_uri", "http://localhost:" + port + "/"));
        payload.add(new BasicNameValuePair("scope", scope));
        req.setEntity(new UrlEncodedFormEntity(payload));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(req)) {
            JsonElement res = gson.fromJson(EntityUtils.toString(response.getEntity()), JsonElement.class);
            String accessToken = res.getAsJsonObject().get("access_token").getAsString();
            String refreshToken = res.getAsJsonObject().get("refresh_token").getAsString();
            return new AccessRefreshToken(accessToken, refreshToken);
        }
    }

    public static AccessRefreshToken refreshToken(String refreshToken) throws Exception {
        HttpPost req = new HttpPost("https://login.live.com/oauth20_token.srf");
        req.addHeader("Content-Type", "application/x-www-form-urlencoded");

        ArrayList<NameValuePair> payload = new ArrayList<>();
        payload.add(new BasicNameValuePair("client_id", clientId));
        payload.add(new BasicNameValuePair("refresh_token", refreshToken));
        payload.add(new BasicNameValuePair("grant_type", "refresh_token"));
        payload.add(new BasicNameValuePair("redirect_uri", "http://localhost:" + port + "/"));
        req.setEntity(new UrlEncodedFormEntity(payload));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(req)) {
            JsonElement res = gson.fromJson(EntityUtils.toString(response.getEntity()), JsonElement.class);
            String accessToken = res.getAsJsonObject().get("access_token").getAsString();
            String newRefreshToken = res.getAsJsonObject().get("refresh_token").getAsString();
            return new AccessRefreshToken(accessToken, newRefreshToken);
        }
    }

    private static String authXBL(String authToken) throws Exception {
        HttpPost req = new HttpPost("https://user.auth.xboxlive.com/user/authenticate");
        req.addHeader("Content-Type", "application/json");
        req.addHeader("Accept", "application/json");

        JsonObject payload = new JsonObject();
        JsonObject payloadProps = new JsonObject();
        payloadProps.addProperty("AuthMethod", "RPS");
        payloadProps.addProperty("SiteName", "user.auth.xboxlive.com");
        payloadProps.addProperty("RpsTicket", "d=" + authToken);
        payload.add("Properties", payloadProps);
        payload.addProperty("RelyingParty", "http://auth.xboxlive.com");
        payload.addProperty("TokenType", "JWT");

        BasicHttpEntity ent = new BasicHttpEntity();
        ent.setContent(new ByteArrayInputStream(payload.toString().getBytes()));
        req.setEntity(ent);

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(req)) {
            JsonElement res = gson.fromJson(EntityUtils.toString(response.getEntity()), JsonElement.class);
            return res.getAsJsonObject().get("Token").getAsString();
        }
    }

    private static XBLTokenUhs authXSTS(String XBLToken) throws Exception {
        HttpPost req = new HttpPost("https://xsts.auth.xboxlive.com/xsts/authorize");
        req.addHeader("Content-Type", "application/json");
        req.addHeader("Accept", "application/json");

        JsonObject payload = new JsonObject();
        JsonObject payloadProps = new JsonObject();
        JsonArray userTokens = new JsonArray();
        userTokens.add(new JsonPrimitive(XBLToken));
        payloadProps.add("UserTokens", userTokens);
        payloadProps.addProperty("SandboxId", "RETAIL");
        payload.add("Properties", payloadProps);
        payload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        payload.addProperty("TokenType", "JWT");

        BasicHttpEntity ent = new BasicHttpEntity();
        ent.setContent(new ByteArrayInputStream(payload.toString().getBytes()));
        req.setEntity(ent);

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(req)) {
            JsonElement res = gson.fromJson(EntityUtils.toString(response.getEntity()), JsonElement.class);
            String token = res.getAsJsonObject().get("Token").getAsString();
            String uhs = res.getAsJsonObject()
                    .get("DisplayClaims")
                    .getAsJsonObject()
                    .get("xui")
                    .getAsJsonArray()
                    .get(0)
                    .getAsJsonObject()
                    .get("uhs")
                    .getAsString();
            return new XBLTokenUhs(token, uhs);
        }
    }

    public static String getMinecraftAccessToken(String userHashcode, String XSTSaccessToken) throws Exception {
        HttpPost req = new HttpPost("https://api.minecraftservices.com/authentication/login_with_xbox");
        req.setHeader("Content-Type", "application/json");
        req.setEntity(new StringEntity("{ \"identityToken\": \"XBL3.0 x=" + userHashcode + ";" + XSTSaccessToken + "\" }"));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(req)) {
            JsonObject res = gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
            if (res != null && res.has("access_token")) {
                return res.get("access_token").getAsString();
            }
            throw new RuntimeException("Failed to get Minecraft access token");
        }
    }

    public static Session queryPlayerProfile(String accessToken) throws Exception {
        HttpGet req = new HttpGet("https://api.minecraftservices.com/minecraft/profile");
        req.addHeader("Authorization", "Bearer " + accessToken);

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(req)) {
            JsonObject res = gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
            if (res != null && res.has("id") && res.has("name")) {
                String uuid = res.get("id").getAsString();
                String username = res.get("name").getAsString();
                return new Session(username, uuid, accessToken, "mojang");
            }
            throw new RuntimeException("Account does not own Minecraft or profile not found");
        }
    }

    public static MSASession authWithAccessRefreshToken(AccessRefreshToken accessRefreshToken) throws Exception {
        String XBLToken = authXBL(accessRefreshToken.accessToken);
        XBLTokenUhs xblTokenUhs = authXSTS(XBLToken);
        String mcAccessToken = getMinecraftAccessToken(xblTokenUhs.uhs, xblTokenUhs.XBLToken);
        Session session = queryPlayerProfile(mcAccessToken);

        return new MSASession(session.getPlayerID(), session.getUsername(), accessRefreshToken.refreshToken, session.getToken());
    }

    public static MSASession authWithCode(String code) throws Exception {
        return authWithAccessRefreshToken(authCode(code));
    }

    public static MSASession authWithRefreshToken(String refreshToken) throws Exception {
        return authWithAccessRefreshToken(refreshToken(refreshToken));
    }

    public static class AccessRefreshToken {
        public final String accessToken;
        public final String refreshToken;

        public AccessRefreshToken(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    private static class XBLTokenUhs {
        public final String XBLToken;
        public final String uhs;

        public XBLTokenUhs(String XBLToken, String uhs) {
            this.XBLToken = XBLToken;
            this.uhs = uhs;
        }
    }

    public static class MSASession {
        public final String uuid;
        public final String username;
        public final String refreshToken;
        public final String accessToken;

        public MSASession(String uuid, String username, String refreshToken, String accessToken) {
            this.uuid = uuid;
            this.username = username;
            this.refreshToken = refreshToken;
            this.accessToken = accessToken;
        }

        public Session toMinecraftSession() {
            return new Session(username, uuid, accessToken, "mojang");
        }
    }
}

