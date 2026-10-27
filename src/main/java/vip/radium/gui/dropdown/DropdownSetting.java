package vip.radium.gui.dropdown;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import vip.radium.RadiumClient;
import vip.radium.gui.font.FontManager;
import vip.radium.module.impl.visuals.Hud;
import vip.radium.property.Property;
import vip.radium.property.impl.DoubleProperty;
import vip.radium.property.impl.EnumProperty;
import vip.radium.property.impl.MultiSelectEnumProperty;
import vip.radium.property.impl.Representation;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class DropdownSetting {

    public enum Type {
        BOOLEAN,
        DOUBLE,
        ENUM,
        MULTI_ENUM,
        COLOR,
        STRING
    }

    private final Property<?> property;
    private final DropdownModule module;
    private final Type type;
    public boolean opened;
    public float percent = 0;
    private boolean dragging;

    // Color picker submode
    private enum ColorPickerMode {
        HUE, SATURATION, BRIGHTNESS
    }
    private ColorPickerMode colorMode = ColorPickerMode.HUE;
    private int separatorHue;
    private int separatorSaturation;
    private int separatorBrightness;

    public DropdownSetting(Property<?> property, DropdownModule module) {
        this.property = property;
        this.module = module;

        if (property instanceof DoubleProperty) {
            this.type = Type.DOUBLE;
        } else if (property instanceof EnumProperty) {
            this.type = Type.ENUM;
        } else if (property instanceof MultiSelectEnumProperty) {
            this.type = Type.MULTI_ENUM;
        } else if (property.getValue() instanceof Boolean) {
            this.type = Type.BOOLEAN;
        } else if (property.getValue() instanceof Integer) {
            this.type = Type.COLOR;
        } else {
            this.type = Type.STRING;
        }
    }

    public Property<?> getProperty() {
        return property;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }

    public void drawScreen(int mouseX, int mouseY) {
        int y = getY();
        Hud hud = RadiumClient.getInstance().getModuleManager().getModule(Hud.class);
        int hudColor = (hud != null && hud.isEnabled()) ? hud.getHudColor() : new Color(50, 150, 255).getRGB();

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        boolean scissor = scaledResolution.getScaleFactor() != 1;
        double clamp = MathHelper.clamp_double(Math.max(1, Minecraft.getDebugFPS()) / 30.0, 1.0, 9999.0);

        switch (type) {
            case DOUBLE: {
                DoubleProperty doubleProp = (DoubleProperty) property;
                if (module.yPerModule == module.getY() && scissor) {
                    GL11.glPushMatrix();
                    GL11.glScissor((int) (module.tab.getPosX() * 2 + 1), 0, 197, 999999999);
                    GL11.glEnable(GL11.GL_SCISSOR_TEST);
                }

                double min = doubleProp.getMin();
                double max = doubleProp.getMax();
                double currentVal = doubleProp.getValue();
                double percentBar = (currentVal - min) / (max - min);

                percent = Math.max(0, Math.min(1, (float) (percent + (Math.max(0, Math.min(percentBar, 1)) - percent) * (0.2 / clamp))));
                
                Gui.drawRect(module.tab.getPosX() + 1, y + 3, module.tab.getPosX() + 99, y + 14, new Color(0, 0, 0, 50).getRGB());
                Gui.drawRect(module.tab.getPosX() + 1, y + 3, (int) (module.tab.getPosX() + 1 + 98 * percent), y + 14, hudColor);

                String formattedValue;
                if (doubleProp.getRepresentation() == Representation.INT) {
                    formattedValue = String.valueOf((int) currentVal);
                } else if (doubleProp.getRepresentation() == Representation.PERCENTAGE) {
                    formattedValue = (int) currentVal + "%";
                } else if (doubleProp.getRepresentation() == Representation.MILLISECONDS) {
                    formattedValue = (int) currentVal + "ms";
                } else if (doubleProp.getRepresentation() == Representation.DISTANCE) {
                    formattedValue = ((int) (currentVal * 10.0) / 10.0) + "m";
                } else {
                    formattedValue = String.valueOf((int) (currentVal * 100.0) / 100.0);
                }

                FontManager.SF_18.drawString(doubleProp.getLabel() + " " + formattedValue, module.tab.getPosX() + 4, y + 5.5f, 0xffffffff);

                if (this.dragging) {
                    double difference = max - min;
                    double clampedX = MathHelper.clamp_double((mouseX - (module.tab.getPosX() + 1)) / 98.0, 0.0, 1.0);
                    double value = min + clampedX * difference;
                    double inc = doubleProp.getIncrement();
                    if (inc > 0) {
                        value = Math.round(value / inc) * inc;
                    }
                    doubleProp.setValue(value);
                }

                if (module.yPerModule == module.getY() && scissor) {
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);
                    GL11.glPopMatrix();
                }
                break;
            }

            case BOOLEAN: {
                Property<Boolean> boolProp = (Property<Boolean>) property;
                Gui.drawRect(module.tab.getPosX() + 89, y + 4, module.tab.getPosX() + 99, y + 14, new Color(0, 0, 0, 50).getRGB());
                if (boolProp.getValue()) {
                    drawCheck(module.tab.getPosX() + 91, y + 8.5f, 2, new Color(hudColor).brighter().getRGB());
                }
                FontManager.SF_18.drawString(boolProp.getLabel(), module.tab.getPosX() + 4, y + 5.5f, new Color(227, 227, 227, 255).getRGB());
                break;
            }

            case ENUM: {
                EnumProperty<?> enumProp = (EnumProperty<?>) property;
                FontManager.SF_17.drawString(enumProp.getLabel(), module.tab.getPosX() + 3, (float) (y + 6), 0xffffffff);
                String valName = enumProp.getValue().name().toUpperCase();
                FontManager.SF_17.drawString(valName,
                        module.tab.getPosX() + 97 - FontManager.SF_17.getWidth(valName), y + 7f,
                        new Color(255, 255, 255, 255).getRGB());
                break;
            }

            case MULTI_ENUM: {
                MultiSelectEnumProperty<?> multiProp = (MultiSelectEnumProperty<?>) property;
                Enum<?>[] acceptableValues = multiProp.getValues();
                int h = (acceptableValues.length * 17) + 2;

                Gui.drawRect(module.tab.getPosX() + 1, y + 3, module.tab.getPosX() + 99, y + 14 + (opened ? h : 0), new Color(0, 0, 0, 50).getRGB());
                String displayName = multiProp.getLabel();
                FontManager.SF_18.drawString(displayName, (float) (module.tab.getPosX() + 49.5 - FontManager.SF_18.getWidth(displayName) / 2.0), y + 5.5f, Color.WHITE.getRGB());

                if (opened) {
                    int yS = y + 17;
                    for (int i = 0; i < acceptableValues.length; i++) {
                        Enum<?> val = acceptableValues[i];
                        boolean isSelected = ((List<?>) multiProp.getValue()).contains(val);
                        int rgb = isSelected ? 0xffffffff : new Color(174, 174, 174, 150).getRGB();
                        FontManager.SF_18.drawString(val.name(), module.tab.getPosX() + 15, (float) (yS + 6), rgb);
                        FontManager.SF_26.drawString(".", module.tab.getPosX() + 9, yS, rgb);
                        yS += 17;
                    }
                }
                break;
            }

            case COLOR: {
                Property<Integer> colorProp = (Property<Integer>) property;
                int color = colorProp.getValue();
                int red = (color >> 16) & 0xFF;
                int green = (color >> 8) & 0xFF;
                int blue = color & 0xFF;
                float[] hsb = Color.RGBtoHSB(red, green, blue, null);

                if (this.dragging) {
                    double selectedX = MathHelper.clamp_double(mouseX - module.tab.getPosX() - 1, 0.35, 97.0);
                    float normalized = (float) (selectedX / 98.0);
                    switch (colorMode) {
                        case HUE:
                            separatorHue = (int) selectedX;
                            colorProp.setValue(Color.getHSBColor(normalized, hsb[1], hsb[2]).getRGB());
                            break;
                        case SATURATION:
                            separatorSaturation = (int) selectedX;
                            colorProp.setValue(Color.getHSBColor(hsb[0], normalized, hsb[2]).getRGB());
                            break;
                        case BRIGHTNESS:
                            separatorBrightness = (int) selectedX;
                            colorProp.setValue(Color.getHSBColor(hsb[0], hsb[1], normalized).getRGB());
                            break;
                    }
                }

                switch (colorMode) {
                    case HUE:
                        for (int max = 98, i = 0; i < max; i++) {
                            Gui.drawRect((int) (module.tab.getPosX() + 1 + i), y + 3, (int) (module.tab.getPosX() + 2 + i), y + 14,
                                    Color.getHSBColor(i / (float) max, hsb[1], hsb[2]).getRGB());
                        }
                        FontManager.SF_18.drawString(colorProp.getLabel(), module.tab.getPosX() + 4, y + 5.5f, 0xffffffff);
                        Gui.drawRect(module.tab.getPosX() + 1 + separatorHue, y + 3, module.tab.getPosX() + 2 + separatorHue, y + 14, Color.WHITE.getRGB());
                        break;
                    case SATURATION:
                        for (int max = 98, i = 0; i < max; i++) {
                            Gui.drawRect((int) (module.tab.getPosX() + 1 + i), y + 3, (int) (module.tab.getPosX() + 2 + i), y + 14,
                                    Color.getHSBColor(hsb[0], i / (float) max, hsb[2]).getRGB());
                        }
                        FontManager.SF_18.drawString(colorProp.getLabel(), module.tab.getPosX() + 4, y + 5.5f, 0xffffffff);
                        Gui.drawRect(module.tab.getPosX() + 1 + separatorSaturation, y + 3, module.tab.getPosX() + 2 + separatorSaturation, y + 14, Color.WHITE.getRGB());
                        break;
                    case BRIGHTNESS:
                        for (int max = 98, i = 0; i < max; i++) {
                            Gui.drawRect((int) (module.tab.getPosX() + 1 + i), y + 3, (int) (module.tab.getPosX() + 2 + i), y + 14,
                                    Color.getHSBColor(hsb[0], hsb[1], i / (float) max).getRGB());
                        }
                        FontManager.SF_18.drawString(colorProp.getLabel(), module.tab.getPosX() + 4, y + 5.5f, 0xffffffff);
                        Gui.drawRect(module.tab.getPosX() + 1 + separatorBrightness, y + 3, module.tab.getPosX() + 2 + separatorBrightness, y + 14, Color.WHITE.getRGB());
                        break;
                }
                break;
            }

            case STRING: {
                FontManager.SF_16.drawString(property.getLabel(), module.tab.getPosX() + 4, y + 5.5f, 0xffffffff);
                break;
            }
        }
    }

    public static void drawCheck(double x, double y, int lineWidth, int color) {
        DropdownModule.start2D();
        GL11.glPushMatrix();
        GL11.glLineWidth(lineWidth);
        DropdownModule.setColor(new Color(color));
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x + 2, y + 3);
        GL11.glVertex2d(x + 6, y - 2);
        GL11.glEnd();
        GL11.glPopMatrix();
        DropdownModule.stop2D();
    }

    public int getY() {
        int y = module.y + 14;
        for (DropdownSetting setting : module.settings.stream().filter(s -> s.getProperty().isAvailable()).collect(Collectors.toList())) {
            if (setting == this) {
                break;
            } else {
                y += setting.getHeight();
            }
        }
        return y;
    }

    public int getHeight() {
        if (type == Type.MULTI_ENUM) {
            MultiSelectEnumProperty<?> multi = (MultiSelectEnumProperty<?>) property;
            return opened ? 17 + multi.getValues().length * 17 : 15;
        } else {
            return 15;
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isHovered(mouseX, mouseY)) {
            switch (type) {
                case BOOLEAN:
                    if (mouseButton == 0) {
                        Property<Boolean> bool = (Property<Boolean>) property;
                        bool.setValue(!bool.getValue());
                    }
                    break;
                case ENUM:
                    if (mouseButton == 0) {
                        EnumProperty<?> enumProp = (EnumProperty<?>) property;
                        Enum<?>[] values = enumProp.getValues();
                        int next = (enumProp.getValue().ordinal() + 1) % values.length;
                        enumProp.setValue(next);
                    }
                    break;
                case MULTI_ENUM:
                    if (mouseButton == 0 || mouseButton == 1) {
                        opened = !opened;
                    }
                    break;
                case DOUBLE:
                    if (mouseButton == 0) {
                        dragging = true;
                    }
                    break;
                case COLOR:
                    if (mouseButton == 0) {
                        dragging = true;
                    } else if (mouseButton == 1) {
                        // Cycle ColorPickerMode
                        ColorPickerMode[] modes = ColorPickerMode.values();
                        colorMode = modes[(colorMode.ordinal() + 1) % modes.length];
                    }
                    break;
            }
        }

        if (type == Type.MULTI_ENUM && opened && mouseX >= module.tab.getPosX() && mouseX <= module.tab.getPosX() + 99) {
            MultiSelectEnumProperty<?> multiProp = (MultiSelectEnumProperty<?>) property;
            Enum<?>[] acceptableValues = multiProp.getValues();
            for (int i = 0; i < acceptableValues.length; i++) {
                int v = getY() + 17 + i * 17;
                if (mouseY >= v && mouseY <= v + 17) {
                    multiProp.setValue(i);
                    break;
                }
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            dragging = false;
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        // Textbox/typing if applicable
    }

    public boolean isHovered(int mouseX, int mouseY) {
        int y = getY();
        switch (type) {
            case DOUBLE:
            case COLOR:
                return mouseX >= module.tab.getPosX() + 1 && mouseY >= y + 3 && mouseX <= module.tab.getPosX() + 99 && mouseY <= y + 14;
            case BOOLEAN:
                return mouseX >= module.tab.getPosX() + 4 && mouseY >= y + 4 && mouseX <= module.tab.getPosX() + 99 && mouseY <= y + 14;
            default:
                return mouseX >= module.tab.getPosX() && mouseY >= y && mouseX <= module.tab.getPosX() + 99 && mouseY <= y + 17;
        }
    }
}
