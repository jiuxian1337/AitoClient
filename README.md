# AitoClient

Minecraft 1.8.9 Forge 客户端，基于 [OneConfig](https://github.com/Polyfrost/OneConfig)。功能以模块形式组织，覆盖 PVP、工具等多个场景；每个模块的开关与参数都在游戏内的 OneConfig 界面中完成，无需手动改文件。
<img width="1920" height="1440" alt="4e038d0e58315f8c7bef8747f9f37dee" src="https://github.com/user-attachments/assets/49efd924-4af8-49e6-a9e7-cc7e6d9eba57" />

## 安装

1. 构建出产物 jar（见下方「构建」）。
2. 将 `AitoClient-1.8.9-forge-1.0.0.jar` 放入 Minecraft 的 `mods` 文件夹。
3. 启动游戏。首次启动时会自动下载并注入 OneConfig（位于游戏目录的 `./OneConfig/`），无需手动安装。
4. 进入游戏后按 OneConfig 界面键（默认 **右 Shift**）打开配置，开启并调整需要的模块。

## 使用

- 所有模块默认关闭，在 OneConfig 界面中手动开启。
- 模块的开关与参数会保存到各自的配置文件，重启后自动恢复。
- 具体有哪些模块、各自支持哪些选项，以游戏内 OneConfig 界面为准。

## 环境要求

| 项 | 要求 |
|---|---|
| Minecraft | 1.8.9 |
| Forge | 11.15.1.2318 |
| Java（运行） | 8 |
| Java（构建） | 17 |
| OptiFine | 可选 |

## 构建

```bash
./gradlew :1.8.9-forge:build
```

产物位于 `versions/1.8.9-forge/build/libs/`，取不带 `-dev` 后缀的 jar 放入 `mods`。

## 开发者文档

- [CODING_STANDARD.md](CODING_STANDARD.md) — 代码开发规范
