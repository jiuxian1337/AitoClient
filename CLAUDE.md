# AitoClient 开发约束（AI 会话必读）

Minecraft 1.8.9 Forge + OneConfig 模块项目。完整规范见 `CODING_STANDARD.md`，以下为必须遵守的关键规则。

## 硬性规则

1. **不写任何注释**（行内、javadoc 都不写）。意图靠命名表达。
2. **运行时状态字段必须 `@Exclude`**，否则 OneConfig 的 Gson 保存整个 config 对象会崩溃、配置丢失。
3. **`initialize()` 必须在 `hideIf`/`addDependency` 之前**。
4. **私有 MC 字段用 `ReflectionHelper.setPrivateValue(声明类.class, 实例, 值, "MCP名", "SRG名")`**，必须同时传 MCP 和 Forge SRG 名（dev 用 MCP、生产用 SRG）。**禁用 `ObfuscationReflectionHelper`**（1.8.9 生产映射失效）。
5. **热路径（onTick/onRender）禁对象分配、禁每 tick 反射找字段、禁字符串解析**。反射字段要缓存。
6. **模块默认关闭**。生命周期：`super(new Mod(...), "...json")` → `initialize()` → `hideIf(...)`。
7. 事件回调入口判空（`mc.thePlayer` 可为 null）；改 MC 状态只能在主线程。

## 风格

- 直接 MC API 调用，扁平结构，一个模块一个类，不做过度抽象。
- 命名自解释：布尔方法 `is*`/`can*`/`should*`，配置字段驼峰。
- 热路径用命令式循环，不用 stream/lambda 分配。
- 不留 TODO、占位、未用字段、空方法体。
- 改动后跑 `./gradlew :1.8.9-forge:compileJava`。

## 关键背景

- 用户偏好：不要注释、中文沟通、参考现有模块（AutoClicker/Eagle）写法。
- 生产环境字段名是 Forge SRG（查 `mappings-srg-named.srg`），不是 `.tiny` 的 intermediary 名。
