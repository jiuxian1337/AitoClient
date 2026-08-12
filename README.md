# AitoClient

基于 [OneConfig](https://github.com/Polyfrost/OneConfig) 的 Minecraft 1.8.9 Forge 客户端，采用模块化体系——每个模块是独立的 `Config`，自带开关、配置页和持久化文件。

## 模块

模块位于 `cc.aito.module.impl.pvp`，在 `ModuleManager` 中注册。每个模块默认关闭，需要在 OneConfig 界面里手动开启。

| 模块 | 说明 |
|---|---|
| **AutoClicker** | 自动攻击。可调 CPS / NoTargetCPS / HitSelect / AttackReduceTick |
| **NoJumpDelay** | 消除跳跃间隔（连跳） |
| **NoClickDelay** | 消除攻击点击冷却 |
| **FastPlace** | 极速放置方块（消除放置延迟） |
| **Eagle** | 边缘自动蹲（合法搭桥辅助）。含 Pitch 检查、方块黑白名单、随机蹲延迟、方块数量 HUD |

## 环境要求

| 项 | 要求 |
|---|---|
| Minecraft | 1.8.9 |
| Forge | 11.15.1.2318 |
| Java（客户端） | 8（Legacy Forge 的 Launchwrapper 无法在 Java 9+ 上运行） |
| Java（构建） | 17 |
| OptiFine | 可选（已验证可与预览版共存） |

## 安装

1. 构建出产物 jar（见下方开发）。
2. 将 `versions/1.8.9-forge/build/libs/AitoClient-1.8.9-forge-1.0.0.jar` 放入 `mods` 文件夹。
3. 首次启动时，内置的启动器会自动下载 OneConfig 到游戏目录的 `./OneConfig/` 并注入，无需手动安装。
4. 进入游戏，按 OneConfig 的界面按键（默认右 Shift）打开配置。

## 开发

```bash
./gradlew :1.8.9-forge:compileJava   # 编译
./gradlew :1.8.9-forge:build         # 打包（产物在 versions/1.8.9-forge/build/libs/）
./gradlew :1.8.9-forge:runClient     # 运行开发客户端（自动使用 Java 8 工具链）
```

- Gradle 守护进程跑在 Java 17（构建插件要求），`runClient` 的客户端进程通过 `build.gradle.kts` 里的工具链配置固定为 Java 8。
- 打包产物 `AitoClient-1.8.9-forge-1.0.0.jar`（不带 `-dev` 后缀的那个）才是放进 `mods` 的 jar。

## 项目结构

```
src/main/java/cc/aito/
├── AitoClient.java              # 主类（@Mod 入口）
├── module/
│   ├── Module.java              # 模块基类（extends Config，含全部事件钩子）
│   ├── ModuleManager.java       # 事件分发：向所有启用模块派发 OneConfig 事件
│   └── impl/pvp/                # PVP 模块
└── utils/
    └── Wrapper.java             # mc 实例等共享工具
```

## 文档

- [CODING_STANDARD.md](CODING_STANDARD.md) — 代码开发规范（效率/可读性/安全性/质量）
