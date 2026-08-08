# XingduAddon

A minecraft 1.21.11 meteor client addon which added some features for servers like 2b2t.xin.

![ICON_BIG](https://github.com/AlsoLate/XingduAddon/blob/main/src/main/resources/assets/xingdu/icon_big.png?raw=true)

## Modules

| Module | Description |
|---|---|
| Automend | Hold the bound key to throw XP bottles straight down to repair equipment; release to stop. |
| PacketEat | Eat without interruption by sending packets to the server. |
| BetterPlayerAlarms | Enhanced player alarms: join/leave server, enter/leave render distance, gamemode changes. |
| XinQueue | Automatically answer questions when queueing in 2b2t.xin. |
| MeteorTextFix | Overrides Meteor's text rendering behavior to fix compatibility issues. |
| BetterElytraFly | Improved elytra flight control with automatic elytra replacement. |
| SnifferNametags | Displays custom nametags for sniffer entities. |
| AutoLogin | Automates 2b2t.xin login, quiz, check-in, and join flows. |
| BaseFinder | Outward map scanner with chunk-loading pauses, container recording, and Xaero waypoints. |

## Build

```bash
./gradlew build
```

Built jar: `build/libs/XingduAddon-<version>.jar`
