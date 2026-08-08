# XingduAddon 开发路线图

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
  - 更新 `XingduAddon.java` 模块注册
  - `./gradlew build` 构建验证通过（仅余 ElytraSwap 既有 unchecked 警告）
  - 同时修复 `gradle-wrapper.properties` 下载源（阿里云镜像超时 → 腾讯云镜像）

## 阶段 2+：待规划 ⏳

- 等待用户提出新需求；新需求先入 `docs/requirements.md` 确认后再规划。
