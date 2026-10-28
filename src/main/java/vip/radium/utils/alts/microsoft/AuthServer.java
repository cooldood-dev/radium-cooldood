package vip.radium.utils.alts.microsoft;

import com.sun.net.httpserver.HttpServer;
import vip.radium.RadiumClient;
import vip.radium.alt.Alt;
import vip.radium.gui.alt.impl.GuiAltManager;
import vip.radium.notification.Notification;
import vip.radium.notification.NotificationType;
import vip.radium.utils.ClipboardUtils;
import vip.radium.utils.RadiumLogger;
import vip.radium.utils.Wrapper;

import java.awt.*;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthServer {
    public static final String URL = "https://login.live.com/oauth20_authorize.srf" +
            "?client_id=" + MSAuth.clientId +
            "&response_type=code" +
            "&scope=XboxLive.signin%20XboxLive.offline_access" +
            "&prompt=select_account" +
            "&redirect_uri=http://localhost:" + MSAuth.port + "/";

    private static final Pattern codePattern = Pattern.compile("code=([^&]+)");
    private static HttpServer serverInstance;
    private static final Object lock = new Object();

    public static void startAndOpen() {
        // Copy login URL to clipboard (like in ScaleHack)
        ClipboardUtils.setClipboardContents(URL);
        RadiumClient.getInstance().getNotificationManager().add(new Notification(
                "Microsoft Login",
                "Login link copied to clipboard!",
                3000,
                NotificationType.INFO
        ));

        // Open in default browser
        try {
            Desktop.getDesktop().browse(URI.create(URL));
        } catch (Throwable t) {
            RadiumLogger.warn("Could not automatically open browser: " + t.getMessage());
        }

        // Start listening on callback server if not already running
        startServer();
    }

    public static void startServer() {
        synchronized (lock) {
            if (serverInstance != null) {
                return;
            }

            new Thread(() -> {
                try {
                    int portNumber = Integer.parseInt(MSAuth.port);
                    HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", portNumber), 0);
                    s.createContext("/", exchange -> {
                        String query = exchange.getRequestURI().getQuery();
                        String responseText = "<html><body style='font-family:sans-serif;text-align:center;padding-top:50px;background:#1e1e1e;color:#fff;'>"
                                + "<h2>Microsoft Authentication Successful!</h2>"
                                + "<p>You can close this tab and return to Minecraft.</p>"
                                + "</body></html>";
                        byte[] responseBytes = responseText.getBytes("UTF-8");
                        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                        exchange.sendResponseHeaders(200, responseBytes.length);
                        OutputStream out = exchange.getResponseBody();
                        out.write(responseBytes);
                        out.flush();
                        out.close();

                        if (query != null) {
                            Matcher m = codePattern.matcher(query);
                            if (m.find()) {
                                String code = m.group(1);
                                handleAuthCode(code);
                            }
                        }
                    });
                    s.start();
                    serverInstance = s;
                    RadiumLogger.info("AuthServer started on port " + portNumber);
                } catch (Exception ex) {
                    RadiumLogger.error("Failed to start AuthServer: " + ex.getMessage());
                }
            }, "MSAuth-Server").start();
        }
    }

    private static void handleAuthCode(String code) {
        new Thread(() -> {
            try {
                GuiAltManager.status = "\247eLogging into Microsoft account...";
                RadiumClient.getInstance().getNotificationManager().add(new Notification(
                        "Microsoft Login",
                        "Logging in with code...",
                        2000,
                        NotificationType.INFO
                ));

                MSAuth.MSASession msaSession = MSAuth.authWithCode(code);
                Wrapper.getMinecraft().session = msaSession.toMinecraftSession();

                if (msaSession.refreshToken != null && !msaSession.refreshToken.isEmpty()) {
                    Alt alt = new Alt(msaSession.refreshToken);
                    alt.setUsername(msaSession.username);
                    alt.setWorking(true);
                    RadiumClient.getInstance().getAltManager().addAlt(alt);
                }

                String successMsg = "Logged into " + msaSession.username;
                GuiAltManager.status = "\247aLogged in (" + msaSession.username + ").";
                RadiumClient.getInstance().getNotificationManager().add(new Notification(
                        "Login Success",
                        successMsg,
                        3000,
                        NotificationType.SUCCESS
                ));
                RadiumLogger.info(successMsg);
            } catch (Exception ex) {
                String errMsg = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                GuiAltManager.status = "\247cLogin failed: " + errMsg;
                RadiumClient.getInstance().getNotificationManager().add(new Notification(
                        "Microsoft Login Failed",
                        errMsg,
                        3000,
                        NotificationType.ERROR
                ));
                RadiumLogger.error("MSAuth failed: " + errMsg);
            }
        }, "MSAuth-Worker").start();
    }
}

