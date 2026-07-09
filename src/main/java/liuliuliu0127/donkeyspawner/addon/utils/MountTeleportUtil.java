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
    private static final int MAX_STAGE_TICKS = 80;
    private static final int CONFIRMED_TP_DELAY_TICKS = 5;

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
                    setStage(Stage.OPEN_REMOVE);
                    return true;
                }

                setStage(Stage.START_TP);
                return true;
            }
            case PIG, STRIDER -> {
                prepareRod(session);
                setStage(Stage.WAIT_ROD_REMOVED);
                return true;
            }
            default -> {
                clearSession();
                return false;
            }
        }
    }

    public static void tick() {
        if (session == null) return;

        if (mc.player == null || mc.level == null) {
            clearSession();
            return;
        }

        session.stageTicks++;

        if (session.stageTicks > MAX_STAGE_TICKS) {
            handleTimeout();
            return;
        }

        switch (session.stage) {
            case OPEN_REMOVE -> {
                if (ensureMountMenu()) setStage(Stage.TAKE_REMOVED_SADDLE);
            }
            case TAKE_REMOVED_SADDLE -> {
                if (!(session.vehicle instanceof LivingEntity living)) {
                    clearSession();
                    return;
                }

                if (living.getItemBySlot(EquipmentSlot.SADDLE).isEmpty()) {
                    session.removedSaddle = true;
                    setStage(Stage.CLOSE_AFTER_REMOVE);
                    return;
                }

                int emptySlot = findEmptyPlayerInventorySlotExcluding(mc.player.getInventory().getSelectedSlot());
                if (emptySlot == -1) {
                    clearSession();
                    return;
                }

                session.saddleInventorySlot = emptySlot;
                if (clickSlotId(SADDLE_SLOT_ID)) setStage(Stage.STORE_REMOVED_SADDLE);
            }
            case STORE_REMOVED_SADDLE -> {
                if (clickInventorySlot(session.saddleInventorySlot)) {
                    setStage(Stage.WAIT_SADDLE_REMOVED);
                }
            }
            case WAIT_SADDLE_REMOVED -> {
                if (isSaddleRemoved()) {
                    session.removedSaddle = true;
                    setStage(Stage.CLOSE_AFTER_REMOVE);
                }
            }
            case CLOSE_AFTER_REMOVE -> {
                closeContainer();
                setStage(Stage.START_TP);
            }
            case WAIT_ROD_REMOVED -> {
                if (isRodRemovedFromHands()) {
                    enableSpoof(session.vehicle);
                    setStage(Stage.START_TP);
                }
            }
            case START_TP -> {
                enableSpoof(session.vehicle);
                if (!canStartGlitchTeleport()) return;

                setStage(Stage.WAIT_CONFIRMED_TP);
            }
            case WAIT_CONFIRMED_TP -> {
                enableSpoof(session.vehicle);
                if (!canStartGlitchTeleport()) {
                    setStage(Stage.START_TP);
                    return;
                }

                if (session.stageTicks < CONFIRMED_TP_DELAY_TICKS) return;

                BetterEntityControl bec = Modules.get().get(BetterEntityControl.class);
                if (!startTeleport(session.vehicle, session.target, bec)) {
                    utilspoofsaddle = false;
                    setStage(session.removedSaddle ? Stage.OPEN_RESTORE : Stage.DONE);
                }
            }
            case TELEPORTING -> {
            }
            case OPEN_RESTORE -> {
                if (!session.hadSaddle && findSaddleInInventory() == -1) {
                    clearSession();
                    return;
                }

                if (ensureMountMenu()) setStage(Stage.PICK_RESTORE_SADDLE);
            }
            case PICK_RESTORE_SADDLE -> {
                int saddleSlot = getRestoreSaddleSlot();
                if (saddleSlot == -1) {
                    setStage(Stage.CLOSE_AFTER_RESTORE);
                    return;
                }

                session.saddleInventorySlot = saddleSlot;
                if (clickInventorySlot(saddleSlot)) setStage(Stage.PLACE_RESTORE_SADDLE);
            }
            case PLACE_RESTORE_SADDLE -> {
                if (clickSlotId(SADDLE_SLOT_ID)) setStage(Stage.WAIT_RESTORE_SADDLE_PLACED);
            }
            case WAIT_RESTORE_SADDLE_PLACED -> {
                if (isSaddleEquipped()) {
                    setStage(session.hadSaddle ? Stage.CLOSE_AFTER_RESTORE : Stage.TAKE_RESTORE_SADDLE);
                }
            }
            case TAKE_RESTORE_SADDLE -> {
                if (clickSlotId(SADDLE_SLOT_ID)) setStage(Stage.WAIT_RESTORE_SADDLE_REMOVED);
            }
            case WAIT_RESTORE_SADDLE_REMOVED -> {
                if (isSaddleRemoved()) setStage(Stage.STORE_RESTORE_SADDLE);
            }
            case STORE_RESTORE_SADDLE -> {
                if (clickInventorySlot(session.saddleInventorySlot)) setStage(Stage.CLOSE_AFTER_RESTORE);
            }
            case CLOSE_AFTER_RESTORE -> {
                closeContainer();
                clearSession();
            }
            case RESTORE_ROD -> {
                restoreRod(session);
                clearSession();
            }
            case DONE -> clearSession();
        }
    }

    public static void onTeleportConsumed(Entity vehicle) {
        if (session == null || session.vehicle != vehicle) return;

        utilspoofsaddle = false;

        switch (session.type) {
            case HORSE -> setStage(Stage.OPEN_RESTORE);
            case PIG, STRIDER -> setStage(Stage.RESTORE_ROD);
            default -> clearSession();
        }
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

        if (session != null && session.vehicle == vehicle) setStage(Stage.TELEPORTING);
        return true;
    }

    private static void handleTimeout() {
        if (session == null) return;

        utilspoofsaddle = false;

        switch (session.stage) {
            case TELEPORTING, WAIT_CONFIRMED_TP -> {
                if (session.type == MountType.HORSE && session.removedSaddle) {
                    setStage(Stage.OPEN_RESTORE);
                } else if (session.type == MountType.PIG || session.type == MountType.STRIDER) {
                    setStage(Stage.RESTORE_ROD);
                } else {
                    clearSession();
                }
            }
            case OPEN_REMOVE, TAKE_REMOVED_SADDLE, STORE_REMOVED_SADDLE, WAIT_SADDLE_REMOVED -> {
                closeContainer();
                clearSession();
            }
            case WAIT_ROD_REMOVED -> {
                restoreRod(session);
                clearSession();
            }
            case OPEN_RESTORE, PICK_RESTORE_SADDLE, PLACE_RESTORE_SADDLE, WAIT_RESTORE_SADDLE_PLACED, TAKE_RESTORE_SADDLE, WAIT_RESTORE_SADDLE_REMOVED, STORE_RESTORE_SADDLE -> {
                closeContainer();
                clearSession();
            }
            default -> clearSession();
        }
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

    private static void setStage(Stage stage) {
        if (session == null) return;

        session.stage = stage;
        session.stageTicks = 0;
    }

    private static boolean ensureMountMenu() {
        if (mc.player == null) return false;

        if (mc.player.containerMenu instanceof HorseInventoryMenu) return true;

        if (session == null || mc.player.getVehicle() != session.vehicle) return false;

        if (mc.player.containerMenu != mc.player.inventoryMenu) {
            closeContainer();
            return false;
        }

        mc.player.sendOpenInventory();
        return false;
    }

    private static boolean isSaddleRemoved() {
        return session != null
            && session.vehicle instanceof LivingEntity living
            && living.getItemBySlot(EquipmentSlot.SADDLE).isEmpty();
    }

    private static boolean isSaddleEquipped() {
        return session != null
            && session.vehicle instanceof LivingEntity living
            && !living.getItemBySlot(EquipmentSlot.SADDLE).isEmpty();
    }

    private static boolean canStartGlitchTeleport() {
        if (session == null) return false;
        if (!shouldSpoofSaddle(session.vehicle)) return false;

        return switch (session.type) {
            case HORSE -> isSaddleRemoved();
            case PIG, STRIDER -> isRodRemovedFromHands();
            default -> false;
        };
    }

    private static boolean isRodRemovedFromHands() {
        return session != null
            && mc.player != null
            && session.rodItem != null
            && mc.player.getMainHandItem().getItem() != session.rodItem
            && mc.player.getOffhandItem().getItem() != session.rodItem;
    }

    private static int getRestoreSaddleSlot() {
        if (session == null) return -1;

        if (session.saddleInventorySlot != -1
            && mc.player != null
            && mc.player.getInventory().getItem(session.saddleInventorySlot).getItem() == Items.SADDLE) {
            return session.saddleInventorySlot;
        }

        return findSaddleInInventory();
    }

    private static void prepareRod(Session session) {
        Player player = mc.player;
        if (player == null || session.rodItem == null) return;

        session.originalSelectedSlot = player.getInventory().getSelectedSlot();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        session.hadRodInMainHand = mainHand.getItem() == session.rodItem;
        session.hadRodInOffhand = offHand.getItem() == session.rodItem;

        if (!session.hadRodInMainHand && !session.hadRodInOffhand) return;

        if (session.hadRodInMainHand) {
            int emptySlot = findEmptyPlayerInventorySlotExcluding(session.originalSelectedSlot);
            if (emptySlot == -1) return;

            InvUtils.move().from(session.originalSelectedSlot).to(emptySlot);
            session.mainHandRodBackupSlot = emptySlot;
        }

        if (session.hadRodInOffhand) {
            int emptySlot = findEmptyPlayerInventorySlotExcluding(session.originalSelectedSlot);
            if (emptySlot == -1) return;

            InvUtils.move().from(SlotUtils.OFFHAND).to(emptySlot);
            session.offhandRodBackupSlot = emptySlot;
        }
    }

    private static void restoreRod(Session session) {
        if (mc.player == null || session.rodItem == null) return;

        if (session.hadRodInMainHand) {
            if (session.mainHandRodBackupSlot != -1) {
                InvUtils.move().from(session.mainHandRodBackupSlot).to(session.originalSelectedSlot);
            }
        }

        if (session.hadRodInOffhand) {
            if (session.offhandRodBackupSlot != -1) {
                InvUtils.move().from(session.offhandRodBackupSlot).to(SlotUtils.OFFHAND);
            }
        }

        if (session.hadRodInMainHand || session.hadRodInOffhand) {
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

    private static void closeContainer() {
        if (mc.player != null && mc.player.containerMenu != mc.player.inventoryMenu) {
            mc.player.closeContainer();
        }
    }

    private static boolean clickInventorySlot(int inventorySlot) {
        int slotId = SlotUtils.indexToId(inventorySlot);
        return slotId != -1 && clickSlotId(slotId);
    }

    private static boolean clickSlotId(int slotId) {
        if (mc.player == null || mc.gameMode == null) return false;
        if (!(mc.player.containerMenu instanceof HorseInventoryMenu)) return false;

        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slotId, 0, ClickType.PICKUP, mc.player);
        return true;
    }

    private static int findItemInInventory(Item item) {
        if (mc.player == null) return -1;

        int size = mc.player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == item) return i;
        }

        return -1;
    }

    private static int findEmptyPlayerInventorySlotExcluding(int excludeSlot) {
        if (mc.player == null) return -1;

        for (int i = SlotUtils.MAIN_START; i <= SlotUtils.MAIN_END; i++) {
            if (i == excludeSlot) continue;
            if (mc.player.getInventory().getItem(i).isEmpty()) return i;
        }

        for (int i = SlotUtils.HOTBAR_START; i <= SlotUtils.HOTBAR_END; i++) {
            if (i == excludeSlot) continue;
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
        OPEN_REMOVE,
        TAKE_REMOVED_SADDLE,
        STORE_REMOVED_SADDLE,
        WAIT_SADDLE_REMOVED,
        CLOSE_AFTER_REMOVE,
        WAIT_ROD_REMOVED,
        START_TP,
        WAIT_CONFIRMED_TP,
        TELEPORTING,
        OPEN_RESTORE,
        PICK_RESTORE_SADDLE,
        PLACE_RESTORE_SADDLE,
        WAIT_RESTORE_SADDLE_PLACED,
        TAKE_RESTORE_SADDLE,
        WAIT_RESTORE_SADDLE_REMOVED,
        STORE_RESTORE_SADDLE,
        CLOSE_AFTER_RESTORE,
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
        private int stageTicks;
        private int saddleInventorySlot = -1;
        private int originalSelectedSlot = -1;
        private int mainHandRodBackupSlot = -1;
        private int offhandRodBackupSlot = -1;
        private boolean removedSaddle;
        private boolean hadRodInMainHand;
        private boolean hadRodInOffhand;

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
