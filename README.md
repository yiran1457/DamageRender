# DamageRender (Stonecutter)

Multi-version DamageRender workspace managed by [Stonecutter](https://stonecutter.kikugie.dev/) 0.9.6.

## Supported versions

| Node | Loader | Minecraft | Build script |
| --- | --- | --- | --- |
| `1.19.2-forge` | Forge | 1.19.2 | `build.forge.gradle.kts` |
| `1.20.1-forge` | Forge | 1.20.1 | `build.forge.gradle.kts` |
| `1.21.1-neoforge` | NeoForge | 1.21.1 | `build.neoforge.gradle.kts` |

Active editable tree: **`src/`** (currently `1.20.1-forge`)

Original single-version projects are kept under `DamageRender-*` for reference.

## Common commands

```bat
rem Switch active version (rewrites conditionals in src/)
gradlew stonecutterSwitchTo1.19.2-forge
gradlew stonecutterSwitchTo1.20.1-forge
gradlew stonecutterSwitchTo1.21.1-neoforge

rem Compile / build one version
gradlew :1.19.2-forge:build
gradlew :1.20.1-forge:build
gradlew :1.21.1-neoforge:build

rem Compile all nodes
gradlew compileJava
```

## Source conditionals

Shared Java sources use Stonecutter comments:

- `//? if forge { ... //?} else { ... //?}` for loader splits
- `//? if =1.19.2 { ... //?} else { ... //?}` inside Forge for 1.19.2 vs 1.20.1

Forge-only helpers (`DamageColorManager`, `DamageNumberRenderer`, `Mat4Util`) are excluded on the NeoForge source set.

## Notes

- Gradle **9.4.1**.
- Stonecutter **0.9.6**.
- Proxy settings live in root `gradle.properties`.
- Version pins live in `versions/<node>/gradle.properties`.
