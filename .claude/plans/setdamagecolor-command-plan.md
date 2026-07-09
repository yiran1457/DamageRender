# Implementation Plan: `/damagerender setDamageColor` command

## Verified API Surface (from build cache MC 1.20.1 sources)

All signatures confirmed in `build/tmp/.cache/expanded/zip_c26ef82ec6ef6a137f51efa8e731224d/`:

- `net.minecraft.core.registries.Registries.DAMAGE_TYPE` : `ResourceKey<Registry<DamageType>>` (line 107)
- `net.minecraft.core.RegistryAccess.registry(ResourceKey<? extends Registry<? extends E>>)` → `Optional<Registry<E>>` (line 28)
- `net.minecraft.core.Registry.registryKeySet()` → `Set<ResourceKey<T>>` (line 122)
- `net.minecraft.resources.ResourceKey.location()` → `ResourceLocation` (line 79)
- `net.minecraft.commands.CommandSourceStack implements SharedSuggestionProvider` (line 43), `registryAccess()` returns `RegistryAccess` (line 359)
- `net.minecraft.commands.SharedSuggestionProvider.suggest(Iterable<String> pStrings, SuggestionsBuilder pBuilder)` → `CompletableFuture<Suggestions>` (line 204). Auto-filters by remaining input prefix (case-insensitive).
- `net.minecraft.network.chat.TextColor.parseColor(String)` → `@Nullable TextColor` (line 83). Handles `#RRGGBB` (hex) and named ChatFormatting colors (e.g. "red", "green", "aqua").
- `TextColor.fromRgb(int)` → `TextColor` (line 78)
- `TextColor.getValue()` → `int` (line 42)
- `TextColor.serialize()` → `String` (line 46): returns name for legacy colors, else `#%06X`
- `TextColor.CODEC` is `Codec<TextColor>` (line 16): comapFlatMap(parseColor) / serialize
- `com.mojang.serialization.Codec.encodeStart(DynamicOps<T>, A)` → `DataResult<T>` (standard DFU)
- `ClientDamageInfoManager.COLOR_CODEC` = `Codec<Map<String, TextColor>>` = `Codec.unboundedMap(Codec.STRING, TextColor.CODEC)`

## Design Decisions

### 1. damageType argument type: `StringArgumentType.string()` (NOT `word()`)

Keys contain `:` (e.g. "minecraft:in_fire") and `word()` forbids `:` in Brigadier.
`StringArgumentType.string()` allows any quoted/unquoted string including colons.
Use `StringArgumentType.getString(ctx, "damageType")`.

### 2. color argument type: `StringArgumentType.string()`

Accepts `#FF5533`, `red`, `16731431`, `-7722014`. `word()` would block `#` prefix.
Use `StringArgumentType.getString(ctx, "color")`.

### 3. Helper methods on ClientDamageInfoManager (recommended)

Add `setColor(String key, TextColor color)` and `removeColor(String key)` to centralize map mutation + trigger save.
This keeps the command builder thin and the save+apply logic in one place.

### 4. SuggestionProvider: gather from three sources, dedup via LinkedHashSet

1. `ctx.getSource().registryAccess().registry(Registries.DAMAGE_TYPE)` → `Optional<Registry<DamageType>>`
   - If present: iterate `registry.registryKeySet()`, map each `ResourceKey.location().toString()` → "minecraft:in_fire"
2. Always add "heal" (not in the damage type registry)
3. Add all keys from `ClientDamageInfoManager.getInstance().getDamageColorMap().keySet()` (covers custom/short keys like "magic", "inFire")
4. Dedup via `LinkedHashSet<String>`

Guard: if `registry` Optional is empty (client not connected / singleplayer not loaded), skip step 1.

### 5. Color parsing helper in Command.java

```
parseColorInput(String input) → TextColor (nullable)
```
- Try `TextColor.parseColor(input)` first (handles `#RRGGBB` and named colors)
- If null: try `Integer.decode(input)` (handles decimal, `0x...`, `#...` as integer) → `TextColor.fromRgb(int)`
- If both fail: return null

Note: `TextColor.parseColor` already handles `#` prefix for hex. `Integer.decode` also handles `0x` prefix. But since `parseColor` returns null for non-hex non-name strings, we fall through to `Integer.decode` for bare decimal integers like `16731431` or `-7722014`.

### 6. saveDamageColorMap() in DamageRender.java

Mirror the existing `loadDamageColorMap()` JSON write style:
- Get the live map from `ClientDamageInfoManager.getInstance().getDamageColorMap()`
- Encode via `COLOR_CODEC.encodeStart(JsonOps.INSTANCE, map)` → `DataResult<JsonElement>`
- `.result()` → `Optional<JsonElement>`; if empty, log and return (don't crash)
- Write via `JsonWriter` with `setIndent("\t")`, `setSerializeNulls(true)`, `setLenient(true)`, `Streams.write(jsonElement, jsonWriter)`
- Use try-with-resources `Files.newBufferedWriter(DAMAGE_COLOR_PATH)`

## Implementation Steps

### Step 1: Add `saveDamageColorMap()` to `DamageRender.java`

Add method after `reloadDamageColorMap()` (around line 105):

```java
/**
 * 将当前颜色映射持久化到 damagerender-damage-color.json。
 * 由 Command set/remove 调用。
 */
public static void saveDamageColorMap() {
    var map = ClientDamageInfoManager.getInstance().getDamageColorMap();
    DataResult<JsonElement> result = ClientDamageInfoManager.COLOR_CODEC.encodeStart(JsonOps.INSTANCE, map);
    Optional<JsonElement> opt = result.result();
    if (opt.isEmpty()) {
        System.err.println("[DamageRender] 无法序列化颜色映射，跳过保存");
        return;
    }
    try (Writer fileWriter = Files.newBufferedWriter(DAMAGE_COLOR_PATH)) {
        JsonWriter jsonWriter = new JsonWriter(fileWriter);
        jsonWriter.setIndent("\t");
        jsonWriter.setSerializeNulls(true);
        jsonWriter.setLenient(true);
        Streams.write(opt.get(), jsonWriter);
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```

**New imports needed in `DamageRender.java`:**
```java
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
```

### Step 2: Add `setColor` / `removeColor` helpers to `ClientDamageInfoManager.java`

Add after `setDamageColorMap(Map)` (around line 156):

```java
/**
 * 设置指定伤害类型的颜色并持久化。
 */
public void setColor(String key, TextColor color) {
    damageColorMap.put(key, color);
    DamageRender.saveDamageColorMap();
}

/**
 * 移除指定伤害类型的颜色（恢复默认/兜底），并持久化。
 * @return 移除前是否存在该键
 */
public boolean removeColor(String key) {
    boolean removed = damageColorMap.remove(key) != null;
    if (removed) {
        DamageRender.saveDamageColorMap();
    }
    return removed;
}
```

**No new imports needed** (TextColor already imported, DamageRender already imported).

### Step 3: Rewrite the `setDamageColor` subcommand builder in `Command.java`

Replace the current `setDamageColor` block (lines 110-120) with an expanded builder that includes `reload`, `<damageType> <color>`, `remove <damageType>`, and `list`.

**New imports needed in `Command.java`:**
```java
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.yiran.damagerender.client.ClientDamageInfoManager;
import net.yiran.damagerender.DamageRender;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
```

Note: `ClientConfig` import already exists. `reloadDamageColorMap` static import already exists.

**SuggestionProvider lambda (static field):**

```java
private static final SuggestionProvider<CommandSourceStack> DAMAGE_TYPE_SUGGESTIONS = (ctx, builder) -> {
    Set<String> candidates = new LinkedHashSet<>();
    // 1. Registered DamageType keys (full "namespace:path" form)
    try {
        RegistryAccess access = ctx.getSource().registryAccess();
        var optReg = access.registry(Registries.DAMAGE_TYPE);
        optReg.ifPresent(reg -> {
            for (ResourceKey<?> key : reg.registryKeySet()) {
                candidates.add(key.location().toString());
            }
        });
    } catch (Exception ignored) {
        // registry not yet available (client not connected)
    }
    // 2. Always include "heal"
    candidates.add("heal");
    // 3. Existing custom/short keys in the color map
    candidates.addAll(ClientDamageInfoManager.getInstance().getDamageColorMap().keySet());
    return SharedSuggestionProvider.suggest(candidates, builder);
};
```

Note: `ctx.getSource()` returns `CommandSourceStack` which implements `SharedSuggestionProvider`, but we call the static `SharedSuggestionProvider.suggest(Iterable<String>, SuggestionsBuilder)`. Need to import `net.minecraft.commands.SharedSuggestionProvider`.

**Color parsing helper (static method):**

```java
/**
 * 解析颜色输入：支持 #RRGGBB、命名颜色（red/green/...）、十进制整数。
 * @return 解析后的 TextColor，失败返回 null
 */
private static TextColor parseColorInput(String input) {
    // 1. #RRGGBB hex 或 命名颜色 (red, green, aqua, ...)
    TextColor color = TextColor.parseColor(input);
    if (color != null) return color;
    // 2. 裸十进制整数（如 16731431 或 -7722014 或 0xFF5533）
    try {
        int rgb = Integer.decode(input);
        return TextColor.fromRgb(rgb);
    } catch (NumberFormatException ignored) {
        return null;
    }
}
```

Note: `Integer.decode` handles `"16731431"`, `"-7722014"`, `"0xFF5533"`. For `#FF5533`, `parseColor` already handles it first. `Integer.decode` does NOT handle bare `#` prefix (it expects `0x`), so the order matters — `parseColor` first.

**Command builder (replacing lines 110-120):**

```java
.then(
        Commands.literal("setDamageColor")
                .then(
                        Commands.literal("reload")
                                .executes(ctx -> {
                                    reloadDamageColorMap();
                                    ctx.getSource().sendSuccess(() -> Component.literal("已重载伤害颜色映射"), false);
                                    return 1;
                                })
                )
                .then(
                        Commands.literal("remove")
                                .then(
                                        Commands.argument("damageType", StringArgumentType.string())
                                                .suggests(DAMAGE_TYPE_SUGGESTIONS)
                                                .executes(ctx -> {
                                                    String key = StringArgumentType.getString(ctx, "damageType");
                                                    boolean removed = ClientDamageInfoManager.getInstance().removeColor(key);
                                                    if (removed) {
                                                        ctx.getSource().sendSuccess(() -> Component.literal("已移除伤害颜色 : " + key), false);
                                                    } else {
                                                        ctx.getSource().sendFailure(Component.literal("未找到伤害类型 : " + key));
                                                    }
                                                    return removed ? 1 : 0;
                                                })
                                )
                )
                .then(
                        Commands.literal("list")
                                .executes(ctx -> {
                                    var map = ClientDamageInfoManager.getInstance().getDamageColorMap();
                                    if (map.isEmpty()) {
                                        ctx.getSource().sendSuccess(() -> Component.literal("当前颜色映射为空"), false);
                                    } else {
                                        map.forEach((k, v) ->
                                                ctx.getSource().sendSuccess(() -> Component.literal(k + " : " + v.serialize()), false)
                                        );
                                        ctx.getSource().sendSuccess(() -> Component.literal("共 " + map.size() + " 项"), false);
                                    }
                                    return 1;
                                })
                )
                .then(
                        Commands.argument("damageType", StringArgumentType.string())
                                .suggests(DAMAGE_TYPE_SUGGESTIONS)
                                .then(
                                        Commands.argument("color", StringArgumentType.string())
                                                .executes(ctx -> {
                                                    String key = StringArgumentType.getString(ctx, "damageType");
                                                    String colorInput = StringArgumentType.getString(ctx, "color");
                                                    TextColor color = parseColorInput(colorInput);
                                                    if (color == null) {
                                                        ctx.getSource().sendFailure(Component.literal("无效颜色 : " + colorInput + "（支持 #RRGGBB、颜色名、十进制整数）"));
                                                        return 0;
                                                    }
                                                    ClientDamageInfoManager.getInstance().setColor(key, color);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("伤害颜色 " + key + " 设置为 : " + color.serialize()), false);
                                                    return 1;
                                                })
                                )
                )
)
```

**Important ordering note:** The `reload`, `remove`, and `list` literal branches are registered BEFORE the `<damageType> <color>` argument branch. This is necessary because Brigadier tries to match literals first, then falls through to arguments. If a user types `/damagerender setDamageColor reload`, the `reload` literal matches. If they type `/damagerender setDamageColor minecraft:in_fire`, no literal matches so it falls through to the `damageType` argument. This is the standard Brigadier pattern and works correctly.

However, there's a subtlety: if a user literally names a damage type "remove", "list", or "reload" — that would conflict. But no vanilla damage type has those names, and custom ones are unlikely. This is an acceptable edge case (same trade-off vanilla commands make).

## Edge Cases and Error Handling

1. **Registry not available (client not connected):** `access.registry(Registries.DAMAGE_TYPE)` returns `Optional.empty()`. The `ifPresent` skips registry keys, so only "heal" + existing map keys are suggested. No crash.

2. **Invalid color input:** `parseColorInput` returns null. Command sends failure message, returns 0, does NOT modify the map or save.

3. **remove on non-existent key:** `removeColor` returns false. Command sends failure message, returns 0. Does NOT save (no change).

4. **Codec encode failure (shouldn't happen but defensive):** `saveDamageColorMap` logs to stderr and returns. Map is still modified in memory; just not persisted. User can retry or use reload to revert.

5. **File I/O failure during save:** `IOException` caught and stack-traced. Map state in memory is still correct.

6. **Setting a key that already exists:** Overwrites. This is the expected behavior (same as `Map.put`).

7. **Key matching:** The color lookup in `getColor()` tries `damageTypeKey()` (full "minecraft:in_fire") first, then `msgId()` (path "in_fire"). So setting a color for "minecraft:in_fire" or "in_fire" both work. The user can use either form. Setting "heal" works because `damageTypeKey()` returns "heal" for healing damage.

## Message Style (Chinese, matching existing pattern)

Existing: `"配置项 X 设置为 : Y"` and `"配置项 X 值为 : Y"`

New messages:
- Set: `"伤害颜色 {key} 设置为 : {color.serialize()}"` 
- Remove success: `"已移除伤害颜色 : {key}"`
- Remove not found: `"未找到伤害类型 : {key}"` (sendFailure)
- Invalid color: `"无效颜色 : {input}（支持 #RRGGBB、颜色名、十进制整数）"` (sendFailure)
- List: `"{key} : {color.serialize()}"` per line, `"共 {size} 项"` summary
- List empty: `"当前颜色映射为空"`
- Reload: `"已重载伤害颜色映射"` (unchanged)

## Dependency / Sequencing

1. **Step 2 (ClientDamageInfoManager helpers) must be done BEFORE Step 3 (Command)** — the command calls `setColor`/`removeColor`.
2. **Step 1 (DamageRender.saveDamageColorMap) must be done BEFORE Step 2** — the helpers call `DamageRender.saveDamageColorMap()`.
3. No circular dependency issue: `DamageRender` already imports `ClientDamageInfoManager` (line 19), and `ClientDamageInfoManager` already imports `DamageRender` (line 12). The new `saveDamageColorMap` calls `ClientDamageInfoManager.getInstance().getDamageColorMap()` — this is fine, `getDamageColorMap` is a simple getter. The helpers on `ClientDamageInfoManager` call `DamageRender.saveDamageColorMap()` — also fine, it's a static method.

## Files to Modify

1. `src/main/java/net/yiran/damagerender/DamageRender.java` — add `saveDamageColorMap()` + 3 imports
2. `src/main/java/net/yiran/damagerender/client/ClientDamageInfoManager.java` — add `setColor()` + `removeColor()` (no new imports)
3. `src/main/java/net/yiran/damagerender/client/Command.java` — add imports, SuggestionProvider field, parseColorInput helper, expand setDamageColor builder

## No Other Files Need Changes

- `DamageInfoData.java` — no changes (key resolution already correct)
- `ClientConfig.java` — no changes (color map is not in ModConfigSpec)
- No new files needed
