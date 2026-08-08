package alsolate.xintools.addon.modules;

import alsolate.xintools.addon.XinToolsAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class Automend extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> inventorySwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("Inventory Switch")
        .description("背包取瓶：当快捷栏没有经验瓶时，自动从背包中拿取经验瓶投掷。")
        .defaultValue(true)
        .build()
    );

    private boolean lastNoBottle = false;

    public Automend() {
        super(XinToolsAddon.CATEGORY, "Automend", "按住模块绑定按键持续向下投掷经验瓶修复装备，松开按键即停止。");
        // 按住绑定键开启模块、松开自动关闭（Meteor 标准 Hold 模式，按键绑定在模块 Bind 区块）
        toggleOnBindRelease = true;
    }

    @Override
    public void onActivate() {
        // 强制保持 Hold 模式：防止用户配置文件中保存的 toggleOnBindRelease=false 覆盖默认值
        toggleOnBindRelease = true;

        // 开启时检测：必须先绑定按键（模块设置中 Bind 区块），未绑定则提示
        if (!keybind.isSet()) {
            ChatUtils.sendMsg(Component.literal("[XinToolsAddon] Automend: 未绑定按键。请在模块设置的 Bind 区块绑定一个按键，按住即可持续投掷经验瓶，松开停止。").withStyle(ChatFormatting.RED));
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // 检测背包是否有经验瓶，根据 inventorySwitch 决定搜索范围
        FindItemResult exp;
        if (inventorySwitch.get()) {
            exp = InvUtils.find(stack -> stack.getItem() == Items.EXPERIENCE_BOTTLE, 0, 35);
        } else {
            exp = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
        }

        if (!exp.found()) {
            if (!lastNoBottle) {
                ChatUtils.sendMsg(Component.literal("[XinToolsAddon] Automend: 背包中没有足够的经验瓶。").withStyle(ChatFormatting.RED));
                lastNoBottle = true;
            }
            // 条件不符合：本次停止投掷，松开按键后（或补充经验瓶后）可再次触发
            return;
        }
        lastNoBottle = false;

        // 通过检测，原地向下投掷经验瓶（不转动视角）
        throwStraightDown(exp);
    }

    /** 原地向下投掷经验瓶：优先用手持槽，否则按 inventorySwitch 处理背包/快捷栏 */
    private void throwStraightDown(FindItemResult exp) {
        if (exp.getHand() != null) {
            throwDownWithHand(exp.getHand());
            return;
        }

        if (inventorySwitch.get() && exp.slot() >= 9) {
            // 从背包移动到当前手持槽
            final int backpackSlot = exp.slot();
            final int hotbarSlot = mc.player.getInventory().getSelectedSlot();
            InvUtils.move().from(backpackSlot).to(hotbarSlot);

            throwDownWithHand(InteractionHand.MAIN_HAND);

            // 如果投掷后还有剩余，移回背包
            ItemStack remain = mc.player.getInventory().getItem(hotbarSlot);
            if (!remain.isEmpty() && remain.getItem() == Items.EXPERIENCE_BOTTLE) {
                InvUtils.move().from(hotbarSlot).to(backpackSlot);
            }
        } else {
            // 快捷栏内交换，投掷，换回
            InvUtils.swap(exp.slot(), true);
            throwDownWithHand(InteractionHand.MAIN_HAND);
            InvUtils.swapBack();
        }
    }

    /** 以向下方向投掷手持物品：先同步服务端朝向为 pitch 90，再临时修改客户端朝向投掷后恢复（视角不闪动） */
    private void throwDownWithHand(InteractionHand hand) {
        // 发送服务端旋转包，让服务端认为玩家朝下（不改变客户端视角）
        mc.player.connection.getConnection().send(new ServerboundMovePlayerPacket.Rot(
            mc.player.getYRot(), 90.0f, mc.player.onGround(), false));

        // 临时修改客户端朝向使本地预测一致，同一帧内恢复，画面不闪
        float oldXRot = mc.player.getXRot();
        mc.player.setXRot(90.0f);
        mc.gameMode.useItem(mc.player, hand);
        mc.player.setXRot(oldXRot);
    }
}
