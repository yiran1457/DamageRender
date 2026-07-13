//? if forge {
//? if =1.19.2 {
/*package net.yiran.damagerender.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

 // 伤害类型 → 颜色映射的统一管理器（客户端单例）。
 //
 // <p>持有内存映射 {@link #map}（fastutil open-addressing，比 HashMap 快 ~20-30%）、
 // 默认颜色表、JSON 编解码器、以及文件持久化读写。所有颜色映射相关操作都集中在此类，避免逻辑分散。
 //
 // <h3>查找规则</h3>
 // {@link #getColor(String)} 按以下顺序查找：
 // <ol>
 //   <li>{@link #map} 中精确匹配 typeKey</li>
 //   <li>{@link #DEFAULT_COLOR} — 最终兜底（#FF5533）</li>
 // </ol>
 //
public class DamageColorManager {

    public static final Codec<Map<String, TextColor>> CODEC =
            Codec.unboundedMap(Codec.STRING, TextColor.CODEC);
    public static final TextColor DEFAULT_COLOR = TextColor.fromRgb(0xFF5533);

    private static final DamageColorManager INSTANCE = new DamageColorManager();

    private static final Path COLOR_FILE_PATH =
            FMLPaths.CONFIGDIR.get().resolve("damagerender-damage-color.json");

         // 默认伤害类型 → 十六进制颜色字符串（首次生成配置文件 / reload 兜底复用）。
     //
    private static final Map<String, String> DEFAULT_DAMAGE_COLORS = Map.ofEntries(
            Map.entry("magic",        toHex(-7722014)),
            Map.entry("lightningBolt",toHex(-256)),
            Map.entry("lava",         toHex(-65536)),
            Map.entry("indirectMagic",toHex(-7722014)),
            Map.entry("freeze",       toHex(-16711681)),
            Map.entry("witherSkull",  toHex(-14221237)),
            Map.entry("inFire",       toHex(-65536)),
            Map.entry("onFire",       toHex(-65536)),
            Map.entry("wither",       toHex(-14221237)),
            Map.entry("heal",         "#00FF00")
    );

     // 当前颜色映射（open addressing，比 HashMap chaining 快 ~20-30%）。 
    private final Object2ObjectOpenHashMap<String, TextColor> map = new Object2ObjectOpenHashMap<>();

    // ---- singleton ----------------------------------------------------------------

    public static DamageColorManager getInstance() {
        return INSTANCE;
    }

    private DamageColorManager() {}

    // ---- 工具 --------------------------------------------------------------------

     // 把 ARGB int 转为 {@code #RRGGBB} 大写十六进制字符串（忽略 alpha 通道）。 
    static String toHex(int color) {
        return "#" + Integer.toHexString(color).substring(2).toUpperCase();
    }

    // ---- 持久化 ------------------------------------------------------------------

         // 从配置文件加载颜色映射：文件存在则解析应用，否则写入默认配置并应用。
     // 供 {@code DamageRender} 构造与 {@link #reload()} 复用。
     //
    public void load() {
        if (Files.exists(COLOR_FILE_PATH)) {
            try {
                apply(JsonParser.parseString(Files.readString(COLOR_FILE_PATH)));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        // 首次运行：写入默认映射
        JsonObject json = defaultColorJson();
        try (Writer fileWriter = Files.newBufferedWriter(COLOR_FILE_PATH)) {
            JsonWriter jsonWriter = new JsonWriter(fileWriter);
            jsonWriter.setIndent("\t");
            jsonWriter.setSerializeNulls(true);
            jsonWriter.setLenient(true);
            Streams.write(json, jsonWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
        apply(json);
    }

     // 重新加载（Command reload 入口）。 
    public void reload() {
        load();
    }

         // 把当前映射持久化到配置文件。set / remove 后自动调用。
     // 编码失败时不写文件，仅打印错误。
     //
    public void save() {
        var result = CODEC.encodeStart(JsonOps.INSTANCE, map);
        var opt = result.result();
        if (opt.isEmpty()) {
            System.err.println("[DamageColorManager] 无法序列化颜色映射，跳过保存");
            return;
        }
        JsonElement element = opt.get();
        JsonObject json = element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        try (Writer fileWriter = Files.newBufferedWriter(COLOR_FILE_PATH)) {
            JsonWriter jsonWriter = new JsonWriter(fileWriter);
            jsonWriter.setIndent("\t");
            jsonWriter.setSerializeNulls(true);
            jsonWriter.setLenient(true);
            Streams.write(json, jsonWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---- JSON / 解析 -------------------------------------------------------------

     // 构造默认颜色映射 JSON（键为伤害类型，值为 "#RRGGBB"）。 
    public static JsonObject defaultColorJson() {
        JsonObject json = new JsonObject();
        DEFAULT_DAMAGE_COLORS.forEach(json::addProperty);
        return json;
    }

         // 把颜色映射 JSON 解析并应用到当前 map。解析失败时不抛异常，保留旧映射。
     //
     // @return 是否解析成功
     //
    public boolean apply(JsonElement json) {
        if (json == null) return false;
        var parsed = CODEC.parse(JsonOps.INSTANCE, json).result();
        if (parsed.isEmpty()) return false;
        replaceAll(parsed.get());
        return true;
    }

    // ---- 查询 --------------------------------------------------------------------

         // 根据伤害类型标识查找对应颜色。
     //
     // @param typeKey 伤害类型标识（msgId），如 "inFire"、"mob"、"heal"
     // @return 映射的颜色，未找到时返回 {@link #DEFAULT_COLOR}
     //
    public TextColor getColor(String typeKey) {
        TextColor color = map.get(typeKey);
        return color != null ? color : DEFAULT_COLOR;
    }

     // 返回当前颜色映射表（可变，用于遍历 / 展示）。 
    public Object2ObjectOpenHashMap<String, TextColor> getMap() {
        return map;
    }

    // ---- 修改 --------------------------------------------------------------------

     // 整体替换映射表（不自动落盘，由 load / reload 调用）。 
    public void replaceAll(Map<String, TextColor> source) {
        map.clear();
        map.putAll(source);
    }

     // 添加/覆盖一个类型颜色，并持久化。 
    public void put(String key, TextColor color) {
        map.put(key, color);
        save();
    }

         // 移除一个类型颜色（恢复为默认/兜底），并持久化。
     //
     // @return 移除前该键是否存在
     //
    public boolean remove(String key) {
        boolean existed = map.remove(key) != null;
        if (existed) save();
        return existed;
    }
}
*///?} else {
package net.yiran.damagerender.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

 // 伤害类型 → 颜色映射的统一管理器（客户端单例）。
 // 
 // <p>持有内存映射 {@link #map}（fastutil open-addressing，比 HashMap 快 ~20-30%）、
 // 默认颜色表、JSON 编解码器、以及文件持久化读写。所有颜色映射相关操作都集中在此类，避免逻辑分散。
 // 
 // <h3>查找规则</h3>
 // {@link #getColor(String)} 按以下顺序查找：
 // <ol>
 //   <li>{@link #map} 中精确匹配 typeKey</li>
 //   <li>{@link #DEFAULT_COLOR} — 最终兜底（#FF5533）</li>
 // </ol>
 //
public class DamageColorManager {

    public static final Codec<Map<String, TextColor>> CODEC =
            Codec.unboundedMap(Codec.STRING, TextColor.CODEC);
    public static final TextColor DEFAULT_COLOR = TextColor.fromRgb(0xFF5533);

    private static final DamageColorManager INSTANCE = new DamageColorManager();

    private static final Path COLOR_FILE_PATH =
            FMLPaths.CONFIGDIR.get().resolve("damagerender-damage-color.json");

         // 默认伤害类型 → 十六进制颜色字符串（首次生成配置文件 / reload 兜底复用）。
     //
    private static final Map<String, String> DEFAULT_DAMAGE_COLORS = Map.ofEntries(
            Map.entry("magic",        toHex(-7722014)),
            Map.entry("lightningBolt",toHex(-256)),
            Map.entry("lava",         toHex(-65536)),
            Map.entry("indirectMagic",toHex(-7722014)),
            Map.entry("freeze",       toHex(-16711681)),
            Map.entry("witherSkull",  toHex(-14221237)),
            Map.entry("inFire",       toHex(-65536)),
            Map.entry("onFire",       toHex(-65536)),
            Map.entry("wither",       toHex(-14221237)),
            Map.entry("heal",         "#00FF00")
    );

     // 当前颜色映射（open addressing，比 HashMap chaining 快 ~20-30%）。 
    private final Object2ObjectOpenHashMap<String, TextColor> map = new Object2ObjectOpenHashMap<>();

    // ---- singleton ----------------------------------------------------------------

    public static DamageColorManager getInstance() {
        return INSTANCE;
    }

    private DamageColorManager() {}

    // ---- 工具 --------------------------------------------------------------------

     // 把 ARGB int 转为 {@code #RRGGBB} 大写十六进制字符串（忽略 alpha 通道）。 
    static String toHex(int color) {
        return "#" + Integer.toHexString(color).substring(2).toUpperCase();
    }

    // ---- 持久化 ------------------------------------------------------------------

         // 从配置文件加载颜色映射：文件存在则解析应用，否则写入默认配置并应用。
     // 供 {@code DamageRender} 构造与 {@link #reload()} 复用。
     //
    public void load() {
        if (Files.exists(COLOR_FILE_PATH)) {
            try {
                apply(JsonParser.parseString(Files.readString(COLOR_FILE_PATH)));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        // 首次运行：写入默认映射
        JsonObject json = defaultColorJson();
        try (Writer fileWriter = Files.newBufferedWriter(COLOR_FILE_PATH)) {
            JsonWriter jsonWriter = new JsonWriter(fileWriter);
            jsonWriter.setIndent("\t");
            jsonWriter.setSerializeNulls(true);
            jsonWriter.setLenient(true);
            Streams.write(json, jsonWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
        apply(json);
    }

     // 重新加载（Command reload 入口）。 
    public void reload() {
        load();
    }

         // 把当前映射持久化到配置文件。set / remove 后自动调用。
     // 编码失败时不写文件，仅打印错误。
     //
    public void save() {
        var result = CODEC.encodeStart(JsonOps.INSTANCE, map);
        var opt = result.result();
        if (opt.isEmpty()) {
            System.err.println("[DamageColorManager] 无法序列化颜色映射，跳过保存");
            return;
        }
        JsonElement element = opt.get();
        JsonObject json = element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        try (Writer fileWriter = Files.newBufferedWriter(COLOR_FILE_PATH)) {
            JsonWriter jsonWriter = new JsonWriter(fileWriter);
            jsonWriter.setIndent("\t");
            jsonWriter.setSerializeNulls(true);
            jsonWriter.setLenient(true);
            Streams.write(json, jsonWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---- JSON / 解析 -------------------------------------------------------------

     // 构造默认颜色映射 JSON（键为伤害类型，值为 "#RRGGBB"）。 
    public static JsonObject defaultColorJson() {
        JsonObject json = new JsonObject();
        DEFAULT_DAMAGE_COLORS.forEach(json::addProperty);
        return json;
    }

         // 把颜色映射 JSON 解析并应用到当前 map。解析失败时不抛异常，保留旧映射。
     // 
     // @return 是否解析成功
     //
    public boolean apply(JsonElement json) {
        if (json == null) return false;
        var parsed = CODEC.parse(JsonOps.INSTANCE, json).result();
        if (parsed.isEmpty()) return false;
        replaceAll(parsed.get());
        return true;
    }

    // ---- 查询 --------------------------------------------------------------------

         // 根据伤害类型标识查找对应颜色。
     // 
     // @param typeKey 伤害类型标识（damageTypeKey 或 msgId），如 "minecraft:in_fire"、"inFire"、"heal"
     // @return 映射的颜色，未找到时返回 {@link #DEFAULT_COLOR}
     //
    public TextColor getColor(String typeKey) {
        TextColor color = map.get(typeKey);
        return color != null ? color : DEFAULT_COLOR;
    }

     // 返回当前颜色映射表（可变，用于遍历 / 展示）。 
    public Object2ObjectOpenHashMap<String, TextColor> getMap() {
        return map;
    }

    // ---- 修改 --------------------------------------------------------------------

     // 整体替换映射表（不自动落盘，由 load / reload 调用）。 
    public void replaceAll(Map<String, TextColor> source) {
        map.clear();
        map.putAll(source);
    }

     // 添加/覆盖一个类型颜色，并持久化。 
    public void put(String key, TextColor color) {
        map.put(key, color);
        save();
    }

         // 移除一个类型颜色（恢复为默认/兜底），并持久化。
     // 
     // @return 移除前该键是否存在
     //
    public boolean remove(String key) {
        boolean existed = map.remove(key) != null;
        if (existed) save();
        return existed;
    }
}
//?}
//?}