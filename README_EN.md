# DamageRender

[简体中文](README.md)

DamageRender displays animated floating numbers above entities when they take damage or receive healing, making combat values easier to read.

## Features

- Shows damage and healing values, with configurable minimum value and healing-number visibility.
- Numbers rise, slow down, and fade out; their lifetime and initial vertical velocity are configurable.
- Merges nearby, short-interval damage numbers by type, with an optional mode to merge all damage for the same entity.
- Supports per-damage-type colours, managed through in-game commands.
- Supports custom number textures using a horizontal atlas containing `0-9` and a decimal point.
- Supports configurable logarithmic base scaling for damage numbers, including an on/off switch and logarithm base.
- Sends damage data by dimension and player display distance, and batches events from the same tick to reduce network traffic.

## Supported versions

| Minecraft | Loader |
| --- | --- |
| 1.19.2 | Forge |
| 1.20.1 | Forge |
| 1.21.1 | NeoForge |
| 26.1.2 | NeoForge |

## Configuration

All display settings are client-side. Change them in the mod configuration screen or query/save them with `/damagerender <option> [value]`.

| Option | Default | Description |
| --- | --- | --- |
| `showDistance` | `32` | Maximum distance for receiving and displaying floating numbers. |
| `maxShowRender` | `128` | Maximum number of floating numbers visible at once. |
| `enableCombineString` | `true` | Merges nearby damage numbers of the same type. |
| `enableCombineEntity` | `false` | Merges damage for the same entity into one number. |
| `enableBaseScaleLogarithm` | `true` | Enables logarithmic base scaling based on damage value. |
| `baseScaleLogBase` | `10.0` | Logarithm base for base scaling; range: `1.01` to `10,000,000`. |
| `initialUpwardSpeed` | `0.20` | Initial vertical velocity of a new number; positive rises, negative falls; range: `-10.0` to `10.0`. |
| `texture` | bundled number texture | Resource location of the number atlas. |

Examples:

```mcfunction
/damagerender enableBaseScaleLogarithm false
/damagerender baseScaleLogBase 1000
/damagerender initialUpwardSpeed 0.35
/damagerender setDamageColor magic #AA55FF
```

Base-scaling settings apply to newly created or newly merged numbers. Initial vertical velocity applies only to newly created numbers.
