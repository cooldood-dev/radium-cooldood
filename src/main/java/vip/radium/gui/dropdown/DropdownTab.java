package vip.radium.gui.dropdown;

import net.minecraft.client.gui.Gui;
import vip.radium.RadiumClient;
import vip.radium.gui.font.FontManager;
import vip.radium.module.Module;
import vip.radium.module.ModuleCategory;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DropdownTab {

    private final ModuleCategory category;
    private float posX;
    private float posY;
    public boolean dragging, opened = true;
    public List<DropdownModule> modules = new CopyOnWriteArrayList<>();

    public DropdownTab(ModuleCategory category, float posX, float posY) {
        this.category = category;
        this.posX = posX;
        this.posY = posY;
        List<Module> mods = RadiumClient.getInstance().getModuleManager().getModulesForCategory(category);
        if (mods != null) {
            for (Module mod : mods) {
                modules.add(new DropdownModule(mod, this));
            }
        }
    }

    public void drawScreen(int mouseX, int mouseY) {
        String icon = "";
        switch (category) {
            case COMBAT:
                icon = "D";
                break;
            case MOVEMENT:
                icon = "A";
                break;
            case PLAYER:
                icon = "B";
                break;
            case VISUALS:
            case ESP:
                icon = "C";
                break;
            case MISCELLANEOUS:
                icon = "F";
                break;
            default:
                icon = "G";
                break;
        }

        // Header background
        Gui.drawRect(posX - 1, posY, posX + 101, posY + 15, new Color(29, 29, 29, 255).getRGB());
        
        // Category icon from stylesicons.ttf
        FontManager.ICONFONT_24.drawString(icon, posX + 88, posY + 5, 0xffffffff);
        
        // Category title
        String name = category.name().charAt(0) + category.name().substring(1).toLowerCase();
        FontManager.SF_20.drawString(name, posX + 4, posY + 4, 0xffffffff);

        if (opened) {
            Gui.drawRect(posX - 1, posY + 15, posX + 101, posY + 15 + getTabHeight() + 1, new Color(29, 29, 29, 255).getRGB());
            modules.forEach(module -> module.drawScreen(mouseX, mouseY));
        } else {
            modules.forEach(module -> module.yPerModule = 0);
        }
    }

    public int getTabHeight() {
        int height = 0;
        for (DropdownModule module : modules) {
            height += module.yPerModule;
        }
        return height;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= posX && mouseY >= posY && mouseX <= posX + 101 && mouseY <= posY + 15;
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (opened) {
            modules.forEach(module -> module.mouseReleased(mouseX, mouseY, state));
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (opened) {
            modules.forEach(module -> module.keyTyped(typedChar, keyCode));
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isHovered(mouseX, mouseY) && mouseButton == 1) {
            opened = !opened;
            if (opened) {
                for (DropdownModule module : modules) {
                    module.fraction = 0;
                }
            }
        }

        if (opened) {
            modules.forEach(module -> {
                try {
                    module.mouseClicked(mouseX, mouseY, mouseButton);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void setPosX(float posX) {
        this.posX = posX;
    }

    public void setPosY(float posY) {
        this.posY = posY;
    }

    public float getPosY() {
        return posY;
    }

    public float getPosX() {
        return posX;
    }

    public List<DropdownModule> getModules() {
        return modules;
    }

    public ModuleCategory getCategory() {
        return category;
    }
}
