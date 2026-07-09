package liuliuliu0127.donkeyspawner.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import liuliuliu0127.donkeyspawner.addon.utils.MountTeleportUtil;

/**
 * 骑乘实体传送模块
 * 右键点击目标位置，将骑乘的实体（马、骆驼、猪、炽足兽等）传送到准心指向的位置
 */
public class MountTP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // ----- 设置 -----
    private final Setting<Boolean> useGlitch = sgGeneral.add(new BoolSetting.Builder()
        .name("use-glitch")
        .description("(马假骑模式)use SpoofSaddle Glitch mode(auto equip or depuip saddle or rod)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> maxRange = sgGeneral.add(new IntSetting.Builder()
        .name("max-range")
        .description("max tp range")
        .defaultValue(64)
        .range(8, 256)
        .sliderRange(8, 256)
        .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("TP cool down(Tick)")
        .defaultValue(5)
        .range(0, 40)
        .sliderRange(0, 40)
        .build()
    );

    private final Setting<Boolean> requireEmptyHand = sgGeneral.add(new BoolSetting.Builder()
        .name("require-empty-hand")
        //.description("需要空手才能触发TP")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> debugOutput = sgGeneral.add(new BoolSetting.Builder()
        .name("debug-output")
        //.description("输出调试信息")
        .defaultValue(false)
        .build()
    );

    // ----- 状态变量 -----
    private boolean wasRightClick = false;
    private int cooldownCounter = 0;

    public MountTP() {
        super(DonkeySpawnerAddon.CATEGORY, "mount-tp", "right click to TP with mount(require enable better entity control)");
    }

    @Override
    public void onActivate() {
        wasRightClick = false;
        cooldownCounter = 0;
    }

    @Override
    public void onDeactivate() {
        wasRightClick = false;
        cooldownCounter = 0;
        MountTeleportUtil.cancel();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;
        MountTeleportUtil.tick();

        // 检查 BetterEntityControl 是否启用
        BetterEntityControl bec = Modules.get().get(BetterEntityControl.class);
        if (bec == null || !bec.isActive()) {
            if (debugOutput.get()) {
                ChatUtils.sendMsg(Component.literal("[MountTP] BetterEntityControl is not active").withStyle(ChatFormatting.RED));
            }
            return;
        }

        // 检查是否骑乘实体
        if (mc.player.getVehicle() == null) {
            if (debugOutput.get()) {
                ChatUtils.sendMsg(Component.literal("[MountTP] No vehicle").withStyle(ChatFormatting.RED));
            }
            return;
        }

        // 冷却递减
        if (cooldownCounter > 0) {
            cooldownCounter--;
            return;
        }

        // 检测右键按下（上升沿）
        boolean isRightClick = mc.options.keyUse.isDown();
        if (isRightClick && !wasRightClick) {
            // 检查是否需要空手
            if (requireEmptyHand.get()) {
                ItemStack mainHand = mc.player.getMainHandItem();
                if (!mainHand.isEmpty()) {
                    wasRightClick = isRightClick;
                    return;
                }
            }

            // 执行射线检测
            Vec3 target = raycastToPosition();
            if (target != null) {
                // 执行TP
                boolean success = MountTeleportUtil.teleportMount(target, useGlitch.get());
                if (success) {
                    cooldownCounter = cooldown.get();
                    if (debugOutput.get()) {
                        ChatUtils.sendMsg(Component.literal("[MountTP] Teleported to " + target.x + ", " + target.y + ", " + target.z)
                            .withStyle(ChatFormatting.GREEN));
                    }
                } else {
                    if (debugOutput.get()) {
                        ChatUtils.sendMsg(Component.literal("[MountTP] Teleport failed").withStyle(ChatFormatting.RED));
                    }
                }
            } else {
                if (debugOutput.get()) {
                    ChatUtils.sendMsg(Component.literal("[MountTP] No valid target found").withStyle(ChatFormatting.YELLOW));
                }
            }
        }
        wasRightClick = isRightClick;
    }

    /**
     * 射线检测 – 获取玩家准心指向的位置（固体方块表面上方）
     * @return 目标位置（Vec3），若无则返回 null
     */
    private Vec3 raycastToPosition() {
        if (mc.player == null || mc.level == null) return null;

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getViewVector(1.0F);
        double range = maxRange.get();

        HitResult hitResult = mc.level.clip(
            new ClipContext(
                eyePos,
                eyePos.add(lookVec.x * range, lookVec.y * range, lookVec.z * range),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                mc.player
            )
        );

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);

            if (state.canOcclude()) {
                // 检查区块是否已加载
                int chunkX = pos.getX() >> 4;
                int chunkZ = pos.getZ() >> 4;
                if (mc.level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                    // 返回方块中心上方位置（实体站在方块上方）
                    return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                }
            }
        }
        return null;
    }
}
