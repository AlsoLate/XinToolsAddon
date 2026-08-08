# XingduAddon 设计规范

> 项目代码与文档的统一规范。所有开发工作必须遵守。

## 1. 命名规范

| 对象 | 规范 | 示例 |
|---|---|---|
| 模块类名 | PascalCase，模块功能名 | `Automend`、`PacketEat` |
| 模块显示名 | 与类名一致（首字母可简化场景除外） | "Automend" |
| 文件/类名 | 类名与文件名一致 | `Automend.java` |
| 变量 | camelCase | `lastNoBottle` |
| 常量 | UPPER_SNAKE_CASE | `MAX_HEIGHT` |
| 设置名 | 英文，单词间空格分隔 | `Inventory Switch` |
| 设置描述 | 英文完整句，说明用途 | `"Hold this key to auto-mend..."` |

## 2. 模块设计规范

- 所有模块继承 `Module`，注册到 `XingduAddon.CATEGORY`。
- 设置必须分组：常用设置放入 `settings.getDefaultGroup()`。
- 每个设置必须有 `name` 与 `description`，名称简洁、描述说明行为。
- 模块构造函数的描述参数需说明触发方式（如 "auto mend when you hold R"）。
- 事件处理使用 `@EventHandler` 注解。

## 3. 代码风格

- 缩进 4 空格，遵循项目现有风格（参考现有模块）。
- 中文注释用于解释逻辑意图；设置名/描述、日志使用英文。
- 不在模块代码中写死按键等用户可配置项，一律通过 Setting 暴露。
- 禁止遗留未使用 import 与调试输出（System.out / printStackTrace）。

## 4. 日志规范

- 模块功能相关提示使用 `ChatUtils.sendMsg()`，重要错误用红色 `ChatFormatting.RED`。
- 消息前缀统一 `[XingduAddon]` + 模块名。

## 5. 文档规范

- `docs/requirements.md`：只记录**已确认**的需求；需求变更需重新确认后更新。
- `docs/technical.md`：技术方案实现后再回填实际采用的做法，保持与代码一致。
- `docs/design.md`：规范变更需在本文件同步更新。
- `docs/roadmap.md`：开发计划，完成一项即更新状态。
- `dev-log/`：开发日志，格式见 `dev-log/README.md`。

## 6. 构建与提交流程

1. 修改代码后必须运行 `./gradlew build` 验证编译。
2. 编译通过后方可记录到当日开发日志。
3. 提交信息遵循：`feat:`（新功能）/ `fix:`（修复）/ `refactor:`（重构）/ `docs:`（文档）。
