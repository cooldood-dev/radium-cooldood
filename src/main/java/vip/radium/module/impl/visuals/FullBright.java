package vip.radium.module.impl.visuals;

import io.github.nevalackin.homoBus.annotations.EventLink;
import io.github.nevalackin.homoBus.Listener;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import vip.radium.event.impl.player.UpdatePositionEvent;
import vip.radium.module.Module;
import vip.radium.module.ModuleCategory;
import vip.radium.module.ModuleInfo;
import vip.radium.property.impl.EnumProperty;
import vip.radium.utils.Wrapper;

@ModuleInfo(label = "Full Bright", category = ModuleCategory.VISUALS)
public final class FullBright extends Module {

    private final EnumProperty<Mode> modeProperty = new EnumProperty<>("Mode", Mode.POTION);

    private float lastGamma;
    private boolean addedNv;
    private boolean hadNv;

    @Override
    public void onEnable() {
        if (Wrapper.getGameSettings() != null) {
            lastGamma = Wrapper.getGameSettings().gammaSetting;
        }
        if (Wrapper.getPlayer() != null) {
            hadNv = Wrapper.getPlayer().isPotionActive(Potion.nightVision);
        } else {
            hadNv = false;
        }
    }

    @EventLink
    private final Listener<UpdatePositionEvent> onUpdate = event -> {
        if (event.isPre() && Wrapper.getPlayer() != null) {
            switch (modeProperty.getValue()) {
                case GAMMA:
                    if (!hadNv && Wrapper.getPlayer().isPotionActive(Potion.nightVision))
                        Wrapper.getPlayer().removePotionEffect(Potion.nightVision.id);
                    if (Wrapper.getGameSettings() != null) {
                        Wrapper.getGameSettings().gammaSetting = 1000.0F;
                    }
                    break;
                case POTION:
                    if (!hadNv) {
                        Wrapper.getPlayer().addPotionEffect(new PotionEffect(
                                Potion.nightVision.id, 260 * 20, 68));
                        addedNv = true;
                    }
                    break;
            }
        }
    };

    @Override
    public void onDisable() {
        switch (modeProperty.getValue()) {
            case POTION:
                if (addedNv && !hadNv && Wrapper.getPlayer() != null)
                    Wrapper.getPlayer().removePotionEffect(Potion.nightVision.id);
                break;
            case GAMMA:
                if (Wrapper.getGameSettings() != null) {
                    Wrapper.getGameSettings().gammaSetting = lastGamma;
                }
                break;
        }
    }

    private enum Mode {
        GAMMA, POTION
    }

}
