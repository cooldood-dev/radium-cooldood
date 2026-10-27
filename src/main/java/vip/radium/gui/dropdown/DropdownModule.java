package vip.radium.gui.dropdown;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import vip.radium.RadiumClient;
import vip.radium.gui.font.FontManager;
import vip.radium.module.Module;
import vip.radium.module.impl.visuals.Hud;
import vip.radium.property.Property;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.*;

public class DropdownModule {

    private final Module module;
    public int yPerModule, y;
    public DropdownTab tab;
    public boolean opened;
    public List<DropdownSetting> settings = new CopyOnWriteArrayList<>();

    private double length = 3, anim = 5;
    float fraction = 0;
    float fractionBackground = 0;

    // Keybinding state for the module itself
    private boolean binding = false;

    public DropdownModule(Module module, DropdownTab tab) {
        this.module = module;
        this.tab = tab;

        // Populate settings from module's properties
        for (Property<?> property : module.getElements()) {
            settings.add(new DropdownSetting(property, this));
        }
    }

    public void drawScreen(int mouseX, int mouseY) {
        Minecraft instance = Minecraft.getMinecraft();
        int debugFPS = Math.max(1, Minecraft.getDebugFPS());

        if (module.isEnabled() && fraction < 1) {
            fraction += 0.0025F * (2000.0F / debugFPS);
        }
        if (!module.isEnabled() && fraction > 0) {
            fraction -= 0.0025F * (2000.0F / debugFPS);
        }

        if (!module.isEnabled()) {
            if (isHovered(mouseX, mouseY) && fractionBackground < 1) {
                fractionBackground += 0.0025F * (2000.0F / debugFPS);
            }
            if (!isHovered(mouseX, mouseY) && fractionBackground > 0) {
                fractionBackground -= 0.0025F * (2000.0F / debugFPS);
            }
        }

        fraction = MathHelper.clamp_float(fraction, 0.0F, 1.0F);
        fractionBackground = MathHelper.clamp_float(fractionBackground, 0.0F, 1.0F);

        if (yPerModule < getY()) {
            yPerModule = (int) (yPerModule + (getY() + 9 - yPerModule) * 0.1);
        } else if (yPerModule > getY()) {
            yPerModule = (int) (yPerModule + (getY() - yPerModule) * 0.1);
        }

        y = (int) (tab.getPosY() + 15);
        for (DropdownModule tabModule : tab.getModules()) {
            if (tabModule == this) {
                break;
            } else {
                y += tabModule.yPerModule;
            }
        }

        Hud hud = RadiumClient.getInstance().getModuleManager().getModule(Hud.class);
        Color colorHUD = (hud != null && hud.isEnabled()) ? new Color(hud.getHudColor()) : new Color(50, 150, 255);

        Gui.drawRect(tab.getPosX(), y, tab.getPosX() + 100, y + yPerModule, new Color(40, 40, 40, 255).getRGB());
        if (!module.isEnabled() && fraction == 0) {
            Gui.drawRect(tab.getPosX(), y, tab.getPosX() + 100, y + 14, interpolateColor(new Color(40, 40, 40, 255), new Color(29, 29, 29, 255), fractionBackground));
        } else {
            Gui.drawRect(tab.getPosX(), y, tab.getPosX() + 100, y + 14, interpolateColor(new Color(40, 40, 40, 255), colorHUD, fraction));
        }

        String displayName = module.getLabel();
        if (binding) {
            displayName = "Press key...";
        }
        FontManager.SF_18.drawString(displayName, tab.getPosX() + 2, (float) (y + 4), 0xffffffff);

        if (!settings.isEmpty() || module.getKey() != 0) {
            double val = debugFPS / 8.3;
            if (opened && length > -3) {
                length -= 3 / val;
            } else if (!opened && length < 3) {
                length += 3 / val;
            }
            if (opened && anim < 8) {
                anim += 3 / val;
            } else if (!opened && anim > 5) {
                anim -= 3 / val;
            }
            drawArrow(tab.getPosX() + 92, y + anim, 2, 0xffffffff, length);
        }

        if (opened || yPerModule != 14) {
            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            if (yPerModule != getY() && scaledResolution.getScaleFactor() != 1) {
                GL11.glScissor(0,
                        scaledResolution.getScaledHeight() * 2 - y * 2 - yPerModule * 2,
                        scaledResolution.getScaledWidth() * 2,
                        yPerModule * 2);
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                drawSettings(mouseX, mouseY);
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            } else {
                drawSettings(mouseX, mouseY);
            }
        } else {
            settings.forEach(setting -> setting.setPercent(0));
        }
    }

    private void drawSettings(int mouseX, int mouseY) {
        settings.stream()
                .filter(s -> s.getProperty().isAvailable())
                .forEach(setting -> setting.drawScreen(mouseX, mouseY));
    }

    public static void drawArrow(double x, double y, int lineWidth, int color, double length) {
        start2D();
        GL11.glPushMatrix();
        GL11.glLineWidth(lineWidth);
        setColor(new Color(color));
        GL11.glBegin(GL_LINE_STRIP);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x + 3, y + length);
        GL11.glVertex2d(x + 6, y);
        GL11.glEnd();
        GL11.glPopMatrix();
        stop2D();
    }

    public static void start2D() {
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
    }

    public static void stop2D() {
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glColor4f(1, 1, 1, 1);
    }

    public static void setColor(Color color) {
        float alpha = (color.getRGB() >> 24 & 0xFF) / 255.0F;
        float red = (color.getRGB() >> 16 & 0xFF) / 255.0F;
        float green = (color.getRGB() >> 8 & 0xFF) / 255.0F;
        float blue = (color.getRGB() & 0xFF) / 255.0F;
        GL11.glColor4f(red, green, blue, alpha);
    }

    private int interpolateColor(Color color1, Color color2, float fraction) {
        int red = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * fraction);
        int green = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * fraction);
        int blue = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * fraction);
        int alpha = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * fraction);
        try {
            return new Color(red, green, blue, alpha).getRGB();
        } catch (Exception ex) {
            return 0xffffffff;
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (binding) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE) {
                module.setKey(Keyboard.KEY_NONE);
            } else {
                module.setKey(keyCode);
            }
            binding = false;
            return;
        }

        if (opened) {
            settings.stream()
                    .filter(s -> s.getProperty().isAvailable())
                    .forEach(setting -> setting.keyTyped(typedChar, keyCode));
        }
    }

    public int getY() {
        if (opened) {
            int i = 17;
            for (DropdownSetting setting : settings.stream().filter(s -> s.getProperty().isAvailable()).collect(Collectors.toList())) {
                i += setting.getHeight();
            }
            return i;
        } else {
            return 14;
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isHovered(mouseX, mouseY)) {
            switch (mouseButton) {
                case 0:
                    module.toggle();
                    break;
                case 1:
                    if (!settings.isEmpty()) {
                        opened = !opened;
                    }
                    break;
                case 2: // Middle click to bind key
                    binding = !binding;
                    break;
            }
        }
        if (opened) {
            settings.stream()
                    .filter(s -> s.getProperty().isAvailable())
                    .forEach(setting -> {
                        try {
                            setting.mouseClicked(mouseX, mouseY, mouseButton);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (opened) {
            settings.stream()
                    .filter(s -> s.getProperty().isAvailable())
                    .forEach(setting -> setting.mouseReleased(mouseX, mouseY, state));
        }
    }

    public boolean isHovered(int mouseX, int mouseY) {
        y = (int) (tab.getPosY() + 15);
        for (DropdownModule tabModule : tab.getModules()) {
            if (tabModule == this) {
                break;
            } else {
                y += tabModule.yPerModule;
            }
        }
        if (opened)
            return mouseX >= tab.getPosX() && mouseY >= y && mouseX <= tab.getPosX() + 101 && mouseY <= y + 14;
        return mouseX >= tab.getPosX() && mouseY >= y && mouseX <= tab.getPosX() + 101 && mouseY <= y + yPerModule;
    }

    public Module getModule() {
        return module;
    }

    public boolean isBinding() {
        return binding;
    }
}
