package alsolate.xintools.addon.modules;

import alsolate.xintools.addon.XinToolsAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerAlarms extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgJoin = settings.createGroup("Player Joining");
    private final SettingGroup sgLeave = settings.createGroup("Player Leaving");
    private final SettingGroup sgEnterRD = settings.createGroup("Player Entering Render Distance");
    private final SettingGroup sgLeaveRD = settings.createGroup("Player Leaving Render Distance");
    private final SettingGroup sgGamemode = settings.createGroup("Gamemode Change");

    // ==================== General ====================
    private final Setting<Boolean> showGamemodeInChat = sgGeneral.add(new BoolSetting.Builder()
        .name("show-gamemode-in-chat")
        .description("Show gamemode of the players in alarm messages in the chat.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> alarmOnJoin = sgGeneral.add(new BoolSetting.Builder()
        .name("alarm-on-join")
        .description("Notify when a player joins the server.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> alarmOnLeave = sgGeneral.add(new BoolSetting.Builder()
        .name("alarm-on-leave")
        .description("Notify when a player leaves the server.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> alarmOnEnterRD = sgGeneral.add(new BoolSetting.Builder()
        .name("alarm-on-enter-render-distance")
        .description("Notify when a player enters your render distance.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> alarmOnLeaveRD = sgGeneral.add(new BoolSetting.Builder()
        .name("alarm-on-leave-render-distance")
        .description("Notify when a player leaves your render distance.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> alarmOnGamemodeChange = sgGeneral.add(new BoolSetting.Builder()
        .name("alarm-on-gamemode-change")
        .description("Notify when a player changes gamemode.")
        .defaultValue(false)
        .build()
    );

    // ==================== Join Settings ====================
    private final Setting<Boolean> useJoinList = sgJoin.add(new BoolSetting.Builder()
        .name("use-names-list")
        .description("Only notify for specific players when they join.")
        .defaultValue(false)
        .visible(alarmOnJoin::get)
        .build()
    );
    private final Setting<List<String>> joinNames = sgJoin.add(new StringListSetting.Builder()
        .name("names")
        .description("Player names to watch for on join.")
        .defaultValue(List.of("ssy_", "e_2", "山水圆"))
        .visible(() -> alarmOnJoin.get() && useJoinList.get())
        .build()
    );
    private final Setting<Boolean> joinChatMessage = sgJoin.add(new BoolSetting.Builder()
        .name("chat-message")
        .description("Show a message in chat when a player joins.")
        .defaultValue(true)
        .visible(alarmOnJoin::get)
        .build()
    );
    private final Setting<String> joinChatText = sgJoin.add(new StringSetting.Builder()
        .name("chat-text")
        .description("Chat message when a player joins. Use {name} and {gamemode} as placeholders.")
        .defaultValue("[+] {name}")
        .visible(() -> alarmOnJoin.get() && joinChatMessage.get())
        .build()
    );

    // ==================== Leave Settings ====================
    private final Setting<Boolean> useLeaveList = sgLeave.add(new BoolSetting.Builder()
        .name("use-names-list")
        .description("Only notify for specific players when they leave.")
        .defaultValue(false)
        .visible(alarmOnLeave::get)
        .build()
    );
    private final Setting<List<String>> leaveNames = sgLeave.add(new StringListSetting.Builder()
        .name("names")
        .description("Player names to watch for on leave.")
        .defaultValue(List.of("ssy_", "e_2", "山水圆"))
        .visible(() -> alarmOnLeave.get() && useLeaveList.get())
        .build()
    );
    private final Setting<Boolean> leaveChatMessage = sgLeave.add(new BoolSetting.Builder()
        .name("chat-message")
        .description("Show a message in chat when a player leaves.")
        .defaultValue(true)
        .visible(alarmOnLeave::get)
        .build()
    );
    private final Setting<String> leaveChatText = sgLeave.add(new StringSetting.Builder()
        .name("chat-text")
        .description("Chat message when a player leaves. Use {name} and {gamemode} as placeholders.")
        .defaultValue("[-] {name}")
        .visible(() -> alarmOnLeave.get() && leaveChatMessage.get())
        .build()
    );

    // ==================== Enter RD Settings ====================
    private final Setting<Boolean> useEnterRDList = sgEnterRD.add(new BoolSetting.Builder()
        .name("use-names-list")
        .description("Only notify for specific players when they enter render distance.")
        .defaultValue(false)
        .visible(alarmOnEnterRD::get)
        .build()
    );
    private final Setting<List<String>> enterRDNames = sgEnterRD.add(new StringListSetting.Builder()
        .name("names")
        .description("Player names to watch for on entering render distance.")
        .defaultValue(List.of("ssy_", "e_2", "山水圆"))
        .visible(() -> alarmOnEnterRD.get() && useEnterRDList.get())
        .build()
    );
    private final Setting<Boolean> enterRDChatMessage = sgEnterRD.add(new BoolSetting.Builder()
        .name("chat-message")
        .description("Show a message in chat when a player enters render distance.")
        .defaultValue(true)
        .visible(alarmOnEnterRD::get)
        .build()
    );
    private final Setting<String> enterRDChatText = sgEnterRD.add(new StringSetting.Builder()
        .name("chat-text")
        .description("Chat message when a player enters render distance. Use {name} and {gamemode} as placeholders.")
        .defaultValue("[+] {name} entered render distance")
        .visible(() -> alarmOnEnterRD.get() && enterRDChatMessage.get())
        .build()
    );

    // ==================== Leave RD Settings ====================
    private final Setting<Boolean> useLeaveRDList = sgLeaveRD.add(new BoolSetting.Builder()
        .name("use-names-list")
        .description("Only notify for specific players when they leave render distance.")
        .defaultValue(false)
        .visible(alarmOnLeaveRD::get)
        .build()
    );
    private final Setting<List<String>> leaveRDNames = sgLeaveRD.add(new StringListSetting.Builder()
        .name("names")
        .description("Player names to watch for on leaving render distance.")
        .defaultValue(List.of("ssy_", "e_2", "山水圆"))
        .visible(() -> alarmOnLeaveRD.get() && useLeaveRDList.get())
        .build()
    );
    private final Setting<Boolean> leaveRDChatMessage = sgLeaveRD.add(new BoolSetting.Builder()
        .name("chat-message")
        .description("Show a message in chat when a player leaves render distance.")
        .defaultValue(true)
        .visible(alarmOnLeaveRD::get)
        .build()
    );
    private final Setting<String> leaveRDChatText = sgLeaveRD.add(new StringSetting.Builder()
        .name("chat-text")
        .description("Chat message when a player leaves render distance. Use {name} and {gamemode} as placeholders.")
        .defaultValue("[-] {name} left render distance")
        .visible(() -> alarmOnLeaveRD.get() && leaveRDChatMessage.get())
        .build()
    );

    // ==================== Gamemode Settings ====================
    private final Setting<Boolean> useGamemodeList = sgGamemode.add(new BoolSetting.Builder()
        .name("use-names-list")
        .description("Only notify for specific players when they change gamemode.")
        .defaultValue(false)
        .visible(alarmOnGamemodeChange::get)
        .build()
    );
    private final Setting<List<String>> gamemodeNames = sgGamemode.add(new StringListSetting.Builder()
        .name("names")
        .description("Player names to watch for on gamemode change.")
        .defaultValue(List.of("ssy_", "e_2", "山水圆"))
        .visible(() -> alarmOnGamemodeChange.get() && useGamemodeList.get())
        .build()
    );
    private final Setting<Boolean> gamemodeChatMessage = sgGamemode.add(new BoolSetting.Builder()
        .name("chat-message")
        .description("Show a message in chat when a player changes gamemode.")
        .defaultValue(true)
        .visible(alarmOnGamemodeChange::get)
        .build()
    );
    private final Setting<String> gamemodeChatText = sgGamemode.add(new StringSetting.Builder()
        .name("chat-text")
        .description("Chat message when a player changes gamemode. Use {name}, {old_gamemode} and {new_gamemode} as placeholders.")
        .defaultValue("{name} changed gamemode: {old_gamemode} -> {new_gamemode}")
        .visible(() -> alarmOnGamemodeChange.get() && gamemodeChatMessage.get())
        .build()
    );

    // ==================== State ====================
    private final Map<UUID, String> nameCache = new HashMap<>();      // UUID -> 玩家名（离开时数据包不含名字，反查用）
    private final Map<UUID, GameType> gamemodeCache = new HashMap<>(); // 游戏模式缓存
    private final Set<UUID> playersInRender = new HashSet<>();        // 当前在渲染距离内的玩家

    public PlayerAlarms() {
        super(XinToolsAddon.CATEGORY, "PlayerAlarms",
            "Notifies you of player events: join/leave server, enter/leave render distance, gamemode changes.");
    }

    @Override
    public void onActivate() {
        nameCache.clear();
        gamemodeCache.clear();
        playersInRender.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.level == null || mc.player == null) return;

        // 渲染距离检测
        if (alarmOnEnterRD.get() || alarmOnLeaveRD.get()) {
            Set<UUID> currentInRender = new HashSet<>();
            for (var entity : mc.level.entitiesForRendering()) {
                if (entity instanceof Player && entity != mc.player) {
                    currentInRender.add(entity.getUUID());
                }
            }

            if (alarmOnEnterRD.get()) {
                for (UUID id : currentInRender) {
                    if (!playersInRender.contains(id)) {
                        String name = getPlayerName(id);
                        if (name != null && shouldAlarm(useEnterRDList.get(), enterRDNames.get(), name)) {
                            sendChat(enterRDChatMessage.get(), enterRDChatText.get(), name, id, ChatFormatting.DARK_RED);
                        }
                    }
                }
            }

            if (alarmOnLeaveRD.get()) {
                for (UUID id : playersInRender) {
                    if (!currentInRender.contains(id)) {
                        String name = getPlayerName(id);
                        if (name != null && shouldAlarm(useLeaveRDList.get(), leaveRDNames.get(), name)) {
                            sendChat(leaveRDChatMessage.get(), leaveRDChatText.get(), name, id, ChatFormatting.DARK_GREEN);
                        }
                    }
                }
            }

            playersInRender.clear();
            playersInRender.addAll(currentInRender);
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (mc.level == null || mc.player == null) return;

        // --- Player Join ---
        if (alarmOnJoin.get() && event.packet instanceof ClientboundPlayerInfoUpdatePacket packet) {
            if (packet.actions().contains(Action.ADD_PLAYER)) {
                for (Entry entry : packet.entries()) {
                    UUID id = entry.profileId();
                    String name = entry.profile().name();
                    nameCache.put(id, name);
                    gamemodeCache.put(id, entry.gameMode());
                    if (shouldAlarm(useJoinList.get(), joinNames.get(), name)) {
                        sendChat(joinChatMessage.get(), joinChatText.get(), name, id, ChatFormatting.GREEN);
                    }
                }
            }
        }

        // --- Player Leave ---
        if (alarmOnLeave.get() && event.packet instanceof ClientboundPlayerInfoRemovePacket packet) {
            for (UUID id : packet.profileIds()) {
                String name = nameCache.get(id);
                if (name == null) name = getPlayerName(id);
                if (name == null) name = id.toString();
                if (shouldAlarm(useLeaveList.get(), leaveNames.get(), name)) {
                    sendChat(leaveChatMessage.get(), leaveChatText.get(), name, id, ChatFormatting.RED);
                }
                nameCache.remove(id);
                gamemodeCache.remove(id);
                playersInRender.remove(id);
            }
        }

        // --- Gamemode Change ---
        if (alarmOnGamemodeChange.get() && event.packet instanceof ClientboundPlayerInfoUpdatePacket packet) {
            if (packet.actions().contains(Action.UPDATE_GAME_MODE)) {
                for (Entry entry : packet.entries()) {
                    UUID id = entry.profileId();
                    GameType newMode = entry.gameMode();

                    // 从客户端内置列表获取旧模式
                    var playerInfo = mc.player.connection.getPlayerInfo(id);
                    if (playerInfo == null) continue; // 刚出现的玩家，忽略

                    GameType oldMode = playerInfo.getGameMode();

                    if (oldMode != newMode) {
                        String name = getPlayerName(id);
                        if (name != null && shouldAlarm(useGamemodeList.get(), gamemodeNames.get(), name)) {
                            if (gamemodeChatMessage.get()) {
                                String msg = gamemodeChatText.get()
                                    .replace("{name}", name)
                                    .replace("{old_gamemode}", oldMode.getName())
                                    .replace("{new_gamemode}", newMode.getName());
                                ChatUtils.sendMsg(Component.literal(msg).withStyle(ChatFormatting.YELLOW));
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== Helpers ====================

    private boolean shouldAlarm(boolean useList, List<String> names, String playerName) {
        if (!useList) return true;
        return names.stream().anyMatch(n -> n.equalsIgnoreCase(playerName));
    }

    private void sendChat(boolean enabled, String template, String name, UUID playerId, ChatFormatting color) {
        if (!enabled) return;
        String msg = template.replace("{name}", name);
        if (showGamemodeInChat.get() && playerId != null) {
            msg = msg.replace("{gamemode}", getGamemodeName(playerId));
        } else {
            // 模板中有 {gamemode} 但不显示，移除占位符避免残留
            msg = msg.replace("{gamemode}", "");
        }
        ChatUtils.sendMsg(Component.literal(msg).withStyle(color));
    }

    private String getPlayerName(UUID id) {
        String cached = nameCache.get(id);
        if (cached != null) return cached;
        if (mc.level != null) {
            Player player = mc.level.getPlayerByUUID(id);
            if (player != null) return player.getGameProfile().name();
        }
        if (mc.player != null && mc.player.connection != null) {
            var entry = mc.player.connection.getPlayerInfo(id);
            if (entry != null && entry.getProfile() != null) return entry.getProfile().name();
        }
        return null;
    }

    private String getGamemodeName(UUID id) {
        GameType mode = gamemodeCache.get(id);
        if (mode != null) return mode.getName();
        // 缓存中没有则尝试从网络信息获取
        if (mc.player != null && mc.player.connection != null) {
            var info = mc.player.connection.getPlayerInfo(id);
            if (info != null) {
                mode = info.getGameMode();
                if (mode != null) return mode.getName();
            }
        }
        return "Unknown";
    }
}
