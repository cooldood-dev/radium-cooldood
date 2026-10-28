package vip.radium.utils;

import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import com.mojang.util.UUIDTypeAdapter;
import com.thealtening.auth.service.AlteningServiceType;
import vip.radium.RadiumClient;
import vip.radium.alt.Alt;
import vip.radium.alt.AltManager;
import vip.radium.gui.alt.impl.GuiAltManager;
import vip.radium.notification.Notification;
import vip.radium.notification.NotificationType;
import net.minecraft.util.Session;
import java.net.Proxy;

public final class SessionUtils {

    public static void switchService(AlteningServiceType service) {
        RadiumClient.getInstance().getAltManager().getAlteningAuth().updateService(service);
    }

    public static Alt importFromClipboard() {
        return AltManager.parseAlt(StringUtils.getTrimmedClipboardContents());
    }

    public static void logInWithRefreshToken(String refreshToken, Alt alt) {
        new Thread(() -> {
            try {
                GuiAltManager.status = "\247eLogging in with Refresh Token...";
                RadiumClient.getInstance().getNotificationManager().add(new Notification("Logging In", 1000, NotificationType.INFO));
                RadiumLogger.info("Logging in with refresh token...");

                org.localts.model.MinecraftTokenResponse mcToken =
                        org.localts.RefreshTokenAuthentication.authenticateWithRefreshToken(refreshToken);
                org.localts.model.MinecraftProfileResponse profile =
                        org.localts.RefreshTokenAuthentication.getMinecraftProfile(mcToken);

                String username = profile.getUsername();
                String uuidString = profile.getUuid().toString().replace("-", "");
                String accessToken = mcToken.getAccessToken();

                Wrapper.getMinecraft().session = new Session(username, uuidString, accessToken, "mojang");

                if (alt != null) {
                    alt.setUsername(username);
                    alt.setWorking(true);
                    // Persist the refresh-token alt back to alts.txt so it survives restarts
                    RadiumClient.getInstance().getAltManager().saveAlts();
                    RadiumLogger.info("Refresh-token alt saved: " + username);
                }

                RadiumClient.getInstance().getNotificationManager().add(new Notification("Login Success", "Logged into " + username, NotificationType.SUCCESS));
                GuiAltManager.status = "\247aLogged in (" + username + ").";
                RadiumLogger.info("Logged in successfully as: " + username);
            } catch (Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "Failed";
                GuiAltManager.status = "\247c" + errorMsg;
                RadiumClient.getInstance().getNotificationManager().add(new Notification("Login Failed", errorMsg, 2000, NotificationType.ERROR));
                RadiumLogger.error("Refresh-token login failed: " + errorMsg);
                if (alt != null) {
                    alt.setWorking(false);
                }
            }
        }).start();
    }

    public static void logInCracked(String username) {
        Wrapper.getMinecraft().session = new Session(username, username, "0", "legacy");
        GuiAltManager.status = "\247aLogged in as cracked (" + username + ").";
        RadiumClient.getInstance().getNotificationManager().add(
                new Notification("Cracked Login", "Username set to " + username, NotificationType.SUCCESS)
        );
        RadiumLogger.info("Logged into cracked account: " + username);
    }

    public static void logIn(Alt alt) {
        if (alt.isRefreshToken()) {
            logInWithRefreshToken(alt.getRefreshToken(), alt);
            return;
        }

        if (alt.getPassword() == null || alt.getPassword().isEmpty()) {
            logInCracked(alt.getUsername() != null && !alt.getUsername().isEmpty() ? alt.getUsername() : alt.getEmail());
            return;
        }

        new Thread(() -> {
            try {
                GuiAltManager.status = "\247eLogging in...";
                RadiumClient.getInstance().getNotificationManager().add(new Notification("Logging In", 1000, NotificationType.INFO));
                RadiumLogger.info("Logging in with email/password alt...");
                YggdrasilUserAuthentication userAuthentication =
                        (YggdrasilUserAuthentication) new YggdrasilAuthenticationService(Proxy.NO_PROXY, "")
                                .createUserAuthentication(Agent.MINECRAFT);
                userAuthentication.setUsername(alt.getEmail());
                userAuthentication.setPassword(alt.getPassword());
                userAuthentication.logIn();
                String username = userAuthentication.getSelectedProfile().getName();
                alt.setUsername(username);
                alt.setWorking(true);
                Wrapper.getMinecraft().session = new Session(username,
                        UUIDTypeAdapter.fromUUID(userAuthentication.getSelectedProfile().getId()),
                        userAuthentication.getAuthenticatedToken(),
                        userAuthentication.getUserType().getName());
                // Persist any username change
                RadiumClient.getInstance().getAltManager().saveAlts();
                RadiumClient.getInstance().getNotificationManager().add(new Notification("Login Success", "Logged into account " + username, NotificationType.SUCCESS));
                GuiAltManager.status = "\247aLogged in (" + username + ").";
                RadiumLogger.info("Logged in successfully as: " + username);
            } catch (AuthenticationException e) {
                GuiAltManager.status = "\247cFailed.";
                RadiumClient.getInstance().getNotificationManager().add(new Notification("Login Failed", 1000, NotificationType.ERROR));
                RadiumLogger.error("Alt login failed: " + (e.getMessage() != null ? e.getMessage() : "AuthenticationException"));
                alt.setWorking(false);
            }
        }).start();
    }
}
