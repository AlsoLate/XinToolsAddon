# XinToolsAddon 开发需求文档

> 本文档用于记录项目所有已确认的功能需求。新增需求必须先经沟通确认后写入本文档，再进入开发。

## 项目概述

- 项目：XinToolsAddon（Meteor Client addon）
- 平台：Fabric 1.21.11
- 用途：为 2b2t.xin 等服务器提供辅助功能
- 需求状态标记：`✅ 已确认` / `🔄 开发中` / `⏳ 待开发` / `❌ 已取消`

---

## 需求 1：Automend 模块改造（原名 ADAutomend）

### 状态

`✅ 已完成`（2026-08-07 完成开发并构建验证通过；2026-08-07 晚按用户新需求改为"按住投掷键持续扔、松开停止"，v6 修复绑定失效）

### 背景

现有 `ADAutomend` 模块的触发条件写死在代码中：必须同时按住 A 键（keyLeft）与 D 键（keyRight）才投掷经验瓶修复装备，且按住 W/S 时无效。

### 需求目标

1. 将模块从 `ADAutomend` 完整重命名为 `Automend`（文件、类名、模块显示名全部变更）。
2. 触发方式改为**按住绑定按键持续投掷、松开停止**，不再写死 A+D。

### 已确认的需求细节（2026-08-07 与用户确认，含二次澄清与 v6 最终方案）

| 项目 | 结论 |
|---|---|
| 触发方式 | **按住模块 Bind 区块的绑定键持续扔、松开即停（v6）**：Meteor 标准 Hold 模式，`Module.keybind` + `toggleOnBindRelease = true`（按住键开启模块、松开自动关闭） |
| 按键绑定位置 | **模块设置界面顶部的 Bind 区块**（模块自带 KeyBind）。注意：设置列表中新增按键设置（KeybindSetting）与 Bind 区块不是同一对象，v5 曾因此失效 |
| 移除 A+D 触发 | **不使用 A+D 触发** |
| 开启时检查 | 开启模块先检测是否绑定 KeyBind：未绑定 → 聊天栏中文红色提示"未绑定按键"（不强制关闭） |
| 经验瓶检查 | 投掷前检测背包是否有经验瓶：没有 → 中文提示一次并暂停本次投掷（松开按键后或补充经验瓶后可重新触发） |
| 投掷方向 | **原地向下**，**不转动玩家视角**（服务端同步 pitch=90，客户端画面不闪） |
| 重命名范围 | **完整重命名**：文件名、类名、模块显示名全部改为 Automend |
| 其余行为 | 保留原有投掷逻辑：背包/快捷栏切换（inventorySwitch 设置，描述已中文化）、手持槽优先 |
| 描述语言 | 模块描述、设置描述、聊天提示全部中文化 |

> 变更记录：2026-08-07 晚修复"绑定按键后仍提示未绑定"问题（v5 使用 KeybindSetting 与 Bind 区块 KeyBind 不互通）→ v6 改用模块自带 `keybind` + `toggleOnBindRelease = true` 实现按住开/松开关，并强制在 onActivate 中恢复该字段防止配置文件覆盖。

### 验收标准

1. 模块在游戏内显示名为 `Automend`，位于 XinTools 分类。
2. 在模块 Bind 区块绑定按键后，**按住该键持续向下投掷经验瓶；松开立即停止（模块自动关闭）**。
3. 背包无经验瓶时中文提示一次并暂停，松开按键后可重新触发。
4. 未绑定按键开启模块 → 聊天栏中文红色提示（不强制关闭）。
5. 模块描述、设置描述、聊天提示均为中文。
6. 项目 `./gradlew build` 编译通过，无残留 `ADAutomend` 引用。

### 涉及文件

- `src/main/java/alsolate/xintools/addon/modules/Automend.java`
- `src/main/java/alsolate/xintools/addon/XinToolsAddon.java`（模块注册处）

---

## 需求 2：PlayerAlarms 模块（BetterPlayerAlarms 合并 PlayerNotifier）

### 状态

`✅ 已完成`（2026-08-08 与用户多轮沟通确认细节，确认后开发）

### 背景

XinToolsAddon 中的 `BetterPlayerAlarms` 与 xinplus 子项目中的 `PlayerNotifier` 功能重叠（均监控玩家上下线）。用户希望将两者合并为单一模块，采用 PlayerNotifier 的简洁通知风格（仅聊天消息、不响铃），并完整重命名为 `PlayerAlarms`。

### 需求目标

1. 将模块完整重命名为 `PlayerAlarms`（文件、类名、模块显示名全部变更）。
2. 逻辑吸收 PlayerNotifier 的轻量风格：**仅聊天彩色消息通知，不响铃**。
3. 保留全部事件：玩家加入/离开服务器、进入/离开渲染距离、游戏模式变更。
4. 移除响铃相关设置（约 15 项）。
5. xinplus 子项目完全不动（PlayerNotifier 保留）。

### 已确认的需求细节（2026-08-08 与用户确认）

| 项目 | 结论 |
|---|---|
| 模块最终名 | **PlayerAlarms**（文件、类名、模块显示名完整重命名） |
| 保留事件 | 全部：加入/离开服务器、进入/离开渲染距离、游戏模式变更 |
| 通知方式 | **仅聊天彩色消息，不响铃**（PlayerNotifier 风格） |
| 消息格式 | **`[+]` / `[-]` 风格**（绿色加号加入、红色减号离开） |
| 保留设置 | 5 个事件开关 + 名单过滤（use-names-list/names）+ 聊天文本（chat-message/chat-text） |
| 删除设置 | 全部响铃相关设置（rings / ring-delay / volume / pitch / sound，约 15 项） |
| 上线扫描 | **不保留**（开模块时不通知已在线玩家） |
| 描述语言 | 英文（Meteor 环境不允许中文） |
| xinplus | **完全不动** |

### 各事件默认消息模板（可通过 chat-text 设置自定义）

| 事件 | 默认模板 | 颜色 |
|---|---|---|
| 玩家加入 | `[+] {name}` | 绿色 |
| 玩家离开 | `[-] {name}` | 红色 |
| 进入渲染距离 | `[+] {name} entered render distance` | 深红 |
| 离开渲染距离 | `[-] {name} left render distance` | 深绿 |
| 游戏模式变更 | `{name} changed gamemode: {old_gamemode} -> {new_gamemode}` | 黄色 |

### 验收标准

1. 游戏内显示名为 `PlayerAlarms`，位于 XinTools 分类。
2. 加入/离开/进出渲染距离/游戏模式变更均只发彩色聊天消息，不响铃。
3. 无任何响铃设置残留；默认消息为 `[+]` / `[-]` 风格。
4. 项目中无 `BetterPlayerAlarms` 残留引用。
5. `./gradlew build` 编译通过。
6. xinplus 子项目未做任何改动。

### 涉及文件

- `src/main/java/alsolate/xintools/addon/modules/PlayerAlarms.java`
- `src/main/java/alsolate/xintools/addon/XinToolsAddon.java`（模块注册处）

---

## 需求 3：项目全量重命名为 XinToolsAddon

### 状态

`✅ 已完成`（2026-08-08 与用户多轮沟通确认细节，确认后开发）

### 背景

旧项目名 `XingduAddon` 中英杂糀，不够简短直接。用户希望全量重命名以更贴目标服务器 2b2t.xin 的工具集定位。

### 需求目标

1. **项目名**：`XingduAddon` → **`XinToolsAddon`**（GitHub 仓库 `AlsoLate/XinToolsAddon`）
2. **包名**：`alsolate.xingdu.addon` → `alsolate.xintools.addon`
3. **主类**：`XingduAddon` → `XinToolsAddon`（文件/类名/Category 文本同步：`Xingdu` → `XinTools`）
4. **资源目录**：`assets/xingdu/` → `assets/xintools/`
5. **全量重命名**：包名、主类、资源目录、Gradle 配置、fabric.mod.json、mixins.json、lang 路径、HTML 说明书、开发日志等全部同步
6. **GitHub 仓库改名**：通过 `gh repo rename` 命令手动执行

### 已确认的需求细节（2026-08-08 与用户确认）

| 项目 | 结论 |
|---|---|
| 新项目名 | **XinToolsAddon** |
| GitHub 仓库 | `AlsoLate/XinToolsAddon`（手动执行 `gh repo rename`） |
| 新包名 | `alsolate.xintools.addon` |
| 新主类 | `XinToolsAddon`（Category 文本：`XinTools`） |
| 资源目录 | `assets/xintools/` |
| Gradle | `archives_base_name=XinToolsAddon`、`maven_group=alsolate.xintools`、`rootProject.name=XinToolsAddon` |
| 范围 | **全量重命名**（项目名/包名/主类/资源目录/资产/lang/HTML 说明书/开发日志） |
| xinplus | **完全不动** |

### 验收标准

1. 根目录 `settings.gradle.kts` 中 `rootProject.name = "XinToolsAddon"`。
2. `gradle.properties` 中 `archives_base_name=XinToolsAddon`、`maven_group=alsolate.xintools`。
3. `src/main/resources/fabric.mod.json` 中 `id=name=xin-tools-addon`、`name=XinToolsAddon`、entrypoint 指向 `alsolate.xintools.addon.XinToolsAddon`。
4. `src/main/resources/xin-tools-addon.mixins.json` 同步重命名。
5. `src/main/java/alsolate/xintools/addon/` 包路径下所有 Java 文件无 `xingdu` 残留。
6. `src/main/resources/assets/xintools/lang/en_us.json`、`zh_cn.json` 路径正确。
7. `./gradlew build` 编译通过，产物 jar 名为 `XinToolsAddon-<version>.jar`。
8. 文档（README/CLAUDE/technical/roadmap/HTML 说明书/开发日志）全部同步。
9. `gh repo rename XingduAddon XinToolsAddon` 命令在用户执行后，仓库 URL 变为 `https://github.com/AlsoLate/XinToolsAddon`（保留为变更记录，不重复执行）。

### 涉及文件

- 全部 `src/main/java/alsolate/xintools/addon/` Java 文件
- `src/main/resources/fabric.mod.json`
- `src/main/resources/xingdu-addon.mixins.json` → `xin-tools-addon.mixins.json`
- `src/main/resources/assets/xingdu/` → `src/main/resources/assets/xintools/`
- `gradle.properties`、`settings.gradle.kts`、`build.gradle.kts`
- `README.md`、`CLAUDE.md`、`docs/` 全部文档
- `xintools-addon-guide/` 整个 HTML 说明书目录
- `dev-log/` 日志（追加新日志，历史日志保留原名作为变更记录）

---

## 需求模板（新增需求时复制此结构）

```markdown
## 需求 N：需求名称

### 状态
⏳ 待开发

### 背景
（为什么需要这个功能）

### 需求目标
（希望实现什么）

### 已确认的需求细节
（表格：触发方式、交互方式、限制等）

### 验收标准
（可验证的完成标准）
```
