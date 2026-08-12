# AitoClient 代码开发规范

本规范针对本项目（Minecraft 1.8.9 Forge + OneConfig 模块体系）的实际特性制定，兼顾运行效率、可读性、安全性和代码质量。所有模块代码必须遵守。

## 1. 运行效率

本项目模块运行在热路径上：`onTick` 每秒约 20 次，`onRender`/`onHudRender` 每秒约 60 帧。下列规则针对这些路径。

### 1.1 热路径禁止
- 每 tick/每帧分配对象：不 `new ArrayList`、不自动装箱、不用捕获变量的 lambda。
- 每 tick 反射找字段：`ReflectionHelper.setPrivateValue` 每次调用都会 `getDeclaredField` + `setAccessible`。热路径里应把 `Field` 缓存为 `static final`，只 `setAccessible` 一次，之后 `field.setInt(...)`。
- 每 tick 做字符串解析（黑名单等）：预解析并缓存，不要在回调里 `split`/`toString`。

### 1.2 热路径建议
- 低频事件（key、mouse、chat、packet、world、screen）可以宽松，但别为它们引入每 tick 的轮询。
- 实体遍历：用 `WorldLoadEvent` 缓存目标集合，避免每 tick 全量扫世界。
- 计时用 `System.currentTimeMillis()`，多次调用无妨，但可缓存到局部变量。
- OneConfig 事件都在 MC 主线程（渲染线程）。不要在里面做阻塞/重活；后台任务用独立线程算、主线程应用结果，注意可见性。

## 2. 可读性

### 2.1 不写注释
- **代码里不写任何注释**（行内、javadoc 都不写）。意图靠命名和结构表达。

### 2.2 命名
- 配置字段：驼峰，与 OneConfig annotation 的 `name` 对应（如 `sneakDelay`、`pitchThreshold`）。
- 运行时状态字段：驼峰 + `@Exclude`。
- 布尔方法用 `is*`/`can*`/`should*`（如 `isHoldingBlock()`、`canActivate()`）。
- 常量用 `UPPER_SNAKE_CASE`。

### 2.3 结构
- 一个方法做一件事。事件回调只做调度，逻辑拆到带语义的私有方法（参考 `onPreEntityUpdate`/`onPostTick`）。
- 复杂布尔表达式提取为方法（`isAtEdge()`、`isBlacklisted(stack)`）。
- 模块内：配置字段在前、运行时状态字段集中在后、构造器、事件方法、私有工具方法。
- 模块放 `cc.aito.module.impl.<分类>`，按 `ModType` 分类。

## 3. 安全性

### 3.1 OneConfig 持久化（本项目最大坑）
- **所有运行时状态字段必须加 `@Exclude`**（或 `transient`）。否则 Gson 序列化整个 config 对象时崩溃，导致配置保存失败（AutoClicker 的 `entityHit` 踩过）。
- **`initialize()` 必须在 `hideIf`/`addDependency` 之前调用**。`optionNames` 在 `initialize()` 里才构建，顺序反了会静默失效。
- 配置字段只存用户设置值，不存运行时可变状态。

### 3.2 生产环境字段访问
- 访问私有的 Minecraft 字段用：
  ```java
  ReflectionHelper.setPrivateValue(声明类.class, 实例, 值, "MCP名", "SRG名");
  ```
  必须**同时传 MCP 名和 Forge SRG 名**：dev 环境匹配前者，生产环境匹配后者。
- **不要用 `ObfuscationReflectionHelper`**——1.8.9 生产环境它的 SRG 映射方向错误，字段找不到（NoClickDelay 踩过）。
- SRG 名从 `mappings-srg-named.srg` 查（如 `leftClickCounter` → `field_71429_W`），**不是** `.tiny` 里的 intermediary 名。
- `ReflectionHelper.findField` 只查声明类的 `getDeclaredField`，不查父类——`jumpTicks` 在 `EntityLivingBase` 就要传 `EntityLivingBase.class`，不能传 `mc.thePlayer.getClass()`。

### 3.3 运行时防护
- 事件回调入口判空：`mc.thePlayer` 可为 null（未进世界）。
- 注入按键/改字段前检查：`mc.currentScreen == null`、`onGround`、`isInWater` 等。
- 改 MC 状态（按键 `KeyBinding.setKeyBindState`、字段）必须在主线程。

## 4. 代码质量

- 模块生命周期固定顺序：构造 → `super(new Mod(...), "...json")` → `initialize()` → `hideIf(...)`（如需）。
- 模块默认关闭（`Module` 2 参构造默认 `false`）；需要默认开启才显式传 `true`。
- 通用逻辑放 `cc.aito.utils`（如 `Wrapper`），同类模块复用，不复制粘贴。
- **不为"扩展性"提前抽象**：一个模块一个类。不引入工厂、接口、泛型层。
- 每次改动跑 `./gradlew :1.8.9-forge:compileJava`，保证编译通过。

## 5. 降低 AI 味

针对 AI 生成代码的常见毛病，明确禁止：

- **不写注释/解释性 javadoc**。这是本项目最反感的。
- **不匹配现有风格**：本项目是直接 MC API 调用、扁平结构。不要写出"教科书式"的封装层。
- **不过度工程**：不需要泛型工厂、策略模式、装饰器。Vape 移植是"参考行为自己实现"，不是照搬框架结构。
- **不写防御性废话**：只在真正可能 null/出错处判空，不为"保险"到处判。
- **热路径用命令式**：循环优先于 stream，避免每 tick 的 lambda 分配。
- **命名直接**：不用 `xxxInternal`、`xxxHelper`、`xxxUtil` 堆砌；不用 `data`/`info`/`obj` 这类空泛词。
- **不留半成品**：不留 TODO、占位实现、未使用字段/方法、无意义的空方法体。
