# XinToolsAddon 开发路线图

> 分阶段开发计划。遵循"小步推进、稳定安全"原则，完成一项更新一项状态。
> 状态：`✅ 已完成` / `🔄 进行中` / `⏳ 待开始`

## 阶段 0：项目管理体系搭建 ✅（2026-08-07）

- 创建 `docs/` 文档体系（需求、技术、设计规范、执行步骤、路线图）
- 创建 `dev-log/` 开发日志体系
- 创建 `CLAUDE.md` 指引文件
- 确认需求 1（Automend 改造）细节

## 阶段 1：Automend 模块改造 ✅（2026-08-07）

- **目标**：完成需求 1（见 `docs/requirements.md` 需求 1）
- **完成内容**：
  - `ADAutomend.java` 重命名为 `Automend.java`，类名/模块显示名同步更新
  - 新增 `Trigger Key` 设置（KeybindSetting，单键），按住触发替换写死的 A+D 判断
  - 更新 `XinToolsAddon.java` 模块注册
  - `./gradlew build` 构建验证通过（仅余 ElytraSwap 既有 unchecked 警告）
  - 同时修复 `gradle-wrapper.properties` 下载源（阿里云镜像超时 → 腾讯云镜像）

## 阶段 2：PlayerAlarms 模块（BetterPlayerAlarms 合并 PlayerNotifier）✅（2026-08-08）

- **目标**：完成需求 2（见 `docs/requirements.md` 需求 2）
- **完成内容**：
  - `BetterPlayerAlarms.java` 重写并重命名为 `PlayerAlarms.java`（文件/类名/显示名）
  - 逻辑改为 PlayerNotifier 风格：仅聊天彩色消息（`[+]`/`[-]`），删除全部响铃设置
  - 保留全部事件：加入/离开/进出渲染距离/游戏模式变更
  - 更新 `XinToolsAddon.java` 模块注册
  - 清理 `liuliuliu0127` 旧项目残留文件（构建失败修复）
  - `./gradlew build` 构建验证通过

## 阶段 3：项目全量重命名为 XinToolsAddon ✅（2026-08-08）

- **目标**：完成需求 3（见 `docs/requirements.md` 需求 3）
- **完成内容**：
  - 项目名：`XingduAddon` → `XinToolsAddon`（GitHub 仓库 `AlsoLate/XinToolsAddon`，用户手动执行 `gh repo rename`，下同）
  - 包名：`alsolate.xingdu.addon` → `alsolate.xintools.addon`
  - 主类：`XingduAddon` → `XinToolsAddon`，Category 文本 `Xingdu` → `XinTools`
  - 资源目录：`assets/xingdu/` → `assets/xintools/`
  - Gradle：`archives_base_name=XinToolsAddon`、`maven_group=alsolate.xintools`、`rootProject.name=XinToolsAddon`
  - fabric.mod.json：`id/name=xin-tools-addon`、entrypoint 同步
  - mixins.json：`xingdu-addon.mixins.json` → `xin-tools-addon.mixins.json`
  - 文档/HTML 说明书/开发日志全量同步
  - `./gradlew build` 验证通过，jar 名 `XinToolsAddon-<version>.jar`

## 阶段 4+：待规划 ⏳

- 等待用户提出新需求；新需求先入 `docs/requirements.md` 确认后再规划。
