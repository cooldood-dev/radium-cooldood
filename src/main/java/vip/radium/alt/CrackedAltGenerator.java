package vip.radium.alt;

import java.security.SecureRandom;
import java.util.Random;

public final class CrackedAltGenerator {

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
    private static final Random RANDOM = new SecureRandom();

    private CrackedAltGenerator() {
    }

    /**
     * Generates a completely random cracked/offline Minecraft username.
     * Rules:
     * - Length: Between 3 and 16 characters inclusive.
     * - Allowed Characters: A-Z, a-z, 0-9, and underscore (_).
     */
    public static String generateRandomUsername() {
        int length = 3 + RANDOM.nextInt(14); // 3 to 16
        return generateRandomUsername(length);
    }

    /**
     * Generates a completely random username of a specific length between 3 and 16.
     */
    public static String generateRandomUsername(int length) {
        if (length < 3) length = 3;
        if (length > 16) length = 16;

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }
}

