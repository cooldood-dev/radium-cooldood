package vip.radium;

import com.thealtening.auth.service.AlteningServiceType;
import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import io.github.nevalackin.homoBus.bus.impl.EventBus;
import vip.radium.alt.AltManager;
import vip.radium.command.CommandManager;
import vip.radium.config.ConfigManager;
import vip.radium.event.Event;
import vip.radium.event.impl.game.ClientStartupEvent;
import vip.radium.player.PlayerManager;
import vip.radium.gui.csgo.SkeetUI;
import vip.radium.gui.font.FontManager;
import vip.radium.module.ModuleManager;
import vip.radium.notification.NotificationManager;
import vip.radium.utils.RadiumLogger;

import static org.lwjgl.opengl.GL11.*;

public final class RadiumClient {
    private static final RadiumClient INSTANCE = new RadiumClient();
    private EventBus<Event> eventBus;
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private PlayerManager playerManager;
    private NotificationManager notificationManager;
    private CommandManager commandManager;
    private AltManager altManager;

    public static final String NAME = "Radium";
    public static final String VERSION = "v1.4.0";
    /** Name of the config that is automatically saved on shutdown and loaded on startup. */
    public static final String AUTO_CONFIG_NAME = "autosave";

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public AltManager getAltManager() {
        return altManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CommandManager getCommandHandler() {
        return commandManager;
    }

    private RadiumClient() {
        getEventBus().subscribe(this);
    }

    @EventLink
    public final Listener<ClientStartupEvent> onClientStart = e -> {
        // Initialise logger first so all subsequent init steps are captured
        RadiumLogger.init();
        RadiumLogger.info("Starting " + NAME + " " + VERSION);

        setGLHints();
        FontManager.initTextures();

        configManager = new ConfigManager();
        RadiumLogger.info("ConfigManager initialised");

        playerManager = new PlayerManager();

        altManager = new AltManager();
        RadiumLogger.info("AltManager initialised, " + altManager.getElements().size() + " alt(s) loaded");

        moduleManager = new ModuleManager();
        RadiumLogger.info("ModuleManager initialised, " + moduleManager.getModules().size() + " module(s) loaded");

        SkeetUI.init();

        notificationManager = new NotificationManager();

        commandManager = new CommandManager();

        moduleManager.postInit();

        // Auto-load the persistent config if it exists
        if (configManager.findConfig(AUTO_CONFIG_NAME) != null) {
            boolean loaded = configManager.loadConfig(AUTO_CONFIG_NAME);
            RadiumLogger.info("Auto-loaded config '" + AUTO_CONFIG_NAME + "': " + loaded);
        }

        // Shutdown hook: auto-save config + flush logger
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            altManager.getAlteningAuth().updateService(AlteningServiceType.MOJANG);

            // Auto-save all module states and settings
            boolean saved = configManager.saveConfig(AUTO_CONFIG_NAME);
            RadiumLogger.info("Auto-saved config '" + AUTO_CONFIG_NAME + "': " + saved);

            altManager.saveAlts();
            RadiumLogger.info("Alts saved on shutdown");

            RadiumLogger.info(NAME + " shutdown complete");
            RadiumLogger.close();
        }));

        RadiumLogger.info(NAME + " " + VERSION + " fully initialised");
    };

    private static void setGLHints() {
        glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
    }

    public EventBus<Event> getEventBus() {
        if (eventBus == null) {
            eventBus = new EventBus<>();
        }
        return eventBus;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public static RadiumClient getInstance() {
        return INSTANCE;
    }
}
