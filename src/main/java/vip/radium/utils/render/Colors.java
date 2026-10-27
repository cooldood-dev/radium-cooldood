package vip.radium.utils.render;

import net.minecraft.util.MathHelper;

import java.awt.*;

public class Colors {
    public static final int WHITE = 0xFFFFFFFF;
    public static final int RED = new Color(0xf44336).getRGB();
    public static final int PINK = new Color(0xff80ab).getRGB();
    public static final int PURPLE = new Color(0xba68c8).getRGB();
    public static final int DEEP_PURPLE = new Color(0x7E5EB5).getRGB();
    public static final int INDIGO = new Color(0x7986cb).getRGB();
    public static final int BLUE = new Color(0x1976d2).getRGB();
    public static final int LIGHT_BLUE = new Color(0x74C3FF).getRGB();
    public static final int CYAN = new Color(0x00ACC1).getRGB();
    public static final int TEAL = new Color(0xA7FFEB).getRGB();
    public static final int GREEN = new Color(0x00FF46).getRGB();

    public static int getColor(Color color) {
        return getColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public static int getColor(Color color, int alpha) {
        return getColor(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp_int(alpha, 0, 255));
    }

    public static int getColor(int brightness) {
        return getColor(brightness, brightness, brightness, 255);
    }

    public static int getColor(int brightness, int alpha) {
        return getColor(brightness, brightness, brightness, alpha);
    }

    public static int getColor(int red, int green, int blue) {
        return getColor(red, green, blue, 255);
    }

    public static int getColor(int red, int green, int blue, int alpha) {
        int color = 0;
        color |= MathHelper.clamp_int(alpha, 0, 255) << 24;
        color |= MathHelper.clamp_int(red, 0, 255) << 16;
        color |= MathHelper.clamp_int(green, 0, 255) << 8;
        color |= MathHelper.clamp_int(blue, 0, 255);
        return color;
    }

    public static int getColorOpacity(int color, int alpha) {
        int red = (color >> 16 & 0xFF);
        int green = (color >> 8 & 0xFF);
        int blue = (color & 0xFF);
        return getColor(red, green, blue, MathHelper.clamp_int(alpha, 0, 255));
    }
}
