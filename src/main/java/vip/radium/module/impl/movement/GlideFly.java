package vip.radium.module.impl.movement;

import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.client.entity.EntityPlayerSP;
import vip.radium.event.EventBusPriorities;
import vip.radium.event.impl.player.MoveEntityEvent;
import vip.radium.event.impl.player.StepEvent;
import vip.radium.event.impl.player.UpdatePositionEvent;
import vip.radium.module.Module;
import vip.radium.module.ModuleCategory;
import vip.radium.module.ModuleInfo;
import vip.radium.property.Property;
import vip.radium.property.impl.DoubleProperty;
import vip.radium.utils.MovementUtils;
import vip.radium.utils.Wrapper;

/**
 * GlideFly — a smooth, anti-kick glide-based fly mode.
 *
 * Behaviour (based on Exhibition's "Glide" mode):
 *   - Glides at the Y level you enabled it from (startY).
 *   - Hold jump to ascend, hold sneak to descend.
 *   - Cannot ascend above startY (anti-kick measure).
 *   - Y motion is a gentle -0.01 drift when neutral so the server
 *     always sees slight downward movement, reducing kick risk.
 *   - Horizontal speed is configurable.
 *   - Timer boost is optional to help bypass NCP lag-backs.
 */
@ModuleInfo(label = "GlideFly", category = ModuleCategory.MOVEMENT)
public final class GlideFly extends Module {

    private final DoubleProperty speedProperty = new DoubleProperty("Speed", 1.5, 0.25, 5.0, 0.05);
    private final DoubleProperty timerBoostProperty = new DoubleProperty("Timer Boost", 0.0, 0.0, 3.0, 0.25);
    private final Property<Boolean> viewBobbingProperty = new Property<>("View Bobbing", true);

    /** The Y level at the moment the module was enabled — used as an ascent ceiling. */
    private double startY;

    @EventLink
    public final Listener<StepEvent> onStep = event -> event.setStepHeight(0.0F);

    @EventLink(EventBusPriorities.HIGHEST)
    public final Listener<MoveEntityEvent> onMove = e -> {
        final EntityPlayerSP player = Wrapper.getPlayer();
        if (player == null) return;

        final double speed = speedProperty.getValue();

        // Handle Y-axis
        if (player.movementInput.sneak) {
            player.motionY = -0.42F;
        } else if (Wrapper.getGameSettings().keyBindJump.isKeyDown()) {
            // Prevent ascending above where we started (anti-kick)
            boolean shouldBlock = player.posY + 0.1 >= startY;
            if (!shouldBlock) {
                player.motionY = 0.42F;
            } else {
                player.motionY = 0.0D;
            }
        } else {
            // Gentle drift down — server sees movement so kick-timers reset
            player.motionY = -0.01D;
        }

        e.setY(player.motionY);

        // Handle XZ speed
        if (MovementUtils.isMoving()) {
            final double yaw = Math.toRadians(player.rotationYaw);
            e.setX(-(Math.sin(yaw) * speed));
            e.setZ(Math.cos(yaw) * speed);
        }
    };

    @EventLink(EventBusPriorities.HIGHEST)
    public final Listener<UpdatePositionEvent> onUpdatePosition = e -> {
        if (!e.isPre()) return;
        final EntityPlayerSP player = Wrapper.getPlayer();
        if (player == null) return;

        // Zero out fall distance so we take no fall damage
        player.fallDistance = 0.0F;

        // View bobbing suppression
        if (viewBobbingProperty.getValue()) {
            player.cameraYaw = 0.105F;
        }

        // Apply timer boost (extra speed on NCP-based servers)
        final float boost = timerBoostProperty.getValue().floatValue();
        Wrapper.getTimer().timerSpeed = boost > 0.0F ? 1.0F + boost : 1.0F;
    };

    @Override
    public void onEnable() {
        final EntityPlayerSP player = Wrapper.getPlayer();
        startY = (player != null) ? player.posY : 0.0D;
        Step.cancelStep = true;
    }

    @Override
    public void onDisable() {
        Step.cancelStep = false;
        Wrapper.getTimer().timerSpeed = 1.0F;
    }
}
