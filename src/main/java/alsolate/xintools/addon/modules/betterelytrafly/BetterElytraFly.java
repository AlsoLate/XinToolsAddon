package alsolate.xintools.addon.modules.betterelytrafly;

import alsolate.xintools.addon.events.TravelEvent;
import alsolate.xintools.addon.events.MoveEvent;
import alsolate.xintools.addon.core.WaveXinModule;
import alsolate.xintools.addon.XinToolsAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class BetterElytraFly extends WaveXinModule {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final int ELYTRA_MAX_DAMAGE = new ItemStack(Items.ELYTRA).getMaxDamage();
    private static final int REPLACE_RETRY_DELAY_TICKS = 5;
    private static final int NO_REPLACEMENT_WARNING_DELAY_TICKS = 100;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgElytraReplace = settings.createGroup("Elytra Replace");

    public final Setting<Boolean> autoStop = sgGeneral.add(new BoolSetting.Builder()
        .name("Stop in Unloaded Chunks")
        .description("Stops flight movement when the current chunk is unloaded")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("Flight Speed")
        .description("Horizontal flight speed")
        .defaultValue(2)
        .min(0.1)
        .sliderMin(0.1)
        .max(20)
        .sliderMax(20)
        .build()
    );

    public final Setting<Double> downSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("Descent Speed")
        .description("Descent speed")
        .defaultValue(1)
        .min(0.1)
        .sliderMin(0.1)
        .max(3)
        .sliderMax(3)
        .build()
    );

    private final Setting<Boolean> elytraReplace = sgElytraReplace.add(new BoolSetting.Builder()
        .name("Enabled")
        .description("Automatically replaces a nearly broken equipped elytra")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> durabilityThreshold = sgElytraReplace.add(new IntSetting.Builder()
        .name("Durability Threshold")
        .description("Replaces the equipped elytra when its remaining durability reaches this value")
        .defaultValue(2)
        .min(1)
        .max(ELYTRA_MAX_DAMAGE - 1)
        .sliderMin(1)
        .sliderMax(ELYTRA_MAX_DAMAGE - 1)
        .visible(elytraReplace::get)
        .build()
    );

    private final Setting<Boolean> chatFeedback = sgElytraReplace.add(new BoolSetting.Builder()
        .name("Chat Feedback")
        .description("Shows elytra replacement and compatibility messages in chat")
        .defaultValue(true)
        .visible(elytraReplace::get)
        .build()
    );

    private final Setting<Boolean> onlyWhileFlying = sgElytraReplace.add(new BoolSetting.Builder()
        .name("Only While Flying")
        .description("Only replaces the equipped elytra while the player is gliding")
        .defaultValue(false)
        .visible(elytraReplace::get)
        .build()
    );

    private final Setting<Boolean> inventoryTweaksCompatibility = sgElytraReplace.add(new BoolSetting.Builder()
        .name("InventoryTweaks Compatibility")
        .description("Temporarily disables Inventory Tweaks while replacing the elytra")
        .defaultValue(true)
        .visible(elytraReplace::get)
        .build()
    );

    private final Setting<Integer> compatibilityDelay = sgElytraReplace.add(new IntSetting.Builder()
        .name("Compatibility Delay")
        .description("Ticks to wait before restoring Inventory Tweaks after a replacement")
        .defaultValue(10)
        .min(1)
        .max(60)
        .sliderMin(1)
        .sliderMax(60)
        .visible(() -> elytraReplace.get() && inventoryTweaksCompatibility.get())
        .build()
    );

    private boolean hasElytra;
    private boolean inventoryTweaksWasActive;
    private int inventoryTweaksRestoreCountdown;
    private int replaceRetryCountdown;
    private int noReplacementWarningCountdown;

    public BetterElytraFly() {
        super(
            XinToolsAddon.CATEGORY,
            "better-elytra-fly",
            "Improves elytra flight control and can automatically replace a nearly broken elytra."
        );
    }

    @Override
    public void onActivate() {
        if (mc.player != null) {
            if (!mc.player.isCreative()) mc.player.getAbilities().mayfly = false;
            mc.player.getAbilities().flying = false;
        }

        hasElytra = false;
        inventoryTweaksWasActive = false;
        inventoryTweaksRestoreCountdown = 0;
        replaceRetryCountdown = 0;
        noReplacementWarningCountdown = 0;
    }

    @Override
    public void onDeactivate() {
        hasElytra = false;
        restoreInventoryTweaks();
        inventoryTweaksRestoreCountdown = 0;
        replaceRetryCountdown = 0;
        noReplacementWarningCountdown = 0;

        if (mc.player != null) {
            if (!mc.player.isCreative()) mc.player.getAbilities().mayfly = false;
            mc.player.getAbilities().flying = false;
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) {
            hasElytra = false;
            return;
        }

        ItemStack chestStack = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        hasElytra = isUsableElytra(chestStack);
    }

    @EventHandler
    private void onElytraReplaceTick(TickEvent.Post event) {
        updateReplacementCountdowns();

        if (!elytraReplace.get()) {
            restoreInventoryTweaks();
            return;
        }

        if (mc.player == null || mc.level == null || replaceRetryCountdown > 0) return;

        ItemStack equipped = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!equipped.is(Items.ELYTRA)) return;
        if (onlyWhileFlying.get() && !mc.player.isFallFlying()) return;

        int remainingDurability = remainingDurability(equipped);
        if (remainingDurability > durabilityThreshold.get()) return;

        int[] replacementDurability = {0};
        FindItemResult replacement = InvUtils.find(stack -> {
            if (!stack.is(Items.ELYTRA)) return false;

            int remaining = remainingDurability(stack);
            if (remaining <= durabilityThreshold.get()) return false;

            replacementDurability[0] = remaining;
            return true;
        });

        if (!replacement.found()) {
            warnNoReplacement();
            return;
        }

        temporarilyDisableInventoryTweaks();
        InvUtils.move().from(replacement.slot()).toArmor(2);
        replaceRetryCountdown = REPLACE_RETRY_DELAY_TICKS;
        noReplacementWarningCountdown = 0;

        if (chatFeedback.get()) {
            infoKey(
                "message.wavexin.better_elytra_fly.elytra_replaced",
                "Replaced elytra (remaining durability: %d -> %d).",
                remainingDurability,
                replacementDurability[0]
            );
        }

        if (inventoryTweaksWasActive) {
            inventoryTweaksRestoreCountdown = Math.max(
                inventoryTweaksRestoreCountdown,
                compatibilityDelay.get()
            );
        }
    }

    private void updateReplacementCountdowns() {
        if (replaceRetryCountdown > 0) replaceRetryCountdown--;
        if (noReplacementWarningCountdown > 0) noReplacementWarningCountdown--;

        if (inventoryTweaksRestoreCountdown > 0) {
            inventoryTweaksRestoreCountdown--;
            if (inventoryTweaksRestoreCountdown == 0) restoreInventoryTweaks();
        }
    }

    private void warnNoReplacement() {
        if (!chatFeedback.get() || noReplacementWarningCountdown > 0) return;

        warningKey(
            "warning.wavexin.better_elytra_fly.no_replacement_elytra",
            "No replacement elytra was found with durability above %d.",
            durabilityThreshold.get()
        );
        noReplacementWarningCountdown = NO_REPLACEMENT_WARNING_DELAY_TICKS;
    }

    private void temporarilyDisableInventoryTweaks() {
        if (!inventoryTweaksCompatibility.get() || inventoryTweaksWasActive) return;

        InventoryTweaks inventoryTweaks = Modules.get().get(InventoryTweaks.class);
        if (inventoryTweaks == null || !inventoryTweaks.isActive()) return;

        inventoryTweaksWasActive = true;
        inventoryTweaks.toggle();

        if (chatFeedback.get()) {
            infoKey(
                "message.wavexin.better_elytra_fly.inventory_tweaks_disabled",
                "Temporarily disabled Inventory Tweaks."
            );
        }
    }

    private void restoreInventoryTweaks() {
        if (!inventoryTweaksWasActive) return;

        InventoryTweaks inventoryTweaks = Modules.get().get(InventoryTweaks.class);
        if (inventoryTweaks != null && !inventoryTweaks.isActive()) {
            inventoryTweaks.toggle();

            if (chatFeedback.get()) {
                infoKey(
                    "message.wavexin.better_elytra_fly.inventory_tweaks_restored",
                    "Restored Inventory Tweaks."
                );
            }
        }

        inventoryTweaksWasActive = false;
    }

    protected final Vec3 getRotationVector(float pitch, float yaw) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3(i * j, -k, h * j);
    }

    public final Vec3 getRotationVec(float tickDelta) {
        return this.getRotationVector(0, mc.player.getYRot(tickDelta));
    }

    public static boolean recastElytra(LocalPlayer player) {
        if (checkConditions(player) && ignoreGround(player)) {
            player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            return true;
        }
        return false;
    }

    public static boolean checkConditions(LocalPlayer player) {
        ItemStack itemStack = player.getItemBySlot(EquipmentSlot.CHEST);
        return !player.getAbilities().flying
            && !player.isPassenger()
            && !player.onClimbable()
            && itemStack.is(Items.ELYTRA)
            && isUsableElytra(itemStack);
    }

    private static boolean ignoreGround(LocalPlayer player) {
        if (!player.isInWater() && !player.hasEffect(MobEffects.LEVITATION)) {
            ItemStack itemStack = player.getItemBySlot(EquipmentSlot.CHEST);
            if (isUsableElytra(itemStack)) {
                return true;
            }
        }
        return false;
    }

    public static double[] directionSpeedKey(double speed) {
        if (mc.player == null) return new double[]{0, 0};

        float forward = (mc.options.keyUp.isDown() ? 1 : 0) + (mc.options.keyDown.isDown() ? -1 : 0);
        float side = (mc.options.keyLeft.isDown() ? 1 : 0) + (mc.options.keyRight.isDown() ? -1 : 0);
        float yaw = mc.player.getYRot();

        if (forward != 0.0f) {
            if (side > 0.0f) yaw += forward > 0.0f ? -45 : 45;
            else if (side < 0.0f) yaw += forward > 0.0f ? 45 : -45;
            side = 0.0f;
            if (forward > 0.0f) forward = 1.0f;
            else if (forward < 0.0f) forward = -1.0f;
        }

        double sin = Math.sin(Math.toRadians(yaw + 90.0f));
        double cos = Math.cos(Math.toRadians(yaw + 90.0f));
        double posX = forward * speed * cos + side * speed * sin;
        double posZ = forward * speed * sin - side * speed * cos;
        return new double[]{posX, posZ};
    }

    @EventHandler
    public void onPlayerMove(MoveEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;

        if (mc.player.isFallFlying()) {
            ChunkPos playerChunk = mc.player.chunkPosition();

            if (autoStop.get()) {
                if (mc.level.getChunkSource().getChunk(playerChunk.x, playerChunk.z, ChunkStatus.FULL, false) == null) {
                    event.setX(0);
                    event.setY(0);
                    event.setZ(0);
                }
            }
        }
    }

    @EventHandler
    public void onMove(TravelEvent event) {
        if (mc.player == null || mc.level == null || !hasElytra || !mc.player.isFallFlying() || event.isPost()) return;

        Vec3 lookVec = getRotationVec(1.0f);
        double lookDist = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
        double motionDist = Math.sqrt(getX() * getX() + getZ() * getZ());

        if (mc.options.keyShift.isDown()) {
            setY(-downSpeed.get());
        } else if (!mc.options.keyJump.isDown()) {
            setY(-0.0D);
        }

        if (mc.options.keyJump.isDown()) {
            if (motionDist > 0 / 10) {
                double rawUpSpeed = motionDist * 0.01325D;
                setY(getY() + rawUpSpeed * 3.2D);
                setX(getX() - lookVec.x * rawUpSpeed / lookDist);
                setZ(getZ() - lookVec.z * rawUpSpeed / lookDist);
            } else {
                double[] dir = directionSpeedKey(speed.get());
                setX(dir[0]);
                setZ(dir[1]);
            }
        }

        if (lookDist > 0.0D) {
            setX(getX() + (lookVec.x / lookDist * motionDist - getX()) * 0.1D);
            setZ(getZ() + (lookVec.z / lookDist * motionDist - getZ()) * 0.1D);
        }

        if (!mc.options.keyJump.isDown()) {
            double[] dir = directionSpeedKey(speed.get());
            setX(dir[0]);
            setZ(dir[1]);
        }

        setY(getY() * 0.9900000095367432D);
        setX(getX() * 0.9800000190734863D);
        setZ(getZ() * 0.9900000095367432D);

        event.cancel();
        mc.player.move(MoverType.SELF, mc.player.getDeltaMovement());
    }

    private double getX() {
        return mc.player.getDeltaMovement().x;
    }

    private double getY() {
        return mc.player.getDeltaMovement().y;
    }

    private double getZ() {
        return mc.player.getDeltaMovement().z;
    }

    private void setX(double f) {
        Vec3 currentVel = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(new Vec3(f, currentVel.y, currentVel.z));
    }

    private void setY(double f) {
        Vec3 currentVel = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(new Vec3(currentVel.x, f, currentVel.z));
    }

    private void setZ(double f) {
        Vec3 currentVel = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(new Vec3(currentVel.x, currentVel.y, f));
    }

    private static int remainingDurability(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    private static boolean isUsableElytra(ItemStack stack) {
        return stack.is(Items.ELYTRA) && stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    @Override
    public String getInfoString() {
        if (!elytraReplace.get() || mc.player == null) return null;

        ItemStack equipped = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!equipped.is(Items.ELYTRA)) return null;
        return Integer.toString(remainingDurability(equipped));
    }
}
