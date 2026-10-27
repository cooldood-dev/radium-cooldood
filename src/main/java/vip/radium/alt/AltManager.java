package vip.radium.alt;

import com.thealtening.api.retriever.BasicDataRetriever;
import com.thealtening.auth.TheAlteningAuthentication;
import vip.radium.RadiumClient;
import vip.radium.utils.FileUtils;
import vip.radium.utils.handler.Manager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AltManager extends Manager<Alt> {

    public static final File ALT_FILE = new File(RadiumClient.NAME, "alts.txt");
    public static final File ALTENING_TOKEN_FILE = new File(RadiumClient.NAME, "token.txt");
    private final BasicDataRetriever alteningAltFetcher;
    private final TheAlteningAuthentication alteningAuth;
    private String apiKey = "api-0000-0000-0000";
    public AltManager() {
        this.alteningAuth = TheAlteningAuthentication.mojang();
        this.alteningAltFetcher = new BasicDataRetriever(apiKey);

        if (!ALT_FILE.exists()) {
            try {
                boolean ignored = ALT_FILE.createNewFile();
            } catch (IOException ignored) {
            }
        }

        Set<Alt> alts = new HashSet<>();

        for (String line : FileUtils.getLines(ALT_FILE))
            alts.add(parseAlt(line));

        getElements().addAll(alts);

        if (!ALTENING_TOKEN_FILE.exists()) {
            try {
                boolean ignored = ALTENING_TOKEN_FILE.createNewFile();
            } catch (IOException ignored) {
            }
        } else {
            List<String> lines = FileUtils.getLines(ALTENING_TOKEN_FILE);
            if (!lines.isEmpty())
                this.setAPIKey(lines.get(0));
        }
    }

    public static Alt parseAlt(String line) {
        if (line == null) return null;
        line = line.trim();
        if (line.isEmpty()) return null;
        if (line.startsWith("token:") || line.startsWith("refresh:")) {
            String token = line.substring(line.indexOf(':') + 1).trim();
            if (!token.isEmpty())
                return new Alt(token);
        }
        String[] userPass = line.split(":", 2);
        if (userPass.length == 2)
            return new Alt(userPass[0], userPass[1]);
        if (!line.contains(":"))
            return new Alt(line);
        return null;
    }

    public void addAlt(Alt alt) {
        getElements().add(alt);
        saveAlts();
    }

    public void saveAlts() {
        try {
            if (!ALT_FILE.exists()) {
                ALT_FILE.createNewFile();
            }
            try (FileWriter fileWriter = new FileWriter(ALT_FILE)) {
                for (Alt alt : getElements()) {
                    if (alt.isRefreshToken()) {
                        fileWriter.write("token:" + alt.getRefreshToken() + System.lineSeparator());
                    } else {
                        fileWriter.write(alt.getEmail() + ":" + alt.getPassword() + System.lineSeparator());
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    public BasicDataRetriever getAlteningAltFetcher() {
        return alteningAltFetcher;
    }

    public TheAlteningAuthentication getAlteningAuth() {
        return alteningAuth;
    }

    public String getAPIKey() {
        return apiKey;
    }

    public void setAPIKey(String apiKey) {
        this.apiKey = apiKey;
        this.alteningAltFetcher.updateKey(apiKey);
    }

    public void saveAPIKey(String apiKey) {
        if (apiKey == null) return;
        setAPIKey(apiKey);

        if (!ALTENING_TOKEN_FILE.exists()) {
            try {
                if (!ALTENING_TOKEN_FILE.createNewFile())
                    return;
            } catch (IOException ignored) {
                return;
            }
        }

        try {
            FileWriter fileWriter = new FileWriter(ALTENING_TOKEN_FILE);
            fileWriter.write(apiKey);
            fileWriter.close();
        } catch (IOException ignored) {
        }
    }
}
