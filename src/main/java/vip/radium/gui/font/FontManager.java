package vip.radium.gui.font;

import vip.radium.utils.render.TTFUtils;

import java.awt.*;

public final class FontManager {

    public static final TrueTypeFontRenderer FR =
            new TrueTypeFontRenderer(TTFUtils.getFontFromLocation("font.ttf", 21), true, true);
    public static final TrueTypeFontRenderer MEDIUM_FR =
            new TrueTypeFontRenderer(TTFUtils.getFontFromLocation("font.ttf", 20), true, true);
    public static final TrueTypeFontRenderer SMALL_FR =
            new TrueTypeFontRenderer(TTFUtils.getFontFromLocation("font.ttf", 18), true, true);
    public static final TrueTypeFontRenderer CSGO_FR = new TrueTypeFontRenderer(
            new Font("Tahoma", Font.BOLD, 11), true, false);
    public static final TrueTypeFontRenderer FN_FR = new TrueTypeFontRenderer(
            TTFUtils.getFontFromLocation("Burbank.ttf", 36), true, false);
    public static final TrueTypeFontRenderer SP_FR = new TrueTypeFontRenderer(
            TTFUtils.getFontFromLocation("smallest_pixel.ttf", 16), true, true);

    // Novoline fonts & icons
    public static final TrueTypeFontRenderer SF_16 =
            new TrueTypeFontRenderer(TTFUtils.getFontFromLocation("SF.ttf", 16), true, true);
    public static final TrueTypeFontRenderer SF_17 =
            new TrueTypeFontRenderer(TTFUtils.getFontFromLocation("SF.ttf", 17), true, true);
    public static final TrueTypeFontRenderer SF_18 =
            new TrueTypeFontRenderer(TTFUtils.getFontFromLocation("SF.ttf", 18), true, true);
    public static final TrueTypeFontRenderer SF_20 =
            new TrueTypeFontRenderer(TTFUtils.getFontFromLocation("SF.ttf", 20), true, true);
    public static final TrueTypeFontRenderer SF_26 =
            new TrueTypeFontRenderer(TTFUtils.getFontFromLocation("SF.ttf", 26), true, true);
    public static final TrueTypeFontRenderer SF_BOLD_18 =
            new TrueTypeFontRenderer(TTFUtils.getFontFromLocation("SFBOLD.ttf", 18), true, true);
    public static final TrueTypeFontRenderer ICONFONT_24 =
            new TrueTypeFontRenderer(TTFUtils.getFontFromLocation("stylesicons.ttf", 24), true, true);
    public static final TrueTypeFontRenderer ICONFONT_35 =
            new TrueTypeFontRenderer(TTFUtils.getFontFromLocation("stylesicons.ttf", 35), true, true);

    private FontManager() {}

    public static void initTextures() {
        FR.generateTextures();
        SP_FR.generateTextures();
        MEDIUM_FR.generateTextures();
        SMALL_FR.generateTextures();

        CSGO_FR.generateTextures();
        FN_FR.generateTextures();

        SF_16.generateTextures();
        SF_17.generateTextures();
        SF_18.generateTextures();
        SF_20.generateTextures();
        SF_26.generateTextures();
        SF_BOLD_18.generateTextures();
        ICONFONT_24.generateTextures();
        ICONFONT_35.generateTextures();
    }
}
