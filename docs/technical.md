# XinToolsAddon 技术方案文档

> 记录项目的技术选型、架构说明与具体功能的技术实现方案。开发前必读。

## 1. 技术栈

| 项目 | 版本/说明 |
|---|---|
| Minecraft | 1.21.11 |
| 加载器 | Fabric |
| 客户端 | Meteor Client（addon 形式） |
| Java | 21 |
| 构建系统 | Gradle + Fabric Loom |
| 映射 | Mojang mappings（代码中可见 `net.minecraft.*` 命名） |

## 2. 项目结构

```
src/main/java/alsolate/xintools/addon/
├── XinToolsAddon.java          # 主类（MeteorAddon），注册所有模块
├── modules/                    # 功能模块（继承 Meteor Module / WaveXinModule）
│   ├── Automend.java           # 按住投掷经验瓶修装备
│   ├── PacketEat.java          # 发包进食
│   ├── PlayerAlarms.java       # 玩家聊天通知（仅消息，不响铃）
│   ├── XinQueue.java           # 排队自动答题
│   ├── betterelytrafly/        # 鞘翅飞行（WaveXin 迁入）
│   ├── sniffernametags/        # 嗅探兽标签（WaveXin 迁入）
│   ├── autologin/              # 自动登录（WaveXin 迁入）
│   └── basefinder/             # 基地扫图（WaveXin 迁入）
├── core/                       # WaveXin 迁入支撑：WaveXinModule、Timer、数据路径等
├── events/                     # WaveXin 迁入：Event、MoveEvent、TravelEvent
├── i18n/                       # WaveXin 迁入：WaveXinI18n（中英双语）
├── gui/                        # WaveXin 迁入：WaveXinEnumDropdown
└── mixin/                      # Mixin 注入
    ├── MixinPlayerEntity.java          # travel 事件（鞘翅飞行）
    ├── MixinClientPlayerEntity.java    # move 事件（鞘翅飞行）
    └── MixinClientPlayNetworkHandler.java  # 标题/聊天文本事件（自动登录）
src/main/resources/
├── fabric.mod.json             # Fabric mod 元数据
├── xin-tools-addon.mixins.json # Mixin 配置
├── questions.json              # XinQueue + AutoLogin 共用题库
└── assets/xintools/            # 图标、lang（en_us/zh_cn 双语）等资源
```

## 3. 模块开发模式（Meteor Client）

所有功能模块继承 `meteordevelopment.meteorclient.systems.modules.Module`：

```java
public class XxxModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<...> xxx = sgGeneral.add(new XxxSetting.Builder()
        .name("...")
        .description("...")
        .defaultValue(...)
        .build());

    public XxxModule() {
        super(XinToolsAddon.CATEGORY, "模块名", "描述");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) { ... }
}
```

## 4. Automend 技术方案（需求 1）

### 4.1 按键绑定实现（最终版，2026-08-07）

**不使用设置中的 `KeybindSetting`**（曾用 Trigger Key 命名，后因与模块 KeyBind 语义重复且造成"绑定无效"困惑而移除）。直接使用**模块自带 KeyBind**（`Module.keybind` 字段，用户在模块设置界面顶部 Bind 区块绑定），配合 Meteor 标准 Hold 模式：

```java
public Automend() {
    super(XinToolsAddon.CATEGORY, "Automend", "按住模块绑定按键持续向下投掷经验瓶修复装备，松开按键即停止。");
    // 按住绑定键开启模块、松开自动关闭（Meteor 标准 Hold 模式）
    toggleOnBindRelease = true;
}

@Override
public void onActivate() {
    // 强制保持 Hold 模式：防止用户配置文件中保存的 toggleOnBindRelease=false 覆盖默认值
    toggleOnBindRelease = true;

    // 开启时检测：必须先绑定按键（模块设置中 Bind 区块），未绑定则提示
    if (!keybind.isSet()) {
        ChatUtils.sendMsg(Component.literal("...未绑定按键...").withStyle(ChatFormatting.RED));
    }
}
```

`Module.keybind`（`meteordevelopment.meteorclient.utils.misc.Keybind`，`public final`）是 Meteor 每个模块都有的标准启停绑定，由用户通过模块设置界面 Bind 区块配置，无需在 addon 中额外实现绑定 UI。

> 重要教训（2026-08-07）：设置列表中的 `KeybindSetting`（曾命名 Trigger Key）与模块 Bind 区块的 KeyBind **不是同一个对象**。用户绑定 Bind 区块后，`KeybindSetting.isSet()` 仍为 false，导致"已绑定却提示未绑定"。最终移除设置中的 KeybindSetting，统一使用模块自带 `Module.keybind` + `toggleOnBindRelease = true` 实现按住开/松开关。

### 4.2 每 tick 投掷逻辑

```java
FindItemResult exp = inventorySwitch.get()
    ? InvUtils.find(stack -> stack.getItem() == Items.EXPERIENCE_BOTTLE, 0, 35)
    : InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);

if (!exp.found()) {
    // 红色提示一次并暂停本次投掷（松开按键后或补充经验瓶后可重新触发）
    return;
}
throwStraightDown(exp);  // 检测通过，原地向下投掷
```

**原地向下投掷、不转动视角**（关键实现，使用 Minecraft 网络包）：

```java
// 1. 发送服务端旋转包：服务端认为玩家 pitch=90（客户端视角不变）
mc.player.connection.getConnection().send(new ServerboundMovePlayerPacket.Rot(
    mc.player.getYRot(), 90.0f, mc.player.onGround(), false));
// 2. 临时修改客户端朝向使本地预测一致，同一帧内恢复（画面不闪）
float oldXRot = mc.player.getXRot();
mc.player.setXRot(90.0f);
mc.gameMode.useItem(mc.player, hand);
mc.player.setXRot(oldXRot);
```

要点：
- 不用 `Rotations.rotate`（它会转动玩家视角，画面闪动）。
- 服务端旋转包用 `ServerboundMovePlayerPacket.Rot(float yRot, float xRot, boolean onGround, boolean changePosition)`（1.21.11 Mojang 映射，已用 javap 核实）。
- 投掷动作保留原逻辑：手持槽优先（`exp.getHand()`），否则 `inventorySwitch` 控制背包移动/快捷栏交换。

### 4.3 保留的现有逻辑（不修改）

- 经验瓶查找（背包/快捷栏切换 `inventorySwitch` 设置，描述已中文化）。
- 手持槽优先投掷。
- 无经验瓶时的中文红色提示。

## 5. PlayerAlarms 技术方案（需求 2，2026-08-08）

### 5.1 模块定位

由 `BetterPlayerAlarms` 与 xinplus 的 `PlayerNotifier` 合并而来，完整重命名为 `PlayerAlarms`。核心变化：**仅聊天彩色消息通知，删除全部响铃逻辑**。

### 5.2 消息格式（默认模板）

| 事件 | 默认模板 | 颜色 |
|---|---|---|
| 玩家加入 | `[+] {name}` | `ChatFormatting.GREEN` |
| 玩家离开 | `[-] {name}` | `ChatFormatting.RED` |
| 进入渲染距离 | `[+] {name} entered render distance` | `DARK_RED` |
| 离开渲染距离 | `[-] {name} left render distance` | `DARK_GREEN` |
| 游戏模式变更 | `{name} changed gamemode: {old_gamemode} -> {new_gamemode}` | `YELLOW` |

### 5.3 关键实现

- **UUID 玩家名缓存（PlayerNotifier 反查机制）**：`nameCache: Map<UUID, String>`。`ClientboundPlayerInfoRemovePacket`（玩家离开）只含 UUID 不含名字，因此在加入时（`ClientboundPlayerInfoUpdatePacket` ADD_PLAYER）缓存 `UUID -> name`，离开时反查；查不到再回退 `getPlayerName`（level 实体 → connection 网络信息）。
- **事件处理**：
  - 加入/离开/游戏模式变更：`PacketEvent.Receive` 监听 `ClientboundPlayerInfoUpdatePacket` / `ClientboundPlayerInfoRemovePacket`。
  - 进出渲染距离：`TickEvent.Pre` 中比对 `mc.level.entitiesForRendering()` 前后集合（`playersInRender`）。
  - 游戏模式变更：用 `mc.player.connection.getPlayerInfo(id).getGameMode()` 取旧模式与包内新模式比对。
- **设置结构**：General 6 项（5 个事件开关 + show-gamemode-in-chat）+ 5 个事件组各 4 项（use-names-list / names / chat-message / chat-text），共 26 项。
- **删除内容**：响铃设置（rings / ring-delay / volume / pitch / sound）、RingState 响铃调度、上线初始扫描（initialJoinCheckDone / alarmedJoinPlayers）、未用字段 playersSpottedRD。
- **占位符**：`{name}`、`{gamemode}`（受 show-gamemode-in-chat 控制）、`{old_gamemode}`、`{new_gamemode}`。
- **描述语言**：英文（Meteor 环境不允许中文）。

## 6. 构建与验证命令

```bash
./gradlew build        # 编译并打包 mod jar
./gradlew runClient    # 启动客户端测试
```

构建产物：`build/libs/XinToolsAddon-<version>.jar`

## 7. 注意事项

- 本项目使用 Mojang mappings，代码中为 `net.minecraft.*` 命名，勿使用 Yarn 命名。
- 修改 mixin 后必须同步检查 `xin-tools-addon.mixins.json` 引用。
- 新增模块后必须在 `XinToolsAddon.onInitialize()` 中注册。
- 包名统一为 `alsolate.xintools.addon`，消息前缀统一 `[XinToolsAddon]`。
- 代码中注释沿用现有风格（中文注释说明逻辑，英文用于设置名/描述）。
- 注意：用 PowerShell 写入 Java 文件时默认带 UTF-8 BOM，需使用无 BOM 编码，否则编译报"非法字符 \ufeff"。
