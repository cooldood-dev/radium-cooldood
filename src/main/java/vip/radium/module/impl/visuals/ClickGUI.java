package vip.radium.module.impl.visuals;

import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import org.lwjgl.input.Keyboard;
import vip.radium.RadiumClient;
import vip.radium.event.impl.KeyPressEvent;
import vip.radium.gui.csgo.SkeetUI;
import vip.radium.gui.dropdown.DropdownGUI;
import vip.radium.module.Module;
import vip.radium.module.ModuleCategory;
import vip.radium.module.ModuleInfo;
import vip.radium.property.impl.EnumProperty;
import vip.radium.utils.Wrapper;

@ModuleInfo(label = "ClickGUI", category = ModuleCategory.VISUALS, key = Keyboard.KEY_RSHIFT)
public final class ClickGUI extends Module {

    public enum GUIMode {
        DROPDOWN,
        SKEET
    }

    private final EnumProperty<GUIMode> modeProperty = new EnumProperty<>("Mode", GUIMode.DROPDOWN);

    public ClickGUI() {
        setSuffixListener(modeProperty);
    }

    @Override
    public void onEnable() {
        openGui();
        toggle();
    }

    @EventLink
    private final Listener<KeyPressEvent> onKeyPress = event -> {
        if (event.getKey() == Keyboard.KEY_INSERT || event.getKey() == getKey()) {
            openGui();
        }
    };

    public void openGui() {
        if (modeProperty.getValue() == GUIMode.DROPDOWN) {
            Wrapper.getMinecraft().displayGuiScreen(DropdownGUI.getInstance());
        } else {
            SkeetUI.getInstance().open();
        }
    }

    public GUIMode getMode() {
        return modeProperty.getValue();
    }
}
