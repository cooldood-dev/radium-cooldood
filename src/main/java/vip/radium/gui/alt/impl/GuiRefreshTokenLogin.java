package vip.radium.gui.alt.impl;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import vip.radium.RadiumClient;
import vip.radium.alt.Alt;
import vip.radium.gui.alt.GuiAltScreen;
import vip.radium.notification.Notification;
import vip.radium.notification.NotificationType;
import vip.radium.utils.SessionUtils;
import vip.radium.utils.StringUtils;
import vip.radium.utils.Wrapper;

import java.io.IOException;

public final class GuiRefreshTokenLogin extends GuiAltScreen {

    private GuiTextField tokenField;

    public GuiRefreshTokenLogin(GuiScreen parent) {
        super(parent);
    }

    @Override
    public void initGui() {
        final int buttonWidth = 200;
        final int buttonHeight = 20;
        final int margin = 2;
        final int offset = buttonHeight + margin;

        final int middleX = width / 2 - (buttonWidth / 2);
        final int middleY = height / 4 + 48;

        this.tokenField = new GuiTextField(3, Wrapper.getMinecraftFontRenderer(),
                "Refresh Token", middleX, middleY, buttonWidth, buttonHeight - margin);
        this.tokenField.setMaxStringLength(4096);

        this.textFields.add(tokenField);

        this.buttonList.add(new GuiButton(1, middleX, middleY + offset, "Login"));
        this.buttonList.add(new GuiButton(2, middleX, middleY + offset * 2, "Login & Add to Alt Manager"));
        this.buttonList.add(new GuiButton(3, middleX, middleY + offset * 3, "Paste from Clipboard"));
        this.buttonList.add(new GuiButton(0, middleX, middleY + offset * 4, "Back"));

        super.initGui();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                Wrapper.getMinecraft().displayGuiScreen(parent);
                break;
            case 1: {
                String token = tokenField.getText().trim();
                if (!token.isEmpty()) {
                    SessionUtils.logInWithRefreshToken(token, null);
                } else {
                    emptyTokenError();
                }
                break;
            }
            case 2: {
                String token = tokenField.getText().trim();
                if (!token.isEmpty()) {
                    Alt alt = new Alt(token);
                    RadiumClient.getInstance().getAltManager().addAlt(alt);
                    SessionUtils.logInWithRefreshToken(token, alt);
                } else {
                    emptyTokenError();
                }
                break;
            }
            case 3: {
                String clip = StringUtils.getTrimmedClipboardContents();
                if (clip != null && !clip.isEmpty()) {
                    if (clip.startsWith("token:") || clip.startsWith("refresh:")) {
                        clip = clip.substring(clip.indexOf(':') + 1).trim();
                    }
                    tokenField.setText(clip);
                } else {
                    RadiumClient.getInstance().getNotificationManager().add(new Notification(
                            "Clipboard Empty",
                            "No token found in clipboard",
                            1500, NotificationType.ERROR));
                }
                break;
            }
        }
    }

    private void emptyTokenError() {
        RadiumClient.getInstance().getNotificationManager().add(new Notification(
                "Login Error",
                "Please enter a valid refresh token",
                1500, NotificationType.ERROR));
    }
}
