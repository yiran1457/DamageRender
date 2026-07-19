# DamageRender

[English](README_EN.md)

DamageRender 会在实体受到伤害或获得治疗时，在实体上方显示动态飘字，让战斗数值更直观。

## 功能

- 显示伤害与治疗数值，支持设置最小显示值和是否显示治疗数字。
- 数字随时间上飘、减速、淡出，并可调整存活时间和初始纵向速度。
- 支持按伤害类型合并短时间内连续产生的数字；也可以将同一实体的伤害合并为单个数字。
- 支持为不同伤害类型设置颜色，并可通过命令管理颜色配置。
- 支持自定义数字纹理；纹理使用 `0-9` 与小数点组成的水平图集。
- 支持调整数字的基础缩放：可启用或关闭对数缩放，并设置对数底数。
- 服务端会按玩家所在维度和显示距离发送数据；同一 tick 的伤害会合并成批包，减少网络开销。

## 支持版本

| Minecraft | 加载器 |
| --- | --- |
| 1.19.2 | Forge |
| 1.20.1 | Forge |
| 1.21.1 | NeoForge |
| 26.1.2 | NeoForge |

## 配置

所有显示设置均为客户端配置，可从模组配置界面修改，也可使用 `/damagerender <选项> [值]` 查看或保存设置。

常用选项：

| 选项 | 默认值 | 说明 |
| --- | --- | --- |
| `showDistance` | `32` | 接收并显示飘字的最远距离。 |
| `maxShowRender` | `128` | 同时显示的最大飘字数量。 |
| `enableCombineString` | `true` | 合并相近时间内的同类伤害数字。 |
| `enableCombineEntity` | `false` | 将同一实体的伤害合并为一个数字。 |
| `enableBaseScaleLogarithm` | `true` | 是否按伤害值进行对数基础缩放。 |
| `baseScaleLogBase` | `10.0` | 基础缩放的对数底数，范围为 `1.01` 至 `10,000,000`。 |
| `initialUpwardSpeed` | `0.20` | 新飘字的初始纵向速度，正数向上、负数向下，范围为 `-10.0` 至 `10.0`。 |
| `texture` | 内置数字纹理 | 数字图集资源位置。 |

示例：

```mcfunction
/damagerender enableBaseScaleLogarithm false
/damagerender baseScaleLogBase 1000
/damagerender initialUpwardSpeed 0.35
/damagerender setDamageColor magic #AA55FF
```

基础缩放设置会应用于新生成或新合并的数字；初始纵向速度仅应用于新生成的数字。
