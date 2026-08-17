# XinToolsAddon — 项目现状与 AI 对接说明

> 供后续 AI / 开发者快速接手。信息以仓库代码为准；有冲突时以源码与 `zh_cn.json` 为准。

**文档日期：** 2026-08-17  
**MC / 生态：** Fabric · Minecraft **1.21.11** · Meteor Client 插件  
**Java 包：** `alsolate.xintools.addon`  
**Gradle 分类名：** `XinTools`

---

## 1. 项目是什么

XinToolsAddon 是给 **Meteor Client** 用的 Fabric 附加模组，集合：

- 2b2t.xin 相关自动化（登录、排队答题）
- 扫图找基（BaseFinder）
- HUD/提示类（统计信息、提示信息）
- 战斗/移动辅助（FastXP、PacketEat、ElytaFly+ 等）
- **可开关的全局汉化（i18n）**，移植并改造自 Sinicization-Plugin 思路

**安全：** 曾对参考用的 Sinicization-Plugin.jar 做静态审计（反编译 + 危险 API 扫描）；未发现后门类逻辑。原插件中的无用外链（如原神）未迁入本项目。

---

## 2. 模块清单（内部名 ↔ 中文名）

与用户沟通时请使用 **中文名**；改 i18n 时必须用 **keySegment(内部名)** 对 `zh_cn.json`。

| 内部 name | keySegment | 中文 title | 源码位置 |
|-----------|------------|------------|----------|
| i18n | i18n | 汉化开关 | `modules/I18nModule.java` |
| FastXP | fast_xp | 快速经验瓶 | `modules/FastXP.java` |
| AutoLogin | auto_login | 自动登录 | `modules/autologin/AutoLogin.java` |
| AutoQueue | auto_queue | 自动排队答题 | `modules/XinQueue.java`（类名 XinQueue） |
| BaseFinder | base_finder | 基地狩猎扫图 | `modules/basefinder/BaseFinder.java` |
| IMGTips | **imgtips** | 提示信息 | `modules/IMGTips.java` |
| IMGWorldStats | **imgworld_stats** | 统计信息 | `modules/IMGWorldStats.java` |
| ChatHighLight | chat_high_light | 聊天高亮 | `modules/ChatHighlight.java` |
| CustomFov | custom_fov | 自定义视角 | `modules/CustomFov.java` |
| PortalESP | portal_esp | 传送门高亮 | `modules/PortalESP.java` |
| Prefix | prefix | 聊天前缀 | `modules/Prefix.java` |
| PacketEat | packet_eat | 发包进食 | `modules/PacketEat.java` |
| PlayerAlarms | player_alarms | 玩家警报 | `modules/PlayerAlarms.java` |
| ElytaFly+ | elyta_fly | 鞘翅飞行+ | `modules/elytrafly/ElytraFlyPlus.java` |
| SnifferNameTags | sniffer_name_tags | 嗅探兽名称标签 | `modules/sniffernametags/SnifferNametags.java` |

**易错 key（已踩坑）：**

- `IMGTips` → `imgtips`，**不是** `img_tips`
- `IMGWorldStats` → `imgworld_stats`，**不是** `img_world_stats`

生成规则见 `WaveXinI18n.keySegment`：先 `([a-z0-9])([A-Z])` 插下划线，再小写，再非字母数字变 `_`。连续大写前缀不会被拆开。

---

## 3. 汉化架构（必读）

### 3.1 总开关

- 模块 **汉化开关（i18n）** → `I18nModule`
- `I18nModule.isActiveStatic()` 被 mixin / WaveXinI18n / 文字逻辑查询

### 3.2 两条翻译管线

| 管线 | 类 | 覆盖范围 | 数据文件 |
|------|-----|----------|----------|
| XinTools 自有 | `WaveXinI18n` | 本插件模块标题/描述/设置/分组/提示文案 | `assets/xintools/lang/zh_cn.json`、`en_us.json` |
| Meteor 生态 | `MeteorTranslator` | 其他 Meteor 模块与 Setting、Config/HUD/Macros/Profiles/PathManager | `meteor_zh_cn.json`、`meteor_en_us.json` |

**WaveXinI18n** 在 i18n 开启时从 **自己的 classpath JSON** 读中文（`ensureOwnLangLoaded`），**不依赖** Minecraft 是否选中文语言。这是「描述变成英文」问题的修复点。

**MeteorTranslator**：

- 首次见到的 `Module`/`Setting`/`SettingGroup` 原文快照进 IdentityHashMap，**永不被译后字符串覆盖**
- `applyToAllModules(true/false)`：全体模块 + Config + Hud + Macros + Profiles + PathManagers 设置
- 写回字段用 **VarHandle**（`title`/`description`/`SettingGroup.name`）
- 分组 key 必须用 **英文原名** 算 `keySegment`，避免分组已译成中文后 key 失效
- 切换后 `refreshOpenGui()`：对 `TabScreen` **双延迟 reopen**；`ModuleScreen` 尽量按模块重开

### 3.3 Mixin 一览

| Mixin | 作用 |
|-------|------|
| MeteorModuleMixin | 构造时快照；i18n 开则译模块 title/description |
| MeteorSettingMixin | 同上，针对 Setting（含 XinTools） |
| TextRendererMixin | **始终**强制 VanillaTextRenderer |
| VanillaTextRendererMixin | **始终** scaleIndividually |
| ModuleScreenCursorMixin | 模块设置 Esc 后恢复鼠标坐标 |
| AccessorMinecraftClient | `rightClickDelay`（FastXP） |
| 其他 | 网络/叠加层/ChatUtils 等业务 |

注册表：`src/main/resources/xin-tools-addon.mixins.json`。

### 3.4 翻译覆盖审计（2026-08-17）

脚本按「模块 super 名 + SettingGroup 变量 + `.name()`」核对：

- 全部模块 **title/description**：有中文 key  
- 全部分组：有 `group.wavexin.*` 或 `SettingGroup.Meteor.*`  
- 全部设置 title：有对应 `setting.wavexin.{mod}.{group}.{setting}.title` 或 `Setting.Meteor.{name}`  

**Meteor 原版/第三方** 依赖 `meteor_zh_cn.json`（条目很多）；个别冷门模组名可能仍缺译，属词条补全而非架构问题。  
**枚举显示名**部分已写（如 FastXP RotationMode）；BaseFinder 等复杂枚举若 GUI 仍显示英文，可按 `enum.wavexin.*` 风格继续补。

---

## 4. 近期需求与已做变更

1. **自动登录**：删除 Auto Answer / questions.json / 收包答题 / 延迟提交；描述改为登录+签到；答题归 **自动排队答题**。  
2. **统计信息**：文字/背景 X/Y `sliderMax` **8000**。  
3. **设置与分组翻译**：General→基础设置，Control→控制，Bind→绑定等；设置项批量进 zh/en。  
4. **Esc 光标**：`ModuleScreenCursorMixin` 保存/恢复坐标。  
5. **FastXP**：默认 **Silent**；含 Auto 向下扔（原 Automend，不强制绑键）。  
6. **i18n GUI**：开关后自动重开 Tab，避免必须手关界面。  
7. **映射**：`rightClickDelay`，勿用 `itemUseTicks`。

---

## 5. 构建与交付

```text
Windows:  .\gradlew.bat clean
          .\gradlew.bat build --rerun-tasks
产物:     build/libs/*.jar → mods/
```

改完代码后按用户习惯应重新打包 zip（含 `src`、`gradle*`、`build.gradle.kts` 等）。

---

## 6. 给后续 AI 的硬性约定

1. **对用户说话用中文模块名**（提示信息、统计信息、自动登录…）；改 key 时用内部名算 `keySegment`，**先算再写 JSON**。  
2. **禁止**再引入原神链接、无关 about 页、未审计的远程执行逻辑。  
3. 改 i18n 相关：  
   - 开关可逆（原文快照 + VarHandle 写回）  
   - 文字渲染与开关解耦（始终 Vanilla）  
   - 分组/设置 key 基于 **英文原名**  
4. 增删模块设置后：更新 `zh_cn.json` + `en_us.json`，并跑一轮 key 审计（见第 3.4 思路）。  
5. 用户偏好：每次有效改动后提供可下载的完整工程包。  
6. AutoLogin **不要**再加回答题逻辑（与 AutoQueue 分工）。  

---

## 7. 关键文件索引

| 路径 | 说明 |
|------|------|
| `XinToolsAddon.java` | 模块注册顺序（i18n 宜靠前） |
| `i18n/MeteorTranslator.java` | 全局应用/还原/GUI 刷新 |
| `i18n/WaveXinI18n.java` | XinTools 文案与 keySegment |
| `modules/I18nModule.java` | 汉化总开关 |
| `modules/FastXP.java` | 经验瓶；Accessor rightClickDelay |
| `modules/autologin/AutoLogin.java` | 无答题版自动登录 |
| `assets/xintools/lang/zh_cn.json` | XinTools 中文（对接时优先查） |
| `assets/xintools/lang/meteor_zh_cn.json` | Meteor 生态中文 |
| `xin-tools-addon.mixins.json` | Mixin 列表 |

---

## 8. 未决 / 可改进（非阻塞）

- 部分 Meteor 第三方模块词条仍可能英文（补 `meteor_zh_cn.json`）。  
- BaseFinder / 鞘翅等 **枚举** 的中文展示可再系统化。  
- 源码里仍有少量设置 `.name()` 直接写中文（聊天高亮、传送门等）；i18n 关时也会显示中文，若要「关 i18n 全英文」需把 name 改回英文并只放在 json。  
- `ModuleScreenCursorMixin` 在不同 MC/GLFW 版本上对 `MouseHandler` 私有字段反射可能失败，此时至少 `glfwSetCursorPos` 仍会执行。  

---

*本文档与 README.md 应随架构变更同步更新。*
