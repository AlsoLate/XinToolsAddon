package liuliuliu0127.donkeyspawner.addon.modules;

import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class DsMaceKill extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMaceSpoof = settings.createGroup("Mace Spoof");
    private final SettingGroup sgMaceExploit = settings.createGroup("Mace Exloit");
    private final SettingGroup sgCombat = settings.createGroup("Combat");

    private final Setting<Double> initialFallHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("initial-fall-height")
        .description("Uses MaceSpoof when enabled during a normal fall higher than this distance from the ground.")
        .defaultValue(3.5)
        .range(1.0, 128.0)
        .sliderRange(1.0, 64.0)
        .build()
    );

    private final Setting<Boolean> autoAttack = sgCombat.add(new BoolSetting.Builder()
        .name("auto-attack")
        .description("Automatically attacks the closest selected entity.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Set<EntityType<?>>> targetEntities = sgCombat.add(new EntityTypeListSetting.Builder()
        .name("target-entities")
        .description("Entity types to attack.")
        .onlyAttackable()
        .defaultValue(EntityType.PLAYER)
        .visible(autoAttack::get)
        .build()
    );

    private final Setting<Double> attackRange = sgCombat.add(new DoubleSetting.Builder()
        .name("attack-range")
        .defaultValue(6.0)
        .range(1.0, 10.0)
        .sliderRange(1.0, 10.0)
        .visible(autoAttack::get)
        .build()
    );

    private final Setting<Integer> attackDelay = sgCombat.add(new IntSetting.Builder()
        .name("attack-delay-ms")
        .defaultValue(80)
        .range(0, 1000)
        .sliderRange(0, 500)
        .visible(autoAttack::get)
        .build()
    );

    private final Setting<Double> spoofMinFallDistance = sgMaceSpoof.add(new DoubleSetting.Builder()
        .name("min-fall-distance")
        .defaultValue(3.0)
        .range(1.0, 20.0)
        .sliderRange(1.0, 15.0)
        .build()
    );

    private final Setting<Double> spoofLandDistance = sgMaceSpoof.add(new DoubleSetting.Builder()
        .name("spoof-land-distance")
        .description("Sends the selected MaceSpoof packet sequence this far above the ground.")
        .defaultValue(2.5)
        .range(0.5, 8.0)
        .sliderRange(0.5, 5.0)
        .build()
    );

    private final Setting<SpoofMode> spoofMode = sgMaceSpoof.add(new EnumSetting.Builder<SpoofMode>()
        .name("spoof-mode")
        .defaultValue(SpoofMode.Fly)
        .build()
    );

    private final Setting<Boolean> useWindCharge = sgMaceExploit.add(new BoolSetting.Builder()
        .name("use-wind-charge")
        .description("Fires a wind charge downward at the start of each MaceExloit cycle.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> loopDelay = sgMaceExploit.add(new IntSetting.Builder()
        .name("loop-delay-ms")
        .defaultValue(400)
        .range(0, 3000)
        .sliderRange(0, 1500)
        .build()
    );

    private final Setting<Integer> fakeHeightDelay = sgMaceExploit.add(new IntSetting.Builder()
        .name("fake-height-delay-ms")
        .defaultValue(80)
        .range(0, 1000)
        .sliderRange(0, 500)
        .build()
    );

    private final Setting<Integer> strikeDelay = sgMaceExploit.add(new IntSetting.Builder()
        .name("strike-delay-ms")
        .defaultValue(80)
        .range(0, 1000)
        .sliderRange(0, 500)
        .build()
    );

    private final Setting<Double> fakeHeight = sgMaceExploit.add(new DoubleSetting.Builder()
        .name("fake-height")
        .defaultValue(4.0)
        .range(0.0, 20.0)
        .sliderRange(0.0, 10.0)
        .build()
    );

    private RunMode mode = RunMode.WaitingForLanding;
    private boolean initialized;
    private boolean spoofSentThisFall;
    private boolean cycleActive;
    private boolean fakeHeightSent;
    private long cycleStartedAt;
    private long lastCycleAt;
    private long lastAttackAt;
    private Entity cycleTarget;
    private KillAura killAura;
    private boolean restoreKillAura;

    public DsMaceKill() {
        super(DonkeySpawnerAddon.CATEGORY, "Ds Mace Kill", "Combines falling MaceSpoof combat with the MaceExloit attack loop.");
    }

    @Override
    public void onActivate() {
        initialized = false;
        spoofSentThisFall = false;
        resetMaceExploitCycle();
        lastCycleAt = 0;
        lastAttackAt = 0;

        killAura = Modules.get().get(KillAura.class);
        restoreKillAura = autoAttack.get() && killAura != null && killAura.isActive();
        if (restoreKillAura) killAura.disable();

        initializeMode();
    }

    @Override
    public void onDeactivate() {
        resetMaceExploitCycle();
        spoofSentThisFall = false;

        if (restoreKillAura && killAura != null && !killAura.isActive()) killAura.enable();
        restoreKillAura = false;
        killAura = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (!initialized) initializeMode();

        switch (mode) {
            case WaitingForLanding -> {
                if (mc.player.onGround()) {
                    mode = RunMode.MaceExloit;
                    lastCycleAt = 0;
                    resetMaceExploitCycle();
                }
            }
            case MaceSpoof -> tickMaceSpoof();
            case MaceExloit -> tickMaceExploit();
        }
    }

    private void initializeMode() {
        if (mc.player == null || mc.level == null) return;

        double distance = getGroundDistance(Math.max(256.0, initialFallHeight.get() + 16.0));
        mode = isNormalFalling() && distance > initialFallHeight.get()
            ? RunMode.MaceSpoof
            : (mc.player.onGround() ? RunMode.MaceExloit : RunMode.WaitingForLanding);
        initialized = true;
    }

    private void tickMaceSpoof() {
        if (mc.player.onGround()) {
            spoofSentThisFall = false;
            return;
        }

        if (!isNormalFalling()) return;

        Entity target = findTarget(attackRange.get());
        if (autoAttack.get()
            && target != null
            && mc.player.fallDistance >= spoofMinFallDistance.get()
            && hasElapsed(lastAttackAt, attackDelay.get())) {
            attackWithMace(target);
            lastAttackAt = System.currentTimeMillis();
        }

        if (!spoofSentThisFall
            && mc.player.fallDistance >= spoofMinFallDistance.get()
            && getGroundDistance(spoofLandDistance.get() + 2.0) <= spoofLandDistance.get()) {
            sendMaceSpoofPackets();
            spoofSentThisFall = true;
        }
    }

    private void tickMaceExploit() {
        Entity target = findTarget(attackRange.get());
        if (target == null) {
            resetMaceExploitCycle();
            return;
        }

        long now = System.currentTimeMillis();
        if (!cycleActive) {
            if (now - lastCycleAt < loopDelay.get()) return;

            cycleActive = true;
            fakeHeightSent = false;
            cycleStartedAt = now;
            cycleTarget = target;

            if (useWindCharge.get()) fireWindCharge();
            return;
        }

        long elapsed = now - cycleStartedAt;
        if (!fakeHeightSent && elapsed >= fakeHeightDelay.get()) {
            sendPosition(mc.player.getX(), mc.player.getY() + fakeHeight.get(), mc.player.getZ());
            fakeHeightSent = true;
        }

        if (elapsed >= strikeDelay.get()) {
            if (!fakeHeightSent) {
                sendPosition(mc.player.getX(), mc.player.getY() + fakeHeight.get(), mc.player.getZ());
                fakeHeightSent = true;
            }

            if (autoAttack.get() && isValidTarget(cycleTarget, attackRange.get())) attackWithMace(cycleTarget);
            lastCycleAt = now;
            resetMaceExploitCycle();
        }
    }

    private void fireWindCharge() {
        Rotations.rotate(mc.player.getYRot(), 90.0, () -> withMainHandItem(Items.WIND_CHARGE, () -> {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }));
    }

    private void attackWithMace(Entity target) {
        if (!isValidTarget(target, attackRange.get())) return;

        Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), () ->
            withMainHandItem(Items.MACE, () -> {
                if (!isValidTarget(target, attackRange.get())) return;
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
            })
        );
    }

    private boolean withMainHandItem(Item item, Runnable action) {
        FindItemResult result = InvUtils.find(item);
        if (!result.found()) return false;

        int sourceSlot = result.slot();
        int selectedSlot = mc.player.getInventory().getSelectedSlot();

        if (sourceSlot >= 0 && sourceSlot <= 8) {
            InvUtils.swap(sourceSlot, true);
            try {
                action.run();
            } finally {
                InvUtils.swapBack();
            }
            return true;
        }

        InvUtils.move().from(sourceSlot).to(selectedSlot);
        try {
            action.run();
        } finally {
            InvUtils.move().from(selectedSlot).to(sourceSlot);
        }
        return true;
    }

    private void sendMaceSpoofPackets() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        switch (spoofMode.get()) {
            case Fly -> {
                sendPosition(x, y + 1.16610926093821, z);
                sendPosition(x, y + 1.170005801788139, z);
                sendPosition(x, y + 1.2426308013947485, z);
                sendPosition(x, y + 2.3400880035762786, z);
                sendPosition(x, y + 2.640088003576279, z);
            }
            case Invalid -> {
                for (int i = 0; i < 20; i++) sendPosition(x, y + 1337.0, z);
            }
            case Normal -> sendPosition(x, y + 1.9, z);
            case ToVoid -> sendPosition(x, -70.0, z);
            case Rotation -> {
                mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(-180.0f, -90.0f, false, mc.player.horizontalCollision));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(180.0f, 90.0f, false, mc.player.horizontalCollision));
            }
        }
    }

    private void sendPosition(double x, double y, double z) {
        mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
            x, y, z, mc.player.getYRot(), mc.player.getXRot(), false, mc.player.horizontalCollision
        ));
    }

    private Entity findTarget(double range) {
        Entity best = null;
        double bestDistance = range;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isValidTarget(entity, range)) continue;

            double distance = mc.player.distanceTo(entity);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }

        return best;
    }

    private boolean isValidTarget(Entity entity, double range) {
        return entity != null
            && entity != mc.player
            && entity.isAlive()
            && !entity.isSpectator()
            && targetEntities.get().contains(entity.getType())
            && (!(entity instanceof Player player) || Friends.get().shouldAttack(player))
            && mc.player.distanceTo(entity) <= range;
    }

    private boolean isNormalFalling() {
        return !mc.player.onGround()
            && mc.player.getDeltaMovement().y < 0.0
            && mc.player.fallDistance > 0.0f
            && !mc.player.isFallFlying()
            && !mc.player.isPassenger()
            && !mc.player.isInWater()
            && !mc.player.isInLava()
            && !mc.player.getAbilities().flying
            && mc.player.getEffect(MobEffects.LEVITATION) == null
            && mc.player.getEffect(MobEffects.SLOW_FALLING) == null;
    }

    private double getGroundDistance(double maxDistance) {
        Vec3 start = mc.player.position().add(0.0, 0.01, 0.0);
        Vec3 end = start.add(0.0, -maxDistance, 0.0);
        BlockHitResult hit = mc.level.clip(new ClipContext(
            start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) return Double.POSITIVE_INFINITY;
        return Math.max(0.0, start.y - hit.getLocation().y);
    }

    private boolean hasElapsed(long time, int delay) {
        return time == 0 || System.currentTimeMillis() - time >= delay;
    }

    private void resetMaceExploitCycle() {
        cycleActive = false;
        fakeHeightSent = false;
        cycleStartedAt = 0;
        cycleTarget = null;
    }

    @Override
    public String getInfoString() {
        return switch (mode) {
            case WaitingForLanding -> "Waiting";
            case MaceSpoof -> "MaceSpoof";
            case MaceExloit -> "MaceExloit";
        };
    }

    private enum RunMode {
        WaitingForLanding,
        MaceSpoof,
        MaceExloit
    }

    private enum SpoofMode {
        Fly,
        Invalid,
        Normal,
        ToVoid,
        Rotation
    }
}
