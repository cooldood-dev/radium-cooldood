package vip.radium.module.impl.visuals;

import vip.radium.module.Module;
import vip.radium.module.ModuleCategory;
import vip.radium.module.ModuleInfo;
import vip.radium.module.ModuleManager;
import vip.radium.property.Property;
import vip.radium.property.impl.DoubleProperty;

@ModuleInfo(label = "Item Physics", category = ModuleCategory.VISUALS)
public final class ItemPhysics extends Module {

    public final DoubleProperty rotateSpeed = new DoubleProperty("Rotate Speed", 1.0, 0.1, 5.0, 0.1);

    public static ItemPhysics getInstance() {
        return ModuleManager.getInstance(ItemPhysics.class);
    }

    public static boolean isItemPhysicsEnabled() {
        ItemPhysics module = getInstance();
        return module != null && module.isEnabled();
    }
}

