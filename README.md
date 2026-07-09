# DamageRender

一个 Minecraft Forge 模组，在实体受击 / 治疗时于其头顶渲染浮动的伤害数字（damage number / 飘字），
并在大量伤害场景下通过服务端合批发包 + 客户端预合并保证性能稳定。

- **Minecraft**：1.20.1
- **Forge**：47.x（loader range `[47,)`）
- **Java**：17
- **mod_id**：`damagerender`
- **版本**：见 `gradle.properties` 的 `mod_version`（当前 `1.3.1-beta3`）
- **作者**：`_yi_ran_`
- **License**：All Rights Reserved

> 本模组需要同时安装在客户端和服务端。服务端负责采集伤害事件并合批发包，客户端负责渲染。

---

## 功能概览

- 实体（生物等）受击时，在头顶向上飘出伤害数值，带初速度、阻尼、淡出、缩小动画。
- 治疗事件同样渲染（绿色，走 `heal` 旁路）；可通过配置开关关闭治疗数字显示。
- **伤害合并**：同一实体、同一伤害类型的多条飘字会累加成一个数字，避免满屏重复数字。
- **颜色映射**：按伤害类型（damage type）配置颜色，支持命令实时增删改查并持久化。
- **就近过滤**：服务端只把玩家可见距离内的伤害发给该玩家，按玩家独立配置可见距离。

---

## 架构与数据流

```
伤害/治疗事件 (服务端, LivingDamageEvent / LivingHealEvent)
        │  ServerEventHandler
        ▼
ServerDamageInfoManager.pending   (按 tick 攒批)
        │  ServerTickEvent (Phase.END)
        ▼
按玩家就近过滤 → DamageInfoBatchPacket   (合包, 每玩家一包)
        │  SimpleChannel
        ▼
DamageInfoBatchPacket.handle (客户端)
        │  按 (entityId, damageType) 预合并累加
        ▼
ClientDamageInfoManager.add   (mergeIndex: O(1) 跨批合并)
        │
        ▼
ClientEventHandler.render (RenderLevelStageEvent.AFTER_ENTITIES)
        │  每帧遍历 + 末尾 removeDead()
        ▼
屏幕
```

### 关键类

| 类 | 作用 |
|---|---|
| `DamageRender` | `@Mod` 主类，注册事件、网络通道 |
| `ClientConfig` | Forge 配置项（渲染距离、合并、存活时间、治疗开关等） |
| `ServerEventHandler` | 监听 `LivingDamageEvent` / `LivingHealEvent` / `ServerTickEvent` / 玩家退出 |
| `ServerDamageInfoManager` | 服务端缓冲队列，tick 末按玩家就近过滤后合批发包 |
| `DamageInfoData` | 单条伤害信息 record + 网络序列化 |
| `DamageInfoBatchPacket` | 合包；客户端收包时按 `(entityId, damageType)` 预合并 |
| `DamageInfoPacket` | 单条包（兼容 / 兜底） |
| `UpdateConfigPacket` | 客户端 → 服务端，上报玩家可见距离 |
| `ClientDamageInfoManager` | 客户端飘字列表 + `mergeIndex`（O(1) 合并查找） |
| `DamageColorManager` | 伤害类型 → 颜色映射统一管理器：内存、JSON 编解码、文件持久化 |
| `ClientEventHandler` | 渲染入口，每帧遍历飘字调用 `render` |
| `DamageString` | 单个飘字：位置/速度/生命/颜色/缩放/文字缓存 |
| `Command` | 客户端命令，运行时调整配置 + 重载颜色映射 |

### 性能设计要点

- **服务端合批**：一个 tick 内的所有伤害攒到 `ServerTickEvent.END` 一次性、每玩家一包发出，避免逐条发包。
- **客户端预合并**：收包时按 `(entityId, damageType)` 累加 `amount`，同 tick 内的同类型伤害无条件合并（不受年龄限制）。
- **`mergeIndex`**：`Int2ObjectOpenHashMap<entityId, Object2ObjectOpenHashMap<typeKey, DamageString>>`，
  把跨批合并查找从 O(n) 降到 O(1)。
- **fastutil 集合**：`ObjectArrayList`（无 range check / modCount）、`Object2ObjectOpenHashMap`（开放寻址，比 `HashMap` 链式快 ~20–30%）、`Object2IntOpenHashMap`（原始 int，无装箱）。
- **单线程直绘**：`Font.drawInBatch` 本身已是批量操作，单飘字渲染仅一次矩阵变换 + 一次 drawInBatch；
  实测几千飘字时多线程拆分反而因调度/同步开销更慢，故采用单线程直绘。
- **文字宽度缓存**：`DamageString.formatDamage()` 在内容变化时缓存 `halfWidth`，避免每帧重算 `font.width`。

---

## 配置项

类型为 `CLIENT` 的 Forge 配置（`damagerender-client.toml`）：

| 配置键 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `showDistance` | int | `32` | 客户端上报给服务端的最大渲染距离（半径，方块） |
| `maxShowRender` | int | `128` | 最多同时渲染的飘字数量，超出则丢弃最旧的 |
| `minValueDisplay` | double | `0.5` | 伤害/治疗数值绝对值小于此值不渲染 |
| `enableCombineString` | bool | `true` | 是否启用飘字合并 |
| `mergeMaxAge` | double | `40.0` | 跨 tick 合并：仅合并生成时间在此 tick 数以内的飘字 |
| `damageStringLife` | int | `30` | 飘字存活时间（tick，范围 5–600） |
| `showHealNumbers` | bool | `true` | 是否显示治疗数字 |

---

## 颜色映射

伤害类型 → 颜色的映射存放在配置目录下的 `damagerender-damage-color.json`，键为伤害类型 key，值为 `"#RRGGBB"`。
首次运行时模组会自动生成一份默认映射（火、岩浆、魔法、凋零、治疗等）。

**默认映射示例**：

```json
{
    "magic": "#8A2BE2",
    "lightningBolt": "#FFFF00",
    "lava": "#FF0000",
    "indirectMagic": "#8A2BE2",
    "freeze": "#00FFFF",
    "witherSkull": "#27004B",
    "inFire": "#FF0000",
    "onFire": "#FF0000",
    "wither": "#27004B",
    "heal": "#00FF00"
}
```

### 类型 key 的取值规则

- 正常伤害：用伤害类型的注册表 key，如 `minecraft:in_fire`（即 `DamageSource#typeHolder()` 的 location）。
- 治疗（heal）：不是真实注册的 DamageType，走 `fallbackKey` 旁路，key 为 `heal`。

颜色查找时优先匹配完整 key（`damageTypeKey()`，含命名空间），未命中再回退到 path 部分（`msgId()`），最后用默认色 `#FF5533`。

### 运行时管理

颜色映射是自由格式 JSON，无法用 `ModConfigSpec` 表达，所有操作通过命令完成：

| 命令 | 说明 |
|---|---|
| `/damagerender setDamageColor set <damageType> <color>` | 添加/覆盖一个伤害类型的颜色，`<damageType>` 支持 Tab 补全（含 `:` 的注册表 key 如 `minecraft:in_fire`），`<color>` 支持 `#RRGGBB`、命名色、十进制 |
| `/damagerender setDamageColor remove <damageType>` | 删除一个伤害类型的颜色（恢复为默认/兜底） |
| `/damagerender setDamageColor list` | 打印当前所有颜色映射 |
| `/damagerender setDamageColor reload` | 从配置文件整体重载（覆盖运行时修改） |

`set` 和 `remove` 操作立即生效并持久化到 `damagerender-damage-color.json`，重启 / `reload` 后仍有效。

---

## 客户端命令

所有命令根节点为 `/damagerender`。不带参数为读取当前值，带参数为设置：

| 子命令 | 参数 | 说明 |
|---|---|---|
| `minValueDisplay [value]` | double ≥ 0 | 读取 / 设置最小显示阈值 |
| `maxShowRender [value]` | int ≥ 0 | 读取 / 设置最大渲染数量 |
| `enableCombineString [value]` | bool | 读取 / 设置是否启用合并 |
| `mergeMaxAge [value]` | double ≥ 0 | 读取 / 设置合并年龄上限 |
| `damageStringLife [value]` | int 5–600 | 读取 / 设置飘字存活时间 |
| `showHealNumbers [value]` | bool | 读取 / 设置是否显示治疗数字 |
| `setDamageColor set <type> <color>` | type(含补全)、color | 添加/覆盖伤害类型颜色 |
| `setDamageColor remove <type>` | type(含补全) | 删除伤害类型颜色 |
| `setDamageColor list` | — | 列出当前颜色映射 |
| `setDamageColor reload` | — | 从文件整体重载颜色映射 |

示例：

```
/damagerender maxShowRender 256
/damagerender showHealNumbers false
/damagerender setDamageColor set minecraft:in_fire #FF8800
/damagerender setDamageColor remove magic
/damagerender setDamageColor list
/damagerender setDamageColor reload
```

---

## 网络协议

通过 Forge `SimpleChannel`（`damagerender:main`，协议版本 `1`）注册三类消息：

| id | 消息 | 方向 | 内容 |
|---|---|---|---|
| 0 | `DamageInfoPacket` | S→C | 单条伤害信息（兜底 / 兼容） |
| 1 | `UpdateConfigPacket` | C→S | 客户端上报可见距离 |
| 2 | `DamageInfoBatchPacket` | S→C | 一个 tick 内对该玩家可见的伤害合包 |

`DamageInfoData` 字段：`entityId`、`damageTypeLocation`(可空)、`fallbackKey`(可空)、`pos`、`amount`。
正常伤害发 `damageTypeLocation`（注册表 key，稳定），治疗走 `fallbackKey="heal"`。

---

## 构建

```bash
# Windows
./gradlew build
# 产物：build/libs/<mod_name>-1.20.1-Forge-<version>.jar
```

### 开发环境

```bash
# IntelliJ IDEA
./gradlew genIntellijRuns   # 生成运行配置

# Eclipse
./gradlew genEclipseRuns
```

映射使用 Parchment（`parchment` channel, `2023.09.03-1.20.1`）。

### 依赖（开发期）

- Minecraft Forge 1.20.1-47.3.29
- CurseForge（编译期 `implementation`）：Modern UI、JEI、IBE Editor

> `gradle.properties` 中默认配置了本地代理（`127.0.0.1:7897`），如不需要请注释掉 `systemProp.http(s).proxy*` 行。

---

## 目录结构

```
src/main/java/net/yiran/damagerender/
├── DamageRender.java              # @Mod 主类
├── ClientConfig.java             # Forge 配置
├── client/
│   ├── ClientDamageInfoManager.java   # 飘字列表 + mergeIndex
│   ├── DamageColorManager.java        # 颜色映射管理（JSON 编解码 / 持久化 / 查询 / 修改）
│   ├── ClientEventHandler.java        # 渲染入口
│   ├── DamageString.java              # 单个飘字
│   └── Command.java                   # 客户端命令
├── server/
│   ├── ServerDamageInfoManager.java   # 服务端缓冲 + 合批发包
│   └── ServerEventHandler.java        # 伤害/治疗/tick 事件
└── data/
    ├── DamageInfoData.java            # 单条伤害 record
    ├── DamageInfoPacket.java          # 单条包
    ├── DamageInfoBatchPacket.java     # 合包
    └── UpdateConfigPacket.java        # 距离上报包
```
