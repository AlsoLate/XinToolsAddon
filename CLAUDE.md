# CLAUDE.md — AI 工作指引

本文件是 XinToolsAddon 项目的 AI 协作指引。开始任何工作前，先阅读本文档。

## 项目简介

- Fabric 1.21.11 的 Meteor Client addon（Mojang mappings，Java 21，Gradle + Fabric Loom）
- 主类：`src/main/java/alsolate/xintools/addon/XinToolsAddon.java`（注册所有模块）
- 模块目录：`src/main/java/alsolate/xintools/addon/modules/`
- Mixin 目录：`src/main/java/alsolate/xintools/addon/mixin/`
- 作者：AlsoLate
- 包名：`alsolate.xintools.addon`

## 保留模块（8个）

| 模块 | 功能 | 来源 |
|---|---|---|
| Automend | 按住绑定键持续向下投掷经验瓶修复装备 | 原有 |
| PacketEat | 通过发包实现无中断进食 | 原有 |
| PlayerAlarms | 玩家上线/下线/进入渲染距离/游戏模式变更聊天通知（仅消息，不响铃） | BetterPlayerAlarms 合并 PlayerNotifier |
| XinQueue | 2b2t.xin 排队自动答题 | 原有 |
| BetterElytraFly | 鞘翅飞行控制增强 + 耐久过低自动换鞘翅 | WaveXin 迁入 |
| SnifferNametags | 嗅探兽自定义名称标签显示 | WaveXin 迁入 |
| AutoLogin | 2b2t.xin 自动登录/答题/签到/加入流程 | WaveXin 迁入 |
| BaseFinder | 基地狩猎扫图（方环/螺旋扫描、容器记录、Xaero 路点） | WaveXin 迁入 |

## WaveXin 迁入支撑代码

- `core/`：WaveXinModule（模块基类）、WaveXinDataPaths、WaveXinSettingsStore、WaveXinSettingsAutoSaver、Timer
- `events/`：Event、MoveEvent、TravelEvent（鞘翅飞行事件系统）
- `i18n/`：WaveXinI18n（中英双语，语言文件在 `assets/xintools/lang/`）
- `gui/`：WaveXinEnumDropdown（翻译枚举下拉框）
- `mixin/`：MixinPlayerEntity、MixinClientPlayerEntity（移动/飞行事件）、MixinClientPlayNetworkHandler（标题/聊天文本事件）
## 标准文件路径指引（必读）

| 用途 | 路径 |
|---|---|
| 已确认的需求（含验收标准） | `docs/requirements.md` |
| 技术方案 / API 细节 | `docs/technical.md` |
| 代码与文档设计规范 | `docs/design.md` |
| 工作流程与执行步骤 | `docs/process.md` |
| 分阶段开发路线图 | `docs/roadmap.md` |
| 文档索引 | `docs/README.md` |
| 每日开发日志（先看最新的） | `dev-log/`（按日期倒序） |
| 日志记录规范 | `dev-log/README.md` |

## 工作说明

### 开始工作时

1. 读取本文件，定位标准文件路径。
2. 读取 `dev-log/` 最新日志，了解当前进度与待办事项。
3. 读取 `docs/roadmap.md`，确认当前所处阶段。
4. 开发具体功能前，读取 `docs/requirements.md` 中对应需求与验收标准。

### 工作过程中

1. 遵循 `docs/design.md` 的命名与代码规范。
2. 技术实现参考 `docs/technical.md`；遇到新 API 先核实（读 Meteor 源码/文档）再编码。
3. 小步推进：每阶段只做一件事，完成并验证后再进入下一步，不做无关改动。
4. 有不确定的关键需求点，必须先向用户提问确认，不自行假设。

### 完成工作时

1. 必须运行 `./gradlew build` 验证编译通过。
2. 回填更新 `docs/technical.md`（实际实现）与 `docs/roadmap.md`（状态）。
3. 更新当日开发日志 `dev-log/YYYY-MM-DD.md`（完成事项 / 待办事项 / 问题记录）。
4. 需求完成时更新 `docs/requirements.md` 对应需求状态。

### 交流语言

- 与用户交流使用中文。
- 代码中设置名、描述、日志使用英文；逻辑注释可用中文。

## 构建命令速查

```bash
./gradlew build        # 编译打包（每次修改后必跑）
./gradlew runClient    # 运行客户端测试
```
