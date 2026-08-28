# XinToolsAddon
这是一个适用于Minecraft 1.21.11，Fabric 0.19.3，MeteorClient-1.21.11-86的MeteorAddon，专门为中国2B2T服务器2b2t.xin设计，目前项目推进了两个版本，共有两名开发者参与编写。
目前已知问题如下，预计会在2026年国庆节左右更新下一版本，并尽量解决下面的问题：
自动排队答题无法使用（进度100%）
翻译质量过低（进度40%）
快速经验瓶的Auto模式不稳定（进度10%）
鞘翅飞行为固定速度（已有变速逻辑解决方案）
发包进食疑似存在卡金苹果吃不进去（还在排查问题）
嗅探兽名称标签分散（会重写）
基地狩猎扫图（已有新高质量找基地逻辑，会重写）
欢迎Pr和Issues，本项目长期更新，作者QQ3958426615

包名：`alsolate.xintools.addon`  
分类：游戏内模块分类 **XinTools**

---

## 功能概览

| 内部名 | 中文名（i18n 开） | 说明 |
|--------|------------------|------|
| i18n | 汉化开关 | 总开关：Meteor/XinTools 界面汉化；关则恢复英文 |
| FastXP | 快速经验瓶 | 快速扔经验瓶；默认 Silent；Auto 按耐久自动修补 |
| AutoLogin | 自动登录 | 2b2t.xin 登录/小红花点击（**不含答题**，答题见 AutoQueue） |
| AutoQueue | 自动排队答题 | 排队验证题自动回答 |
| BaseFinder | 基地狩猎扫图 | 螺旋/普通扫图、容器记录、Xaero 路径点 |
| IMGTips | 提示信息 | 视距/图腾/死亡坐标/延迟/药水等提示 |
| IMGWorldStats | 统计信息 | 世界与游玩数据 HUD |
| ChatHighLight | 聊天高亮 | 聊天玩家高亮与路人前缀 |
| CustomFov | 自定义视角 | 世界 FOV / 手持物 FOV |
| PortalESP | 传送门高亮 | 下界/末地/折跃门方块框 |
| Prefix | 聊天前缀 | 修改 Meteor 聊天前缀 |
| PacketEat | 发包进食 | 发包减少进食打断 |
| PlayerAlarms | 玩家警报 | 加入/离开/视距/模式变更提醒 |
| ElytaFly+ | 鞘翅飞行+ | 额外鞘翅控制模式 |
| SnifferNameTags | 嗅探兽名称标签 | 嗅探兽自定义名牌 |

---

## 构建

**要求：** JDK 21、网络（拉依赖）

```bash
# Windows
.\gradlew.bat clean
.\gradlew.bat build --rerun-tasks

# Linux / macOS
./gradlew clean build --rerun-tasks
```

产物：`build/libs/*.jar` → 放入 `.minecraft/mods/`（与 Meteor Client、Fabric API 一起）。

只保留一份 XinTools jar，避免重复加载。

---

## 汉化（i18n）

1. 游戏内打开模块 **汉化开关（i18n）**。
2. 开启后：
   - Meteor / 第三方模块标题与设置走 `assets/xintools/lang/meteor_zh_cn.json`
   - XinTools 模块走 `assets/xintools/lang/zh_cn.json`（**不依赖** Minecraft 语言是否为中文）
   - Config / HUD / Macros / Profiles / Baritone(PathManager) 的 Setting 一并翻译
   - 设置分组名：`General`→基础设置，`Control`→控制，`Bind`→绑定 等（此项仍有问题，无法完成翻译）
3. 关闭后硬还原为构造时英文原文；GUI 会自动重开当前 Tab。
4. 原版文字渲染**始终强制**（避免中文字变成空白），与 i18n 开关解耦。

### 关键 key 规则

`WaveXinI18n.keySegment(name)`：

- 驼峰 → 下划线小写
- 连续大写不会在中间拆开：`IMGTips` → **`imgtips`**（不是 `img_tips`）
- `IMGWorldStats` → **`imgworld_stats`**

模块标题示例：

- `module.wavexin.imgtips.title` → 提示信息  
- `module.wavexin.imgworld_stats.title` → 统计信息  

设置：`setting.wavexin.{moduleSeg}.{groupSeg}.{settingSeg}.title`  
分组：`group.wavexin.{moduleSeg}.{groupSeg}`  
通用分组：`SettingGroup.Meteor.General` 等  

改模块 **内部 name** 或增删设置后，必须同步改 `zh_cn.json` / `en_us.json`，并用实际 `keySegment` 结果核对，禁止想当然写 `img_tips`。

---

## 目录结构（仅供参考，请以实际为准）

```
src/main/java/alsolate/xintools/addon/
  XinToolsAddon.java          # 入口，注册模块
  core/                       # WaveXinModule、设置持久化等
  i18n/
    MeteorTranslator.java     # 全局翻译 + Config/HUD/Macros/Profiles/PathManager + GUI 刷新
    WaveXinI18n.java          # XinTools 专用；自带 zh/en 表，不依赖游戏语言
  mixin/                      # 模块/设置翻译、文字渲染、光标、Accessor 等
  modules/                    # 各功能模块
src/main/resources/
  fabric.mod.json
  xin-tools-addon.mixins.json
  assets/xintools/lang/
    zh_cn.json                # XinTools 中文
    en_us.json                # XinTools 英文
    meteor_zh_cn.json         # Meteor 生态中文（体积大）
    meteor_en_us.json
```

---

## 已知行为 / 注意

- **FastXP** 默认旋转模式为 **Silent**；Auto 模式只在带经验修补的装备低于阈值时向下投掷经验瓶。
- **自动登录** 无答题；每日功能为小红花点击并领取余额奖励。（存在问题）
- 模块设置界面 Esc 退出时尽量保持鼠标位置（`ModuleScreenCursorMixin`）。（存在问题，不生效）
- Mojang 映射：`Minecraft.rightClickDelay`（不要用旧名 `itemUseTicks`）。
- 与用户沟通模块时优先用 **中文名**，改文件时对照 `zh_cn.json` 的真实 key。

---

## 许可证
使用MIT
见仓库根目录 `LICENSE`
