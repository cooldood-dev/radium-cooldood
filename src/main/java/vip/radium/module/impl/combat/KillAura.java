package vip.radium.module.impl.combat;

import io.github.nevalackin.homoBus.Listener;
import io.github.nevalackin.homoBus.annotations.EventLink;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import vip.radium.RadiumClient;
import vip.radium.event.EventBusPriorities;
import vip.radium.event.impl.packet.PacketReceiveEvent;
import vip.radium.event.impl.packet.PacketSendEvent;
import vip.radium.event.impl.player.StepEvent;
import vip.radium.event.impl.player.UpdatePositionEvent;
import vip.radium.event.impl.render.Render3DEvent;
import vip.radium.module.Module;
import vip.radium.module.ModuleCategory;
import vip.radium.module.ModuleInfo;
import vip.radium.module.ModuleManager;
import vip.radium.module.impl.movement.Flight;
import vip.radium.module.impl.movement.NoSlowdown;
import vip.radium.module.impl.movement.Speed;
import vip.radium.module.impl.player.Scaffold;
import vip.radium.property.Property;
import vip.radium.property.impl.DoubleProperty;
import vip.radium.property.impl.EnumProperty;
import vip.radium.utils.HypixelUtil;
import vip.radium.utils.InventoryUtils;
import vip.radium.utils.MathUtils;
import vip.radium.utils.PlayerUtils;
import vip.radium.utils.PriorityManager;
import vip.radium.utils.RotationUtils;
import vip.radium.utils.TeamUtils;
import vip.radium.utils.Timer;
import vip.radium.utils.Wrapper;
import vip.radium.utils.ncp.Angle;
import vip.radium.utils.ncp.Direction;
import vip.radium.utils.ncp.Location;
import vip.radium.utils.render.Colors;
import vip.radium.utils.render.RenderingUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(label = "Kill Aura", category = ModuleCategory.COMBAT, key = Keyboard.KEY_R)
public final class KillAura extends Module {

    private static final C08PacketPlayerBlockPlacement BLOCK_PACKET = new C08PacketPlayerBlockPlacement(
            new BlockPos(-0.0F, -0.0F, -0.0F), 255, null, 0.0f, 0.0f, 0.0f);

    public static int waitTicks;
    public static int setupTick;
    public static boolean blockJump;
    public static boolean wantedToJump;
    public static boolean wantsToStep;
    public static int stepDelay;
    public static boolean allowCrits;

    // Timers
    private final Timer delay = new Timer();
    private final Timer fakeSwingTimer = new Timer();
    private final Timer deathTimer = new Timer();
    private final Timer switchTimer = new Timer();
    private final Timer blockTimer = new Timer();
    private final Timer angleTimer = new Timer();

    // Numbers / Properties
    public final DoubleProperty range = new DoubleProperty("Range", 4.5, 1.0, 7.0, 0.1);
    public final DoubleProperty blockRange = new DoubleProperty("Block Range", 4.5, 1.0, 10.0, 0.1);
    public final DoubleProperty fov = new DoubleProperty("FOV", 180.0, 1.0, 180.0, 1.0);
    public final DoubleProperty angleStep = new DoubleProperty("Angle Step", 180.0, -1.0, 180.0, 5.0);
    public final DoubleProperty existed = new DoubleProperty("Existed", 50.0, 1.0, 120.0, 5.0);
    public final DoubleProperty minAPS = new DoubleProperty("Min APS", 5.0, 1.0, 20.0, 1.0);
    public final DoubleProperty maxAPS = new DoubleProperty("Max APS", 15.0, 1.0, 20.0, 1.0);
    public final DoubleProperty predictionTicks = new DoubleProperty("Prediction Ticks", 1.0, 1.0, 10.0, 1.0);
    public final DoubleProperty predictionScale = new DoubleProperty("Prediction Scale", 1.0, 0.0, 2.0, 0.05);
    public final DoubleProperty maxTargets = new DoubleProperty("Max Targets", 4.0, 1.0, 10.0, 1.0);
    public final DoubleProperty switchDelay = new DoubleProperty("Switch Delay", 300.0, 50.0, 1000.0, 50.0);

    // Enums
    public final EnumProperty<Priority> priority = new EnumProperty<>("Priority", Priority.ANGLE);
    public final EnumProperty<AttackMode> attackMode = new EnumProperty<>("Attack Mode", AttackMode.SMART);

    // Booleans
    public final Property<Boolean> exclusiveMode = new Property<>("Priority Only", false);
    public final Property<Boolean> autoBlock = new Property<>("Autoblock", true);
    public final Property<Boolean> autoShovel = new Property<>("Auto Shovel", false);
    public final Property<Boolean> pitSpawn = new Property<>("Pit Spawn", true);
    public final Property<Boolean> antiLag = new Property<>("Anti-Lag", true);
    public final Property<Boolean> reduce = new Property<>("Reduce", false);
    public final Property<Boolean> antiCritFunky = new Property<>("Anti-Crit Funky", false);
    public final Property<Boolean> prediction = new Property<>("Prediction", true);
    public final Property<Boolean> noswing = new Property<>("No Swing", false);
    public final Property<Boolean> indicator = new Property<>("Indicator", false);
    public final Property<Boolean> deathCheck = new Property<>("Death Check", true);
    public final Property<Boolean> raytrace = new Property<>("Raytrace", true);
    public final Property<Boolean> particles = new Property<>("Particles", false);
    public final Property<Boolean> stepCompat = new Property<>("Step Compat", true);

    // Target filters
    public final Property<Boolean> targetPlayers = new Property<>("Players", true);
    public final Property<Boolean> targetMobs = new Property<>("Mobs", false);
    public final Property<Boolean> targetPassive = new Property<>("Passive", false);
    public final Property<Boolean> targetVillagers = new Property<>("Villagers", false);
    public final Property<Boolean> targetGolems = new Property<>("Golems", false);
    public final Property<Boolean> targetInvisibles = new Property<>("Invisibles", false);
    public final Property<Boolean> targetTeams = new Property<>("Teams", false);
    public final Property<Boolean> targetArmorOnly = new Property<>("Armor Only", false);
    public final Property<Boolean> targetFriends = new Property<>("Friends", false);

    private final HashMap<EntityLivingBase, EntityDelta> deltaHashMap = new HashMap<>();

    private int index;
    private boolean isCritSetup;
    private int nextRandom = -1;
    public Vector2f lastAngles = new Vector2f(0, 0);
    public int swapped = -1;

    private final UUID[] whitelistedUUIDs = {
            UUID.fromString("3ea40f14-cbcf-4191-ba3b-c126ca334714"),
            UUID.fromString("ff33c84a-03de-4919-9160-b461f10f4657"),
            UUID.fromString("fb6d1e25-9c98-4865-ac77-6a307493abfa")
    };

    public List<EntityLivingBase> loaded = new CopyOnWriteArrayList<>();
    public boolean isBlocking;
    public EntityLivingBase target;
    public boolean shouldToggle;
    private boolean disabled;
    private int wait;
    private int critWaitTicks;

    public KillAura() {
        setSuffixListener(priority);
    }

    public static KillAura getInstance() {
        return ModuleManager.getInstance(KillAura.class);
    }

    public static EntityLivingBase getTarget() {
        KillAura killaura = getInstance();
        return (killaura != null && killaura.isEnabled()) ? killaura.target : null;
    }

    public static boolean isAutoBlocking() {
        KillAura aura = getInstance();
        return aura != null && aura.isEnabled() && aura.autoBlock.getValue() && aura.isBlocking;
    }

    public static double getEffectiveHealth(final EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            final EntityPlayer player = (EntityPlayer) entity;
            return player.getHealth() * (PlayerUtils.getTotalArmorProtection(player) / 20.0);
        } else {
            return entity.getHealth();
        }
    }

    @Override
    public void onEnable() {
        deltaHashMap.clear();
        disabled = false;
        allowCrits = true;
        if (Wrapper.getPlayer() != null) {
            lastAngles.x = Wrapper.getPlayer().rotationYaw;
            lastAngles.y = Wrapper.getPlayer().rotationPitch;
        }
        isBlocking = false;
        isCritSetup = false;
        blockJump = false;
        wantsToStep = false;
        wantedToJump = false;
        wait = 0;
        stepDelay = -2;
        setupTick = 0;
        swapped = -1;
        shouldToggle = false;
        critWaitTicks = 0;
        target = null;
        loaded.clear();
    }

    @Override
    public void onDisable() {
        if (Wrapper.getPlayer() != null) {
            lastAngles.x = Wrapper.getPlayer().rotationYaw;
            lastAngles.y = Wrapper.getPlayer().rotationPitch;
            if (isBlocking) {
                Wrapper.sendPacketDirect(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            }
        }
        deltaHashMap.clear();
        disabled = false;
        allowCrits = true;
        isBlocking = false;
        isCritSetup = false;
        blockJump = false;
        wantsToStep = false;
        wantedToJump = false;
        wait = -1;
        stepDelay = -2;
        setupTick = 0;
        swapped = -1;
        shouldToggle = false;
        critWaitTicks = 0;
        target = null;
        loaded.clear();
    }

    @EventLink
    public final Listener<PacketReceiveEvent> onPacketReceive = event -> {
        if (Wrapper.getPlayer() == null || Wrapper.getWorld() == null) return;

        Packet packet = event.getPacket();

        if (packet instanceof S08PacketPlayerPosLook) {
            critWaitTicks = 6;
            setupTick = 0;
        }

        if (packet instanceof S45PacketTitle && deathCheck.getValue()) {
            S45PacketTitle titlePacket = (S45PacketTitle) packet;
            if (titlePacket.getType().equals(S45PacketTitle.Type.TITLE)) {
                String text = StringUtils.stripControlCodes(titlePacket.getMessage().getFormattedText());
                if ((text.toLowerCase().contains("died") || text.toLowerCase().contains("game over")) && isEnabled()) {
                    shouldToggle = true;
                }
            }
        }
    };

    @EventLink
    public final Listener<StepEvent> onStep = event -> {
        if (Wrapper.getPlayer() == null || Wrapper.getWorld() == null) return;

        Criticals critModule = ModuleManager.getInstance(Criticals.class);
        boolean crits = (critModule != null && critModule.isEnabled() && critWaitTicks <= 0) &&
                (!ModuleManager.getInstance(Speed.class).isEnabled() && !ModuleManager.getInstance(Flight.class).isEnabled());

        if (event.isPre() && crits && stepDelay > 0) {
            critWaitTicks = 3;
            KillAura.wantsToStep = true;
            event.setStepHeight(0.0F);
        }
        if (!event.isPre()) {
            KillAura.wantsToStep = false;
            critWaitTicks = 2;
            stepDelay = 0;
        }
    };

    @EventLink
    public final Listener<Render3DEvent> onRender3D = event -> {
        if (Wrapper.getPlayer() == null || Wrapper.getWorld() == null) return;

        if (indicator.getValue()) {
            GL11.glPushMatrix();
            RenderingUtil.pre3D();
            Minecraft mc = Wrapper.getMinecraft();
            mc.entityRenderer.setupCameraTransform(event.getPartialTicks(), 2);

            try {
                for (EntityLivingBase targetEntity : loaded) {
                    GL11.glPushMatrix();
                    double[] p = getPrediction(targetEntity, predictionTicks.getValue().intValue(), predictionScale.getValue());

                    double x = (targetEntity.prevPosX + (targetEntity.posX - targetEntity.prevPosX) * event.getPartialTicks()) - RenderManager.renderPosX + p[0];
                    double y = (targetEntity.prevPosY + (targetEntity.posY - targetEntity.prevPosY) * event.getPartialTicks()) - RenderManager.renderPosY + p[1];
                    double z = (targetEntity.prevPosZ + (targetEntity.posZ - targetEntity.prevPosZ) * event.getPartialTicks()) - RenderManager.renderPosZ + p[2];
                    GL11.glTranslated(x, y, z);
                    GL11.glRotatef(-(targetEntity.prevRotationYawHead + (targetEntity.rotationYawHead - targetEntity.prevRotationYawHead) * event.getPartialTicks()), 0, 1, 0);
                    float collisSize = targetEntity.getCollisionBorderSize();

                    AxisAlignedBB var11 = targetEntity.getEntityBoundingBox().expand(collisSize, collisSize, collisSize);
                    AxisAlignedBB var12 = new AxisAlignedBB(var11.minX - targetEntity.posX, var11.minY - targetEntity.posY, var11.minZ - targetEntity.posZ,
                            var11.maxX - targetEntity.posX, var11.maxY - targetEntity.posY, var11.maxZ - targetEntity.posZ);
                    RenderingUtil.glColor(targetEntity != this.target ? Colors.getColor(255, 45) : targetEntity.hurtTime > 0 ? Colors.getColorOpacity(1186128252, 75) : Colors.getColor(255, 41, 41, 75));
                    RenderingUtil.drawBoundingBox(var12);
                    RenderingUtil.glColor(targetEntity != this.target ? Colors.getColor(255, 150) : targetEntity.hurtTime > 0 ? Colors.getColorOpacity(-5054084, 200) : Colors.getColor(255, 41, 41, 200));
                    GL11.glLineWidth(1);
                    RenderingUtil.drawOutlinedBoundingBox(var12);

                    GL11.glPopMatrix();
                }
            } catch (Exception ignored) {
            }

            RenderingUtil.post3D();
            GL11.glPopMatrix();
        }
    };

    @EventLink(EventBusPriorities.LOWEST)
    public final Listener<UpdatePositionEvent> onUpdatePositionEvent = event -> {
        Minecraft mc = Wrapper.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        Criticals critModule = ModuleManager.getInstance(Criticals.class);
        int maxTargetsCount = this.maxTargets.getValue().intValue();

        allowCrits = false;
        int min = minAPS.getValue().intValue();
        int max = maxAPS.getValue().intValue();
        if (min > max) {
            minAPS.setValue((double) max);
            min = max;
        }
        if (blockRange.getValue() < this.range.getValue()) {
            blockRange.setValue(this.range.getValue());
        }
        if (nextRandom == -1) {
            nextRandom = (int) Math.round(20.0 / randomInt(min, max));
        }

        boolean block = autoBlock.getValue();

        if (event.isPre()) {
            if (shouldToggle) {
                if (isBlocking) {
                    isBlocking = false;
                    Wrapper.sendPacketDirect(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                }
                toggle();
                return;
            }

            tickEntities();
            if (stepDelay > 0) stepDelay--;
            if (critWaitTicks > 0) critWaitTicks--;

            if (wantedToJump && mc.thePlayer.isCollidedVertically && mc.thePlayer.onGround) {
                if (blockJump) mc.thePlayer.jump();
                wantedToJump = false;
            }

            if (deathCheck.getValue()) {
                if (!mc.thePlayer.isEntityAlive() && !disabled) {
                    target = null;
                    loaded.clear();
                    toggle();
                    deathTimer.reset();
                    return;
                }
            }

            if (blockJump) blockJump = false;

            if (isBlocking && (mc.thePlayer.getCurrentEquippedItem() == null || !(mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemSword))) {
                isBlocking = false;
            }

            Scaffold scaffold = ModuleManager.getInstance(Scaffold.class);
            boolean disable = scaffold != null && scaffold.isEnabled();

            boolean ignorePit = HypixelUtil.isInGame("THE HYPIXEL PIT") && pitSpawn.getValue();
            if (ignorePit) {
                double x = mc.thePlayer.posX;
                double y = mc.thePlayer.posY;
                double z = mc.thePlayer.posZ;
                if (y > 90.0 && x < 30 && x > -30 && z < 30 && z > -30) {
                    disable = true;
                }
            }

            float rangeVal = this.range.getValue().floatValue();
            boolean crits = (critModule != null && critModule.isEnabled() && critWaitTicks <= 0) &&
                    ((!ModuleManager.getInstance(Speed.class).isEnabled() || (mc.thePlayer.onGround && !PlayerUtils.isMoving())) &&
                            !ModuleManager.getInstance(Flight.class).isEnabled()) &&
                    !PlayerUtils.isOnLiquid();

            AttackMode attack = attackMode.getValue();
            boolean single = maxTargetsCount == 1;
            if (single) {
                index = 0;
            }

            loaded = getTargets();
            if (index >= loaded.size()) {
                index = 0;
            }

            if (!mc.thePlayer.onGround && !mc.thePlayer.isCollidedVertically) {
                stepDelay = 1;
            }

            if (stepDelay < 0) {
                wantsToStep = false;
            }

            if (!loaded.isEmpty()) {
                sortList();
                for (EntityLivingBase e : loaded) {
                    double xDelta = e.posX - e.lastTickPosX;
                    double zDelta = e.posZ - e.lastTickPosZ;

                    if (deltaHashMap.containsKey(e)) {
                        deltaHashMap.get(e).logDeltas(xDelta, zDelta, mc.thePlayer.ticksExisted);
                    } else {
                        deltaHashMap.put(e, new EntityDelta(xDelta, zDelta));
                    }
                }

                if (switchTimer.delay((long) switchDelay.getValue().intValue()) && !single) {
                    EntityLivingBase lastTarget = target;
                    incrementIndex();
                    target = loaded.get(Math.min(loaded.size() - 1, index));

                    if (lastTarget == target && loaded.size() > 1) {
                        incrementIndex();
                        target = loaded.get(Math.min(loaded.size() - 1, index));
                    }

                    switchTimer.reset();
                }

                if (target == null || single) {
                    target = loaded.get(single ? 0 : Math.min(loaded.size() - 1, index));
                }

                if (target != null && !validEntity(target)) {
                    loaded = getTargets();
                    sortList();
                    if (!loaded.isEmpty()) {
                        target = loaded.get(0);
                        switchTimer.reset();
                    } else {
                        if (!isBlocking) blockTimer.reset();
                        lastAngles.x = mc.thePlayer.rotationYaw;
                        lastAngles.y = mc.thePlayer.rotationPitch;
                        target = null;
                        deltaHashMap.clear();
                        wait = 0;
                        setupTick = 0;
                    }
                }

                if (target != null) {
                    if (!disable) {
                        double[] p = getPrediction(target, predictionTicks.getValue().intValue(), predictionScale.getValue());
                        double xDiff = (target.posX + p[0]) - mc.thePlayer.posX;
                        double yDiff = (target.posY + p[1]) - mc.thePlayer.posY - 0.5;
                        double zDiff = (target.posZ + p[2]) - mc.thePlayer.posZ;

                        double yDifference = Math.abs((target.posY + p[2]) - mc.thePlayer.posY);
                        double dist = MathHelper.sqrt_double(xDiff * xDiff + zDiff * zDiff);
                        double distance = MathUtils.roundToPlace(dist, 1);

                        boolean shouldReduce = reduce.getValue() && distance <= 3;
                        float targetYaw = MathHelper.clamp_float(RotationUtils.getYawChangeGiven(target.posX, target.posZ, lastAngles.x), -180, 180);
                        int maxAngleStep = angleStep.getValue().intValue();

                        if (maxAngleStep > 5 && maxAngleStep < 175) {
                            maxAngleStep += (int) randomNumber(5, -5);
                        }

                        if (targetYaw > maxAngleStep) targetYaw = maxAngleStep;
                        else if (targetYaw < -maxAngleStep) targetYaw = -maxAngleStep;

                        if (maxAngleStep > -1 && !scaffold.isEnabled()) {
                            if (shouldReduce) {
                                float pitch = (float) -(Math.atan2(yDiff - (distance > 2.1 ? 1.25 : 1.5), dist) * 180.0D / Math.PI);
                                float newYaw = 0F;

                                if (distance <= (HypixelUtil.isInGame("DUEL") ? 1.12 : 0.75) && yDifference <= 1.5) {
                                    event.setYaw(lastAngles.x);
                                    event.setPitch(MathHelper.clamp_float(88.9F + (float) (0.5F * Math.random()), -89.5F, 89.5F));
                                } else {
                                    event.setPitch(MathHelper.clamp_float(pitch / 1.1F, -89.5F, 89.5F));

                                    Vec3 v = getDirection(lastAngles.x, event.getPitch());
                                    double off = Direction.directionCheck(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ), mc.thePlayer.getEyeHeight(), v,
                                            target.posX + p[0], target.posY + p[1] + target.height / 2D, target.posZ + p[2], target.width, target.height,
                                            HypixelUtil.isInGame("DUEL") ? 1.2 : HypixelUtil.isInGame("HYPIXEL PIT") ? 0.85 : 1);

                                    float tempNewYaw = (float) MathUtils.getIncremental(lastAngles.x + (targetYaw / 1.1F), 20);
                                    boolean willViolate = maxAngleStep > 0 && target.waitTicks <= 0 && Angle.INSTANCE.willViolateYaw(new Location(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, tempNewYaw, 0), target);

                                    if ((angleTimer.roundDelay(1000) && off >= 0.11 && !willViolate) || (angleTimer.roundDelay(200) && !willViolate && off >= 0.2)) {
                                        newYaw += targetYaw;
                                        angleTimer.reset();
                                        lastAngles.x = (float) MathUtils.getIncremental(lastAngles.x + newYaw, 20) + randomNumber(1, -1);
                                        event.setYaw(lastAngles.x);
                                    }
                                    event.setYaw(lastAngles.x);
                                }
                            } else {
                                float pitch = (float) -(Math.atan2(yDiff, dist) * 180.0D / Math.PI);
                                lastAngles.x += targetYaw / 1.1F;
                                event.setYaw(lastAngles.x);
                                event.setPitch(MathHelper.clamp_float(pitch / 1.1F, -89.5F, 89.5F));
                            }
                        }

                        lastAngles.y = event.getPitch();

                        boolean setupCrits = target.hurtTime <= 1 || (target.waitTicks <= 1);
                        boolean dontCrit = antiCritFunky.getValue() && hasEnchant(target, "Crit", "Funk");

                        if (target instanceof EntityPlayer && antiCritFunky.getValue() && hasEnchant(target, "Retro")) {
                            int criticalHits = ((EntityPlayer) target).criticalHits;
                            if ((criticalHits == 0 || criticalHits > 3) || target.waitTicks > 0) {
                                if (criticalHits == 0) {
                                    ((EntityPlayer) target).criticalHits++;
                                }
                                crits = false;
                                dontCrit = true;
                            }
                        }

                        if (!dontCrit) {
                            if (crits) {
                                boolean canAttackRightNow = (attack == AttackMode.ALWAYS) ||
                                        (attack == AttackMode.PRECISE ? target.waitTicks <= 0 :
                                                target.waitTicks <= 0 || (target.hurtResistantTime <= 10 && target.hurtResistantTime >= 7) || target.hurtTime > 7);

                                boolean isAttacking = mc.thePlayer.getDistanceToEntity(target) <= (mc.thePlayer.canEntityBeSeen(target) ? rangeVal : Math.min(3, rangeVal)) && delay.roundDelay(50L * nextRandom);

                                if (isAttacking && canAttackRightNow && isNextTickGround()) {
                                    if (mc.thePlayer.onGround && mc.thePlayer.isCollidedVertically) {
                                        stepDelay = 2;
                                        blockJump = true;
                                        event.setPosY(event.getPosY() + 0.125F);
                                        event.setOnGround(false);
                                        isCritSetup = true;
                                    }
                                }
                            } else {
                                setupTick = 0;
                            }
                        } else {
                            setupTick = 0;
                            isCritSetup = true;
                        }

                        if (!block && !mc.thePlayer.isBlocking() && isBlocking) {
                            isBlocking = false;
                            Wrapper.sendPacketDirect(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                        }

                        if (block && mc.thePlayer.inventory.getCurrentItem() != null && mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemSword) {
                            mc.thePlayer.setItemInUse(mc.thePlayer.getCurrentEquippedItem(), 0x11938);
                        }
                    }
                }

                int maxAngleStep = angleStep.getValue().intValue();
                if (maxAngleStep == -1) {
                    lastAngles.x = mc.thePlayer.rotationYaw;
                    lastAngles.y = mc.thePlayer.rotationPitch;
                }

            } else {
                if (!isBlocking) blockTimer.reset();
                lastAngles.x = mc.thePlayer.rotationYaw;
                lastAngles.y = mc.thePlayer.rotationPitch;
                deltaHashMap.clear();
                target = null;
                wait = 0;
                stepDelay = 0;
                setupTick = 0;
                wantsToStep = false;
            }

            boolean packetMode = ModuleManager.getInstance(NoSlowdown.class).isEnabled();
            if (mc.thePlayer.isBlocking() && isBlocking && packetMode && PlayerUtils.isMoving() && mc.thePlayer.ticksExisted % 2 == 0) {
                isBlocking = false;
                Wrapper.sendPacketDirect(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                blockTimer.reset();
            }

        } else if (!event.isPre()) {
            // POST Event
            Scaffold scaffold = ModuleManager.getInstance(Scaffold.class);
            boolean disable = scaffold != null && scaffold.isEnabled();

            if (!loaded.isEmpty() && loaded.get(Math.min(loaded.size() - 1, index)) != null && target != null && !disable) {
                boolean canCrit = (mc.thePlayer.fallDistance > 0.0625F && !mc.thePlayer.onGround);
                boolean isCriticalAttack = canCrit || isCritSetup;
                AttackMode attack = attackMode.getValue();

                boolean isOptimalAttack = attack == AttackMode.ALWAYS || (attack == AttackMode.PRECISE ? target.waitTicks <= 0 : (target.hurtTime <= 5));
                boolean shouldAttack = isCriticalAttack || isOptimalAttack;

                double[] p = getPrediction(target, predictionTicks.getValue().intValue(), predictionScale.getValue());
                double distance = mc.thePlayer.getDistance(target.posX + p[0], target.posY + p[1], target.posZ + p[2]);

                long attackDelay = 50L * nextRandom;
                float rangeVal = this.range.getValue().floatValue();
                boolean isAttacking = distance <= (mc.thePlayer.canEntityBeSeen(target) ? rangeVal : Math.min(3, rangeVal)) &&
                        (Wrapper.getTimer().timerSpeed <= 1 ? delay.roundDelay(attackDelay) : delay.delay(attackDelay));

                boolean canAttackRightNow = attack == AttackMode.ALWAYS || (attack == AttackMode.PRECISE ? target.waitTicks <= 0 :
                        target.waitTicks <= 0 || (target.hurtResistantTime <= 10 && target.hurtResistantTime >= 7) || target.hurtTime > 7);

                if (scaffold != null && scaffold.isEnabled()) {
                    lastAngles.x = event.getYaw();
                    lastAngles.y = event.getPitch();
                }

                boolean dontCrit = antiCritFunky.getValue() && hasEnchant(target, "Crit", "Funk");
                if (ModuleManager.getInstance(Speed.class).isEnabled() && dontCrit) {
                    if (!mc.thePlayer.onGround && mc.thePlayer.motionY < 0) {
                        isAttacking = false;
                    }
                }

                if (mc.thePlayer.isBlocking() && isAttacking && isBlocking) {
                    isBlocking = false;
                    Wrapper.sendPacketDirect(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                    blockTimer.reset();
                }

                if (isAttacking) {
                    Vec3 v = getDirection(event.getYaw(), event.getPitch());
                    int maxAngleStep = angleStep.getValue().intValue();

                    double off = maxAngleStep <= 0 ? 0 : Direction.directionCheck(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ), mc.thePlayer.getEyeHeight(), v,
                            target.posX + p[0], target.posY + p[1] + target.height / 2D, target.posZ + p[2], target.width, target.height,
                            HypixelUtil.isInGame("DUEL") ? 1.85 : HypixelUtil.isInGame("HYPIXEL PIT") ? 0.85 : 1);

                    if (maxAngleStep <= 0 || maxAngleStep >= 180 || off <= 0.6 || MathUtils.getIncremental(angleTimer.getDifference(), 50) <= 100) {

                        if (canAttackRightNow && shouldAttack) {
                            if (!noswing.getValue()) {
                                mc.thePlayer.swingItem();
                            } else {
                                mc.thePlayer.swingItemFake();
                            }

                            boolean whitelisted = false;
                            for (UUID whitelistedUUID : whitelistedUUIDs) {
                                if (whitelistedUUID.equals(target.getUniqueID())) {
                                    whitelisted = true;
                                    break;
                                }
                            }

                            if (!whitelisted) {
                                boolean shouldSilentSwap = autoShovel.getValue() && HypixelUtil.isInGame("PIT") &&
                                        (mc.thePlayer.inventory.getCurrentItem() == null || mc.thePlayer.inventory.getCurrentItem().getItem() != Items.diamond_shovel);

                                if (shouldSilentSwap && target.waitTicks <= 0) {
                                    int swapTo = InventoryUtils.findItemInInventory(Items.diamond_shovel);
                                    if (swapTo != -1) {
                                        swapped = swapTo;
                                        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, swapped, mc.thePlayer.inventory.currentItem, 2, mc.thePlayer);
                                        mc.playerController.updateController();
                                    }
                                }

                                Wrapper.sendPacketDirect(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));

                                if (swapped != -1) {
                                    mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, swapped, mc.thePlayer.inventory.currentItem, 2, mc.thePlayer);
                                    mc.playerController.updateController();
                                    swapped = -1;
                                }
                            }

                            if (target != null && target.waitTicks <= 0) {
                                if (target instanceof EntityPlayer) {
                                    EntityPlayer player = (EntityPlayer) target;
                                    if ((!event.isOnGround() && isCritSetup) || mc.thePlayer.fallDistance > 0) {
                                        player.criticalHits++;
                                    } else {
                                        if (player.criticalHits > 1) {
                                            if (antiCritFunky.getValue() && hasEnchant(target, "Retro")) {
                                                player.criticalHits = 0;
                                            }
                                        }
                                    }
                                }
                                target.waitTicks = 10;
                            }

                            delay.reset();
                            nextRandom = (int) Math.round(20.0 / randomInt(min, max));
                        } else {
                            if (attack != AttackMode.PRECISE && fakeSwingTimer.delay(nextRandom * 50L)) {
                                mc.thePlayer.swingItemFake();
                                fakeSwingTimer.setDifference(nextRandom > 2 ? 100 : 0);
                            }
                        }

                        isCritSetup = false;

                        if (particles.getValue()) {
                            float sharpLevel = EnchantmentHelper.func_152377_a(mc.thePlayer.inventory.getCurrentItem(), target.getCreatureAttribute());
                            if (sharpLevel > 0) mc.thePlayer.onEnchantmentCritical(target);
                            if ((mc.thePlayer.onGround && ModuleManager.getInstance(Criticals.class).isEnabled()) || (!mc.thePlayer.onGround && mc.thePlayer.fallDistance > 0.66)) {
                                mc.thePlayer.onCriticalHit(target);
                            }
                        }
                    }
                }

                if (wait <= 0 && mc.thePlayer.isBlocking() && !isBlocking) {
                    isBlocking = true;
                    Wrapper.sendPacketDirect(BLOCK_PACKET);
                }
            }

            wait--;

            if (loaded.isEmpty() && target == null && isBlocking && blockTimer.delay(50) && block) {
                blockTimer.reset();
                isBlocking = false;
                Wrapper.sendPacketDirect(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            }
        }
    };

    public boolean hasEnchant(EntityLivingBase target, String... enchants) {
        if (!HypixelUtil.isInGame("PIT")) return false;

        if (target instanceof EntityPlayer) {
            ItemStack pants = target.getEquipmentInSlot(2);
            if (HypixelUtil.isItemMystic(pants)) {
                for (String pitEnchant : HypixelUtil.getPitEnchants(pants)) {
                    for (String enchant : enchants) {
                        if (pitEnchant.contains(enchant)) return true;
                    }
                }
            }
        }
        return false;
    }

    private final double[] ZERO = new double[]{0, 0, 0};

    private double[] getPrediction(EntityLivingBase player, int ticks, double scale) {
        if (!prediction.getValue() || !deltaHashMap.containsKey(player)) {
            return ZERO;
        }

        EntityDelta delta = deltaHashMap.get(player);
        double[] weightedDeltas = delta.getWeightedDeltas();
        double yDelta = (player.posY - player.lastTickPosY);
        if (MathUtils.roundToPlace(yDelta, 1) != 0 || !player.onGround) {
            yDelta -= 0.08D;
            yDelta *= 0.9800000190734863D;
        }

        if (yDelta >= 0.5) yDelta = 0;

        double lastMotionY = yDelta;
        double currentPos = 0;
        double finalX = 0, finalY = 0, finalZ = 0;

        Minecraft mc = Wrapper.getMinecraft();
        for (int i = 0; i < ticks; i++) {
            double motionX = (weightedDeltas[0] * i);
            double motionZ = (weightedDeltas[1] * i);
            double motionY = lastMotionY;

            AxisAlignedBB playerBoundingBox = player.getEntityBoundingBox();
            AxisAlignedBB tempBoundingBox = new AxisAlignedBB(playerBoundingBox.minX, playerBoundingBox.minY, playerBoundingBox.minZ,
                    playerBoundingBox.maxX, playerBoundingBox.maxY, playerBoundingBox.maxZ).offset(motionX, currentPos, motionZ);

            final List<AxisAlignedBB> var15 = mc.theWorld.getCollidingBoundingBoxes(player, tempBoundingBox.addCoord(0, motionY, 0));
            for (final AxisAlignedBB var18 : var15) {
                motionY = var18.calculateYOffset(tempBoundingBox, motionY);
            }

            currentPos += motionY;
            finalX = motionX;
            finalY = motionY;
            finalZ = motionZ;

            motionY -= 0.08D;
            motionY *= 0.9800000190734863D;
            lastMotionY = motionY;
        }

        return new double[]{finalX * scale, finalY * scale, finalZ * scale};
    }

    private static class EntityDelta {
        private final ArrayBlockingQueue<double[]> deltas = new ArrayBlockingQueue<>(5);
        private int lastUpdatedTick;

        private EntityDelta(double initialDeltaX, double initialDeltaY) {
            deltas.add(new double[]{initialDeltaX, initialDeltaY});
        }

        private void logDeltas(double deltaX, double deltaY, int currentTick) {
            double distance = MathHelper.sqrt_double(deltaX * deltaX + deltaY * deltaY);

            if (distance >= 5) {
                if (deltas.remainingCapacity() == 0) deltas.remove();
                deltas.add(new double[]{0, 0});
                return;
            }

            if (currentTick - lastUpdatedTick > 5) deltas.clear();
            if (deltas.remainingCapacity() == 0) deltas.remove();

            lastUpdatedTick = currentTick;
            deltas.add(new double[]{deltaX, deltaY});
        }

        public double[] getWeightedDeltas() {
            int denominator = deltas.size();
            if (denominator == 0) return new double[]{0, 0};
            double deltaX = 0, deltaY = 0;
            for (double[] d : deltas) {
                deltaX += d[0];
                deltaY += d[1];
            }
            return new double[]{deltaX / denominator, deltaY / denominator};
        }
    }

    public Vec3 getDirection(float yaw, float pitch) {
        double y = -Math.sin(Math.toRadians(pitch));
        double xz = Math.cos(Math.toRadians(pitch));
        double x = -xz * Math.sin(Math.toRadians(yaw));
        double z = xz * Math.cos(Math.toRadians(yaw));
        return new Vec3(x, y, z);
    }

    public boolean isPosOnGround(double posX, double posY, double posZ) {
        Minecraft mc = Wrapper.getMinecraft();
        boolean isOnSlab = MathUtils.roundToPlace((posY - (int) posY), 1) == 0.5;
        Block nextBlockUnder = mc.theWorld.getBlockState(new BlockPos(posX, posY - (isOnSlab ? 0 : 0.1), posZ)).getBlock();
        boolean feetBlockAir = isOnSlab ? nextBlockUnder.getMaterial() == Material.air : (nextBlockUnder instanceof BlockSlab && !nextBlockUnder.isFullBlock()) || nextBlockUnder.getMaterial() == Material.air;
        return !feetBlockAir;
    }

    public boolean isNextTickGround() {
        Minecraft mc = Wrapper.getMinecraft();
        boolean forceTrue = !(stepCompat.getValue()) || !PlayerUtils.isMoving();
        boolean nextTickGround = false;

        double motionX = mc.thePlayer.posX - mc.thePlayer.lastTickPosX;
        double motionZ = mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ;

        double[][] offsets = new double[][]{new double[]{-0.351, -0.351}, new double[]{-0.351, 0.351}, new double[]{0.351, 0.351}, new double[]{0.351, -0.351}};
        for (double[] offset : offsets) {
            double offsetX = offset[0];
            double offsetZ = offset[1];

            double posX = offsetX + mc.thePlayer.posX + motionX;
            double posY = -0.45 + mc.thePlayer.posY;
            double posZ = offsetZ + mc.thePlayer.posZ + motionZ;

            if (isPosOnGround(posX, posY, posZ)) {
                nextTickGround = true;
                break;
            }
        }
        return forceTrue || nextTickGround;
    }

    private void tickEntities() {
        Minecraft mc = Wrapper.getMinecraft();
        for (Entity ent : mc.theWorld.getLoadedEntityList()) {
            if (ent instanceof EntityLivingBase) {
                ((EntityLivingBase) ent).waitTicks--;
            }
        }
    }

    private void incrementIndex() {
        index += 1;
        if (index >= loaded.size()) {
            index = 0;
        }
    }

    public boolean validEntity(EntityLivingBase entity) {
        if (entity == null) return false;
        Minecraft mc = Wrapper.getMinecraft();

        boolean players = targetPlayers.getValue();
        boolean animals = targetPassive.getValue();
        boolean mobs = targetMobs.getValue();
        boolean villager = targetVillagers.getValue();
        boolean golems = targetGolems.getValue();
        boolean friends = targetFriends.getValue();
        boolean invis = targetInvisibles.getValue();
        boolean teams = targetTeams.getValue();
        boolean armor = targetArmorOnly.getValue();

        float rangeVal = this.range.getValue().floatValue();
        float focusRange = rangeVal >= blockRange.getValue().floatValue() ?
                (mc.thePlayer.canEntityBeSeen(entity) ? rangeVal : Math.min(3, rangeVal)) : blockRange.getValue().floatValue();

        if ((mc.thePlayer.getHealth() > 0) && (entity.getHealth() > 0 && !entity.isDead && entity.deathTime <= 0) || Float.isNaN(entity.getHealth())) {
            boolean raytraceCheck = (!this.raytrace.getValue()) || (mc.thePlayer.canEntityBeSeen(entity));
            if (mc.thePlayer.getDistanceToEntity(entity) <= focusRange && raytraceCheck && entity.ticksExisted > existed.getValue().intValue()) {
                if (!isInFOV(entity)) return false;

                if (entity instanceof EntityPlayer && players) {
                    EntityPlayer ent = (EntityPlayer) entity;
                    AntiBot antiBot = ModuleManager.getInstance(AntiBot.class);
                    if ((antiBot != null && antiBot.isBot(ent)) || ent.isPlayerSleeping()) return false;
                    boolean isFriend = RadiumClient.getInstance().getPlayerManager().isFriend(ent);
                    return !(TeamUtils.isTeam(mc.thePlayer, ent) && teams) && !(ent.isInvisible() && !invis) && !(armor && !hasArmor(ent)) && (friends || !isFriend);
                }
                if (mobs && entity instanceof EntityWither && teams) {
                    return !TeamUtils.isTeam(mc.thePlayer, entity);
                }
                return (entity instanceof EntityMob || entity instanceof EntitySlime || entity instanceof EntityGhast || entity instanceof EntityDragon) && mobs ||
                        ((entity instanceof EntityAnimal || entity instanceof EntitySnowman || entity instanceof EntitySquid) && animals) ||
                        (entity instanceof EntityVillager && villager) || (entity instanceof EntityGolem && golems);
            }
        }
        return false;
    }

    private boolean hasArmor(EntityPlayer player) {
        return (player.inventory.armorInventory[0] != null) || (player.inventory.armorInventory[1] != null) ||
                (player.inventory.armorInventory[2] != null) || (player.inventory.armorInventory[3] != null);
    }

    private void sortList() {
        Minecraft mc = Wrapper.getMinecraft();
        Priority cur = priority.getValue();

        switch (cur) {
            case RANGE:
                loaded.sort(Comparator.comparingDouble(o -> o.getDistanceToEntity(mc.thePlayer)));
                break;
            case HEALTH:
                loaded.sort(Comparator.comparingDouble(EntityLivingBase::getHealth));
                break;
            case BOUNTY:
                loaded.sort(Comparator.comparingDouble(o -> o.getHealth() + (o instanceof EntityPlayer && PriorityManager.isPriority((EntityPlayer) o) ? -100D : 0)));
                break;
            case FOV:
                loaded.sort(Comparator.comparingDouble(o -> Math.abs(RotationUtils.getYawChange(o.posX, o.posZ))));
                break;
            case ANGLE:
                loaded.sort(Comparator.comparingDouble(o -> o.getDistanceToEntity(mc.thePlayer)));
                loaded.sort(Comparator.comparingDouble(o -> MathUtils.roundToPlace(RotationUtils.getRotations(o)[0], 15)));
                break;
            case ARMOR:
                loaded.sort(Comparator.comparingInt(o -> (o instanceof EntityPlayer ? ((EntityPlayer) o).inventory.getTotalArmorValue() : (int) o.getHealth())));
                break;
            case HEALTH_VAMP:
                loaded.sort(Comparator.comparingDouble(this::getTargetWeighted));
                break;
            case BOUNTY_VAMP:
                loaded.sort(Comparator.comparingDouble(this::getTargetWeightedBounty));
                break;
        }

        int maxTargetsCount = this.maxTargets.getValue().intValue();
        loaded = loaded.subList(0, Math.min(maxTargetsCount, loaded.size()));
    }

    private double getTargetWeightedBounty(EntityLivingBase entityLivingBase) {
        double weight = entityLivingBase.getHealth();
        int maxAngleStep = angleStep.getValue().intValue();
        Minecraft mc = Wrapper.getMinecraft();

        if (entityLivingBase instanceof EntityPlayer) {
            if (PriorityManager.isPriority((EntityPlayer) entityLivingBase) && entityLivingBase.hurtTime < 6) {
                weight -= 20;
            }
        }

        if ((mc.thePlayer.getHealth() / mc.thePlayer.getMaxHealth()) <= 0.975) {
            weight += Math.max(entityLivingBase.waitTicks, 0);
            if (entityLivingBase.hurtTime >= 6) weight += 10;

            if (maxAngleStep > 0) {
                float yawDiff = MathHelper.clamp_float(RotationUtils.getYawChangeGiven(entityLivingBase.posX, entityLivingBase.posZ, lastAngles.x), -180, 180);
                float forwardYawDiff = Math.abs(yawDiff);
                boolean willViolate = entityLivingBase.waitTicks <= 0 && Angle.INSTANCE.willViolateYaw(new Location(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, lastAngles.x + yawDiff, 0), entityLivingBase);
                if (willViolate) weight += 5;
                float estimatedYawChange = (float) MathUtils.getIncremental(forwardYawDiff, 20);
                if (estimatedYawChange >= 60) weight += estimatedYawChange / 20;
            }
        }
        return weight;
    }

    private double getTargetWeighted(EntityLivingBase entityLivingBase) {
        double weight = entityLivingBase.getHealth();
        int maxAngleStep = angleStep.getValue().intValue();
        Minecraft mc = Wrapper.getMinecraft();

        if (mc.thePlayer.getHealth() <= 19.5) {
            weight += Math.max(entityLivingBase.waitTicks, 0);
            if (entityLivingBase.hurtTime >= 6) weight += 10;

            if (maxAngleStep > 0) {
                float yawDiff = MathHelper.clamp_float(RotationUtils.getYawChangeGiven(entityLivingBase.posX, entityLivingBase.posZ, lastAngles.x), -180, 180);
                float forwardYawDiff = Math.abs(yawDiff);
                boolean willViolate = entityLivingBase.waitTicks <= 0 && Angle.INSTANCE.willViolateYaw(new Location(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, lastAngles.x + yawDiff, 0), entityLivingBase);
                if (willViolate) weight += 5;
                float estimatedYawChange = (float) MathUtils.getIncremental(forwardYawDiff, 20);
                if (estimatedYawChange >= 60) weight += estimatedYawChange / 20;
            }
        }
        return weight;
    }

    private List<EntityLivingBase> getTargets() {
        Minecraft mc = Wrapper.getMinecraft();
        List<EntityLivingBase> targets = new ArrayList<>();
        boolean priorityOnly = false;
        boolean allowPriorityOnly = priority.getValue() != Priority.BOUNTY_VAMP || exclusiveMode.getValue();

        for (Object o : mc.theWorld.getLoadedEntityList()) {
            if (o instanceof EntityLivingBase) {
                EntityLivingBase entity = (EntityLivingBase) o;
                if (validEntity(entity)) {
                    if (allowPriorityOnly) {
                        if (entity instanceof EntityPlayer && PriorityManager.isPriority((EntityPlayer) entity)) {
                            if (!priorityOnly) targets.clear();
                            priorityOnly = true;
                            targets.add(entity);
                        }
                    }
                    if (!priorityOnly) targets.add(entity);
                }
            }
        }
        return targets;
    }

    private boolean isInFOV(EntityLivingBase entity) {
        int fovVal = this.fov.getValue().intValue();
        return Math.abs(RotationUtils.getYawChange(entity.posX, entity.posZ)) <= fovVal &&
                Math.abs(RotationUtils.getPitchChange(entity, entity.posY)) <= fovVal;
    }

    private float randomNumber(float max, float min) {
        return min + (float) (Math.random() * (max - min));
    }

    private double randomInt(int min, int max) {
        return min + (Math.random() * (max - min));
    }

    public enum Priority {
        ANGLE("Angle"),
        RANGE("Range"),
        FOV("FOV"),
        ARMOR("Armor"),
        HEALTH("Health"),
        BOUNTY("Bounty"),
        HEALTH_VAMP("Health Vamp"),
        BOUNTY_VAMP("Bounty Vamp");

        private final String name;

        Priority(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum AttackMode {
        SMART("Smart"),
        PRECISE("Precise"),
        ALWAYS("Always");

        private final String name;

        AttackMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
