package vip.radium.gui.dropdown;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import vip.radium.RadiumClient;
import vip.radium.module.ModuleCategory;
import vip.radium.module.impl.visuals.Hud;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DropdownGUI extends GuiScreen {

    private static DropdownGUI INSTANCE;
    private final List<DropdownTab> tabs = new CopyOnWriteArrayList<>();

    private boolean dragging;
    private int dragX;
    private int dragY;

    public DropdownGUI() {
        initTabs();
    }

    public static DropdownGUI getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DropdownGUI();
        }
        return INSTANCE;
    }

    private void initTabs() {
        if (tabs.isEmpty()) {
            float x = 20;
            for (ModuleCategory category : ModuleCategory.values()) {
                tabs.add(new DropdownTab(category, x, 20));
                x += 108;
            }
        }
    }

    @Override
    public void initGui() {
        initTabs();
        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GL11.glPushMatrix();
        ScaledResolution scaledResolution = new ScaledResolution(mc);

        int[] scaledMouse = getScaledMouseCoordinates(mc, mouseX, mouseY);
        int x = scaledMouse[0];
        int y = scaledMouse[1];

        scale(mc);

        // Background dim
        Gui.drawRect(0, 0, scaledResolution.getScaledWidth() * 2, scaledResolution.getScaledHeight() * 2, new Color(0, 0, 0, 120).getRGB());

        for (DropdownTab tab : tabs) {
            tab.drawScreen(x, y);
            if (tab.dragging) {
                tab.setPosX(dragX + x);
                tab.setPosY(dragY + y);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        GL11.glPopMatrix();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int[] scaledMouse = getScaledMouseCoordinates(mc, mouseX, mouseY);
        int x = scaledMouse[0];
        int y = scaledMouse[1];

        for (DropdownTab tab : tabs) {
            if (tab.isHovered(x, y) && mouseButton == 0) {
                if (!anyDragging()) {
                    tab.dragging = true;
                    dragX = (int) (tab.getPosX() - x);
                    dragY = (int) (tab.getPosY() - y);
                }
            }
            tab.mouseClicked(x, y, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean anyDragging() {
        for (DropdownTab tab : tabs) {
            if (tab.dragging) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE && !areAnyListening()) {
            mc.displayGuiScreen(null);
            if (mc.currentScreen == null) {
                mc.setIngameFocus();
            }
        } else {
            tabs.forEach(tab -> tab.keyTyped(typedChar, keyCode));
        }
    }

    private boolean areAnyListening() {
        for (DropdownTab tab : tabs) {
            for (DropdownModule module : tab.getModules()) {
                if (module.isBinding()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            tabs.forEach(tab -> tab.dragging = false);
        }
        int[] scaledMouse = getScaledMouseCoordinates(mc, mouseX, mouseY);
        tabs.forEach(tab -> tab.mouseReleased(scaledMouse[0], scaledMouse[1], state));
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public List<DropdownTab> getTabs() {
        return tabs;
    }

    public static int[] getScaledMouseCoordinates(Minecraft mc, int mouseX, int mouseY) {
        int x = mouseX;
        int y = mouseY;
        switch (mc.gameSettings.guiScale) {
            case 0:
                x *= 2;
                y *= 2;
                break;
            case 1:
                x *= 0.5;
                y *= 0.5;
                break;
            case 3:
                x = (int) (x * 1.5);
                y = (int) (y * 1.5);
                break;
        }
        return new int[]{x, y};
    }

    public static void scale(Minecraft mc) {
        switch (mc.gameSettings.guiScale) {
            case 0:
                GL11.glScaled(0.5, 0.5, 0.5);
                break;
            case 1:
                GL11.glScaled(2.0, 2.0, 2.0);
                break;
            case 3:
                GL11.glScaled(0.6666666666666667, 0.6666666666666667, 0.6666666666666667);
                break;
        }
    }
}
