package vip.radium.gui.alt.impl;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import vip.radium.RadiumClient;
import vip.radium.alt.Alt;
import vip.radium.alt.CrackedAltGenerator;
import vip.radium.gui.alt.GuiAltScreen;
import vip.radium.notification.Notification;
import vip.radium.notification.NotificationType;
import vip.radium.utils.ClipboardUtils;
import vip.radium.utils.SessionUtils;
import vip.radium.utils.Wrapper;
import vip.radium.utils.render.RenderingUtils;

import java.io.IOException;

public final class GuiCrackedGenerator extends GuiAltScreen {

    private GuiTextField usernameField;

    public GuiCrackedGenerator(GuiScreen parent) {
        super(parent);
    }

    @Override
    public void initGui() {
        final int buttonWidth = 200;
        final int buttonHeight = 20;
        final int margin = 4;
        final int offset = buttonHeight + margin;

        final int middleX = width / 2 - (buttonWidth / 2);
        final int middleY = height / 4 + 40;

        this.usernameField = new GuiTextField(10, Wrapper.getMinecraftFontRenderer(),
                "Username", middleX, middleY, buttonWidth, buttonHeight);
        this.usernameField.setMaxStringLength(16);

        // Pre-generate a random username on open
        String randomName = CrackedAltGenerator.generateRandomUsername();
        this.usernameField.setText(randomName);

        this.textFields.clear();
        this.textFields.add(usernameField);

        this.buttonList.clear();
        this.buttonList.add(new GuiButton(1, middleX, middleY + offset, "Generate New Username"));
        this.buttonList.add(new GuiButton(2, middleX, middleY + offset * 2, "Login"));
        this.buttonList.add(new GuiButton(3, middleX, middleY + offset * 3, "Login & Add to Alt Manager"));
        this.buttonList.add(new GuiButton(4, middleX, middleY + offset * 4, "Copy Username"));
        this.buttonList.add(new GuiButton(0, middleX, middleY + offset * 5, "Back"));

        super.initGui();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        String username = usernameField.getText().trim();

        switch (button.id) {
            case 0:
                Wrapper.getMinecraft().displayGuiScreen(parent);
                break;
            case 1:
                String generated = CrackedAltGenerator.generateRandomUsername();
                usernameField.setText(generated);
                break;
            case 2:
                if (validateUsername(username)) {
                    SessionUtils.logInCracked(username);
                }
                break;
            case 3:
                if (validateUsername(username)) {
                    Alt alt = new Alt(username, "");
                    alt.setUsername(username);
                    alt.setWorking(true);
                    RadiumClient.getInstance().getAltManager().addAlt(alt);
                    SessionUtils.logInCracked(username);
                }
                break;
            case 4:
                if (validateUsername(username)) {
                    ClipboardUtils.setClipboardContents(username);
                    RadiumClient.getInstance().getNotificationManager().add(new Notification(
                            "Copied",
                            "Copied username: " + username,
                            1500,
                            NotificationType.INFO
                    ));
                }
                break;
        }
    }

    private boolean validateUsername(String name) {
        if (name == null || name.length() < 3 || name.length() > 16) {
            RadiumClient.getInstance().getNotificationManager().add(new Notification(
                    "Invalid Username",
                    "Length must be between 3 and 16 characters",
                    2000,
                    NotificationType.ERROR
            ));
            return false;
        }

        if (!name.matches("^[a-zA-Z0-9_]{3,16}$")) {
            RadiumClient.getInstance().getNotificationManager().add(new Notification(
                    "Invalid Username",
                    "Allowed characters: A-Z, a-z, 0-9, and _",
                    2000,
                    NotificationType.ERROR
            ));
            return false;
        }

        return true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        RenderingUtils.drawGuiBackground(width, height);

        final float centerX = width / 2.0F;

        Wrapper.getMinecraftFontRenderer().drawStringWithShadow(
                "\247lCracked Alt Generator",
                centerX - (Wrapper.getMinecraftFontRenderer().getWidth("\247lCracked Alt Generator") / 2.0F),
                height / 4.0F - 10,
                -1
        );

        String info = "\2477Offline / Cracked Minecraft Username";
        Wrapper.getMinecraftFontRenderer().drawStringWithShadow(
                info,
                centerX - (Wrapper.getMinecraftFontRenderer().getWidth(info) / 2.0F),
                height / 4.0F + 10,
                -1
        );

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}

