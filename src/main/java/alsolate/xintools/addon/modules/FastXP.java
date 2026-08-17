package alsolate.xintools.addon.modules;

import alsolate.xintools.addon.XinToolsAddon;
import alsolate.xintools.addon.mixin.AccessorMinecraftClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Fast XP thrower. Modes:
 * Silent / Vanilla — hold use key, optional look pitch (original FastXP)
 * Auto — while module is on, throw straight down (former Automend logic, no key bind required)
 */
public class FastXP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> yeetDelay = sgGeneral.add(new IntSetting.Builder()
        .name("throw-delay")
        .description("Delay between throws (item use cooldown ticks).")
        .defaultValue(0)
        .min(0)
        .sliderMax(10)
        .build()
    );

    private final Setting<RotationMode> rotMode = sgGeneral.add(new EnumSetting.Builder<RotationMode>()
        .name("rotation-mode")
        .description("Silent/Vanilla: hold use key. Auto: throw straight down while module is enabled.")
        .defaultValue(RotationMode.Silent)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Apply pitch rotation (Silent/Vanilla only).")
        .defaultValue(true)
        .visible(() -> rotMode.get() != RotationMode.Auto)
        .build()
    );

    private final Setting<Integer> pitch = sgGeneral.add(new IntSetting.Builder()
        .name("pitch")
        .description("Pitch angle for Silent/Vanilla modes.")
        .defaultValue(90)
        .range(-90, 90)
        .sliderMax(90)
        .visible(() -> rotMode.get() != RotationMode.Auto && rotate.get())
        .build()
    );

    private final Setting<Integer> repairThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("repair-threshold")
        .description("Repair Mending items when their durability percentage is below this value.")
        .defaultValue(20)
        .min(1)
        .max(99)
        .sliderMax(99)
        .visible(() -> rotMode.get() == RotationMode.Auto)
        .build()
    );

    private boolean lastNoBottle = false;
    private int autoCooldown;

    public FastXP() {
        super(
            XinToolsAddon.CATEGORY,
            "FastXP",
            "Throw experience bottles quickly. Auto mode throws straight down without holding a key."
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;

        if (rotMode.get() == RotationMode.Auto) {
            if (autoCooldown > 0) autoCooldown--;
            tickAuto();
            return;
        }

        if (mc.player.getMainHandItem().getItem() == Items.EXPERIENCE_BOTTLE && mc.options.keyUse.isDown()) {
            ((AccessorMinecraftClient) mc).setRightClickDelay(yeetDelay.get());

            if (rotate.get()) {
                if (rotMode.get() == RotationMode.Silent) {
                    Rotations.rotate(mc.player.getYRot(), pitch.get());
                } else if (rotMode.get() == RotationMode.Vanilla) {
                    mc.player.setXRot(pitch.get());
                }
            }
        }
    }

    private void tickAuto() {
        if (autoCooldown > 0 || !needsRepair()) return;

        FindItemResult exp = InvUtils.find(stack -> stack.getItem() == Items.EXPERIENCE_BOTTLE, 0, 35);

        if (!exp.found()) {
            if (!lastNoBottle) {
                ChatUtils.sendMsg(Component.literal("[XinTools] FastXP: no experience bottles.")
                    .withStyle(ChatFormatting.RED));
                lastNoBottle = true;
            }
            return;
        }
        lastNoBottle = false;

        ((AccessorMinecraftClient) mc).setRightClickDelay(yeetDelay.get());
        throwStraightDown(exp);
        autoCooldown = Math.max(1, yeetDelay.get() + 2);
    }

    private boolean needsRepair() {
        int threshold = repairThreshold.get();
        EquipmentSlot[] armorSlots = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (isBelowRepairThreshold(stack, threshold)) return true;
        }
        return isBelowRepairThreshold(mc.player.getMainHandItem(), threshold)
            || isBelowRepairThreshold(mc.player.getOffhandItem(), threshold);
    }

    private boolean isBelowRepairThreshold(ItemStack stack, int threshold) {
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) return false;
        var enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null
            || enchantments.getLevel(mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.MENDING)) <= 0) return false;
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        return remaining * 100L < stack.getMaxDamage() * (long) threshold;
    }

    private void throwStraightDown(FindItemResult exp) {
        if (exp.getHand() != null) {
            throwDownWithHand(exp.getHand());
            return;
        }

        if (exp.slot() >= 9) {
            final int backpackSlot = exp.slot();
            final int hotbarSlot = mc.player.getInventory().getSelectedSlot();
            InvUtils.move().from(backpackSlot).to(hotbarSlot);
            throwDownWithHand(InteractionHand.MAIN_HAND);
            ItemStack remain = mc.player.getInventory().getItem(hotbarSlot);
            if (!remain.isEmpty() && remain.getItem() == Items.EXPERIENCE_BOTTLE) {
                InvUtils.move().from(hotbarSlot).to(backpackSlot);
            }
        } else {
            InvUtils.swap(exp.slot(), true);
            throwDownWithHand(InteractionHand.MAIN_HAND);
            InvUtils.swapBack();
        }
    }

    private void throwDownWithHand(InteractionHand hand) {
        mc.player.connection.getConnection().send(new ServerboundMovePlayerPacket.Rot(
            mc.player.getYRot(), 90.0f, mc.player.onGround(), false));

        float oldXRot = mc.player.getXRot();
        mc.player.setXRot(90.0f);
        mc.gameMode.useItem(mc.player, hand);
        mc.player.setXRot(oldXRot);
    }

    public enum RotationMode {
        Auto,
        Silent,
        Vanilla
    }
}
