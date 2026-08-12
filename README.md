# AitoClient

A Minecraft 1.8.9 Forge client built on [OneConfig](https://github.com/Polyfrost/OneConfig), featuring a modular module system.

## Modules

Each module is a OneConfig `Config` subclass under `cc.aito.module`, with its own config page, toggle and JSON profile. Modules are registered in `ModuleManager`.

- `AutoClicker` (`cc.aito.module.impl.pvp`) — CPS / NoTargetCPS / HitSelect / AttackReduceTick

## Development

Requirements: JDK 17 (Gradle daemon / build) and JDK 8 (1.8.9 client runtime).

```bash
./gradlew :1.8.9-forge:compileJava   # compile
./gradlew :1.8.9-forge:runClient     # run the client (uses Java 8 automatically)
```

The client JVM is pinned to a Java 8 toolchain in `build.gradle.kts` because legacy Forge's Launchwrapper cannot run on Java 9+.
