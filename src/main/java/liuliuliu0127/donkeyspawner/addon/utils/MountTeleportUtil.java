package liuliuliu0127.donkeyspawner.addon.utils;

import liuliuliu0127.donkeyspawner.addon.modules.BetterEntityControl;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class MountTeleportUtil {
    private static final Minecraft mc = MeteorClient.mc;
    private static final int SADDLE_SLOT_ID = 0;
    private static final int MAX_SESSION_TICKS = 40;

    public static boolean utilspoofsaddle = false;

    private static Session session;

    public static boolean teleportMount(Vec3 target, boolean useGlitch) {
        if (mc.player == null || mc.level == null) return false;

        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) return false;

        BetterEntityControl bec = Modules.get().get(BetterEntityControl.class);
        if (bec == null || !bec.isActive()) return false;

        if (!useGlitch) return startTeleport(vehicle, target, bec);
        if (session != null) return false;

        session = Session.create(vehicle, target);
        if (session == null) return false;

        switch (session.type) {
            case HORSE -> {
                if (session.hadSaddle) {
                    session.stage = Stage.PREPARE_REMOVE_SADDLE;
                    requestMountInventory();
                    return true;
                }

                enableSpoof(vehicle);
                return startTeleport(vehicle, target, bec);
            }
            case PIG, STRIDER -> {
                prepareRod(session);
                enableSpoof(vehicle);
                return startTeleport(vehicle, target, bec);
            }
            default -> {
                clearSession();
                return false;
            }
        }
    }

    public static void tick() {
        if (session == null || mc.player == null || mc.level == null) {
            clearSession();
            return;
        }

        session.ticks++;
        if (session.ticks > MAX_SESSION_TICKS) {
            if (session.stage == Stage.RESTORE_SADDLE) {
                clearSession();
            } else if (session.type == MountType.HORSE && session.hadSaddle) {
                utilspoofsaddle = false;
                session.stage = Stage.RESTORE_SADDLE;
                session.ticks = 0;
                requestMountInventory();
            } else {
                restoreSession();
                clearSession();
            }
            return;
        }

        switch (session.stage) {
            case PREPARE_REMOVE_SADDLE -> {
                if (!tryOpenMountInventory()) return;

                if (removeSaddleToInventory(session)) {
                    closeContainer();
                    enableSpoof(session.vehicle);
                    startTeleport(session.vehicle, session.target, Modules.get().get(BetterEntityControl.class));
                }
            }
            case RESTORE_SADDLE -> {
                if (!tryOpenMountInventory()) return;

                if (session.hadSaddle) {
                    equipSaddleFromInventory(session);
                } else {
                    equipAndRemoveSaddle(session);
                }

                closeContainer();
                clearSession();
            }
            case RESTORE_ROD -> {
                restoreRod(session);
                clearSession();
            }
            default -> {
            }
        }
    }

    public static void onTeleportConsumed(Entity vehicle) {
        if (session == null || session.vehicle != vehicle) return;

        utilspoofsaddle = false;
        session.stage = switch (session.type) {
            case HORSE -> Stage.RESTORE_SADDLE;
            case PIG, STRIDER -> Stage.RESTORE_ROD;
            default -> Stage.DONE;
        };

        if (session.stage == Stage.RESTORE_SADDLE) requestMountInventory();
    }

    public static void cancel() {
        restoreSession();
        clearSession();
    }

    public static boolean shouldSpoofSaddle(Entity entity) {
        return utilspoofsaddle && session != null && session.vehicle == entity;
    }

    public static int findSaddleInInventory() {
        return findItemInInventory(Items.SADDLE);
    }

    private static boolean startTeleport(Entity vehicle, Vec3 target, BetterEntityControl bec) {
        if (bec == null) return false;

        bec.pendingTpTarget = target;
        bec.isTeleporting = true;

        if (session != null && session.vehicle == vehicle) {
            session.stage = Stage.TELEPORTING;
            session.ticks = 0;
        }

        return true;
    }

    private static void enableSpoof(Entity vehicle) {
        if (session != null && session.vehicle == vehicle) utilspoofsaddle = true;
    }

    private static void restoreSession() {
        if (session == null) return;

        utilspoofsaddle = false;

        if (session.type == MountType.PIG || session.type == MountType.STRIDER) {
            restoreRod(session);
        }
    }

    private static void clearSession() {
        utilspoofsaddle = false;
        session = null;
    }

    private static void prepareRod(Session session) {
        Player player = mc.player;
        if (player == null || session.rodItem == null) return;

        session.originalSelectedSlot = player.getInventory().getSelectedSlot();
        ItemStack mainHand = player.getMainHandItem();
        session.hadRodInMainHand = mainHand.getItem() == session.rodItem;

        if (!session.hadRodInMainHand) return;

        int emptySlot = findEmptyMainInventorySlot();
        if (emptySlot == -1) emptySlot = findEmptySlotExcluding(session.originalSelectedSlot);
        if (emptySlot == -1) return;

        InvUtils.move().from(session.originalSelectedSlot).to(emptySlot);
        session.rodBackupSlot = emptySlot;
    }

    private static void restoreRod(Session session) {
        if (mc.player == null || session.rodItem == null) return;

        if (session.hadRodInMainHand) {
            if (session.rodBackupSlot != -1) {
                InvUtils.move().from(session.rodBackupSlot).to(session.originalSelectedSlot);
            }
            return;
        }

        quickSwitchRod(session.rodItem, session.originalSelectedSlot);
    }

    private static void quickSwitchRod(Item rodItem, int originalSelectedSlot) {
        int rodSlot = findItemInInventory(rodItem);
        if (rodSlot == -1) return;

        int hotbarSlot = rodSlot;
        int movedFrom = -1;

        if (!SlotUtils.isHotbar(rodSlot)) {
            int emptyHotbar = findEmptyHotbarSlot();
            if (emptyHotbar == -1) return;

            movedFrom = rodSlot;
            hotbarSlot = emptyHotbar;
            InvUtils.move().from(rodSlot).to(hotbarSlot);
        }

        InvUtils.swap(hotbarSlot, true);
        InvUtils.swapBack();

        if (movedFrom != -1) {
            InvUtils.move().from(hotbarSlot).to(movedFrom);
        }

        if (originalSelectedSlot >= 0 && originalSelectedSlot <= 8) {
            InvUtils.swap(originalSelectedSlot, false);
        }
    }

    private static boolean removeSaddleToInventory(Session session) {
        if (!(session.vehicle instanceof LivingEntity living)) return false;
        if (living.getItemBySlot(EquipmentSlot.SADDLE).isEmpty()) return true;

        int emptySlot = findEmptySlotExcluding(mc.player.getInventory().getSelectedSlot());
        if (emptySlot == -1) return false;

        clickSlotId(SADDLE_SLOT_ID);
        clickInventorySlot(emptySlot);
        session.saddleInventorySlot = emptySlot;
        return true;
    }

    private static boolean equipSaddleFromInventory(Session session) {
        if (!(session.vehicle instanceof LivingEntity living)) return false;
        if (!living.getItemBySlot(EquipmentSlot.SADDLE).isEmpty()) return true;

        int saddleSlot = session.saddleInventorySlot != -1 ? session.saddleInventorySlot : findSaddleInInventory();
        if (saddleSlot == -1) return false;

        clickInventorySlot(saddleSlot);
        clickSlotId(SADDLE_SLOT_ID);
        return true;
    }

    private static boolean equipAndRemoveSaddle(Session session) {
        if (!(session.vehicle instanceof LivingEntity living)) return false;
        if (!living.getItemBySlot(EquipmentSlot.SADDLE).isEmpty()) {
            return removeSaddleToInventory(session);
        }

        int saddleSlot = findSaddleInInventory();
        if (saddleSlot == -1) return false;

        clickInventorySlot(saddleSlot);
        clickSlotId(SADDLE_SLOT_ID);
        clickSlotId(SADDLE_SLOT_ID);
        clickInventorySlot(saddleSlot);
        return true;
    }

    private static boolean tryOpenMountInventory() {
        if (mc.player == null) return false;

        if (mc.player.containerMenu instanceof HorseInventoryMenu) return true;

        requestMountInventory();
        return false;
    }

    private static void requestMountInventory() {
        if (mc.player == null || mc.player.getVehicle() == null) return;
        mc.player.sendOpenInventory();
    }

    private static void closeContainer() {
        if (mc.player != null && mc.player.containerMenu != mc.player.inventoryMenu) {
            mc.player.closeContainer();
        }
    }

    private static void clickInventorySlot(int inventorySlot) {
        int slotId = SlotUtils.indexToId(inventorySlot);
        if (slotId != -1) clickSlotId(slotId);
    }

    private static void clickSlotId(int slotId) {
        if (mc.player == null || mc.gameMode == null) return;
        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slotId, 0, ClickType.PICKUP, mc.player);
    }

    private static int findItemInInventory(Item item) {
        if (mc.player == null) return -1;

        int size = mc.player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == item) return i;
        }

        return -1;
    }

    private static int findEmptySlotExcluding(int excludeSlot) {
        if (mc.player == null) return -1;

        int size = mc.player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            if (i == excludeSlot) continue;
            if (mc.player.getInventory().getItem(i).isEmpty()) return i;
        }

        return -1;
    }

    private static int findEmptyMainInventorySlot() {
        if (mc.player == null) return -1;

        for (int i = SlotUtils.MAIN_START; i <= SlotUtils.MAIN_END; i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) return i;
        }

        return -1;
    }

    private static int findEmptyHotbarSlot() {
        if (mc.player == null) return -1;

        for (int i = SlotUtils.HOTBAR_START; i <= SlotUtils.HOTBAR_END; i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) return i;
        }

        return -1;
    }

    private enum MountType {
        HORSE,
        PIG,
        STRIDER,
        UNSUPPORTED
    }

    private enum Stage {
        PREPARE_REMOVE_SADDLE,
        TELEPORTING,
        RESTORE_SADDLE,
        RESTORE_ROD,
        DONE
    }

    private static class Session {
        private final Entity vehicle;
        private final Vec3 target;
        private final MountType type;
        private final boolean hadSaddle;
        private final Item rodItem;

        private Stage stage = Stage.DONE;
        private int ticks;
        private int saddleInventorySlot = -1;
        private int originalSelectedSlot = -1;
        private int rodBackupSlot = -1;
        private boolean hadRodInMainHand;

        private Session(Entity vehicle, Vec3 target, MountType type, boolean hadSaddle, Item rodItem) {
            this.vehicle = vehicle;
            this.target = target;
            this.type = type;
            this.hadSaddle = hadSaddle;
            this.rodItem = rodItem;
        }

        private static Session create(Entity vehicle, Vec3 target) {
            if (vehicle instanceof AbstractHorse || vehicle instanceof Camel) {
                boolean hadSaddle = vehicle instanceof LivingEntity living && !living.getItemBySlot(EquipmentSlot.SADDLE).isEmpty();
                return new Session(vehicle, target, MountType.HORSE, hadSaddle, null);
            }

            if (vehicle instanceof Pig) {
                return new Session(vehicle, target, MountType.PIG, false, Items.CARROT_ON_A_STICK);
            }

            if (vehicle instanceof Strider) {
                return new Session(vehicle, target, MountType.STRIDER, false, Items.WARPED_FUNGUS_ON_A_STICK);
            }

            return null;
        }
    }
}
