package vip.radium.module.impl.visuals;

import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import org.lwjgl.input.Keyboard;
import vip.radium.RadiumClient;
import vip.radium.event.impl.world.TickEvent;
import vip.radium.module.Module;
import vip.radium.module.ModuleCategory;
import vip.radium.module.ModuleInfo;
import vip.radium.property.impl.EnumProperty;
import vip.radium.utils.Wrapper;

@ModuleInfo(label = "Freelook", category = ModuleCategory.VISUALS)
public final class Freelook extends Module {

    private final EnumProperty<Mode> modeProperty = new EnumProperty<>("Mode", Mode.HOLD);

    private float cameraYaw;
    private float cameraPitch;
    private int previousPerspective;
    private boolean active;

    public Freelook() {
        setSuffixListener(modeProperty);
    }

    public static Freelook getInstance() {
        return RadiumClient.getInstance().getModuleManager().getModule(Freelook.class);
    }

    public static boolean isFreelooking() {
        Freelook freelook = getInstance();
        return freelook != null && freelook.isEnabled() && freelook.active;
    }

    public static float getCameraYaw() {
        Freelook freelook = getInstance();
        return freelook != null ? freelook.cameraYaw : 0.0F;
    }

    public static float getCameraPitch() {
        Freelook freelook = getInstance();
        return freelook != null ? freelook.cameraPitch : 0.0F;
    }

    public static void handleMouseInput(float yawDelta, float pitchDelta) {
        Freelook freelook = getInstance();
        if (freelook != null && freelook.isEnabled() && freelook.active) {
            freelook.cameraYaw += yawDelta * 0.15F;
            freelook.cameraPitch -= pitchDelta * 0.15F;
            freelook.cameraPitch = Math.max(-90.0F, Math.min(90.0F, freelook.cameraPitch));
        }
    }

    @Override
    public void onEnable() {
        if (Wrapper.getPlayer() != null && Wrapper.getGameSettings() != null) {
            cameraYaw = Wrapper.getPlayer().rotationYaw;
            cameraPitch = Wrapper.getPlayer().rotationPitch;
            previousPerspective = Wrapper.getGameSettings().thirdPersonView;
            Wrapper.getGameSettings().thirdPersonView = 1;
            active = true;
        }
    }

    @Override
    public void onDisable() {
        if (active) {
            active = false;
            if (Wrapper.getGameSettings() != null) {
                Wrapper.getGameSettings().thirdPersonView = previousPerspective;
            }
        }
    }

    @EventLink
    private final Listener<TickEvent> onTick = event -> {
        if (modeProperty.getValue() == Mode.HOLD) {
            int key = getKey();
            if (key != Keyboard.KEY_NONE) {
                if (!Keyboard.isKeyDown(key)) {
                    toggle();
                }
            }
        }
    };

    public enum Mode {
        TOGGLE,
        HOLD
    }
}
