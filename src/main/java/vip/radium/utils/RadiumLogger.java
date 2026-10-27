package vip.radium.utils;

import vip.radium.RadiumClient;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * Radium Client logger.
 * Writes to both stdout and a daily-rolled file:
 *   .minecraft/Radium/logs/radium_YYYY-MM-DD.log
 */
public final class RadiumLogger {

    private static final Logger LOGGER = Logger.getLogger("Radium");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static FileHandler fileHandler;
    private static boolean initialized = false;

    private RadiumLogger() {}

    public static synchronized void init() {
        if (initialized) return;
        try {
            File logsDir = new File(RadiumClient.NAME, "logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }

            String logFileName = new File(logsDir,
                    "radium_" + FILE_DATE_FORMAT.format(new Date()) + ".log").getAbsolutePath();

            fileHandler = new FileHandler(logFileName, true);
            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("[%s] [Radium/%s] %s%n",
                            DATE_FORMAT.format(new Date(record.getMillis())),
                            record.getLevel().getName(),
                            record.getMessage());
                }
            });

            // Remove default handlers and set up ours
            LOGGER.setUseParentHandlers(false);
            LOGGER.setLevel(Level.ALL);
            LOGGER.addHandler(fileHandler);

            // Mirror to console as well
            ConsoleHandler ch = new ConsoleHandler();
            ch.setLevel(Level.ALL);
            ch.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("[%s] [Radium/%s] %s%n",
                            DATE_FORMAT.format(new Date(record.getMillis())),
                            record.getLevel().getName(),
                            record.getMessage());
                }
            });
            LOGGER.addHandler(ch);

            initialized = true;
            info("Radium logger initialized -> " + logFileName);
        } catch (IOException e) {
            System.err.println("[Radium] Logger init failed: " + e.getMessage());
        }
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warn(String message) {
        LOGGER.warning(message);
    }

    public static void error(String message) {
        LOGGER.severe(message);
    }

    public static void error(String message, Throwable t) {
        LOGGER.log(Level.SEVERE, message, t);
    }

    public static void debug(String message) {
        LOGGER.fine(message);
    }

    public static void close() {
        if (fileHandler != null) {
            fileHandler.close();
        }
    }
}
