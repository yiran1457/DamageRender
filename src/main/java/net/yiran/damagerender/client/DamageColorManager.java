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
//? if forge {
import net.minecraftforge.fml.loading.FMLPaths;
//?} else {
/*import net.neoforged.fml.loading.FMLPaths;
*///?}

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 客户端伤害类型到颜色的映射及其 JSON 持久化。
 * 未配置的类型统一回退到 {@link #DEFAULT_COLOR}。
 */
public class DamageColorManager {

    public static final Codec<Map<String, TextColor>> CODEC =
            Codec.unboundedMap(Codec.STRING, TextColor.CODEC);
    public static final TextColor DEFAULT_COLOR = TextColor.fromRgb(0xFF5533);

    private static final DamageColorManager INSTANCE = new DamageColorManager();

    private static final Path COLOR_FILE_PATH =
            FMLPaths.CONFIGDIR.get().resolve("damagerender-damage-color.json");

    /** 首次创建配置文件时写入的默认颜色。 */
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

    /** 当前生效的颜色映射。 */
    private final Object2ObjectOpenHashMap<String, TextColor> map = new Object2ObjectOpenHashMap<>();

    // ---- 单例 ----------------------------------------------------------------------

    public static DamageColorManager getInstance() {
        return INSTANCE;
    }

    private DamageColorManager() {}

    // ---- 工具 ----------------------------------------------------------------------

    /** 将 ARGB 整数转换为忽略 alpha 的 {@code #RRGGBB} 字符串。 */
    static String toHex(int color) {
        return "#" + Integer.toHexString(color).substring(2).toUpperCase();
    }

    // ---- 持久化 --------------------------------------------------------------------

    /** 加载配置；首次运行时写入并应用默认映射。 */
    public void load() {
        if (Files.exists(COLOR_FILE_PATH)) {
            try {
                apply(JsonParser.parseString(Files.readString(COLOR_FILE_PATH)));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
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

    /** 命令入口：重新读取颜色配置。 */
    public void reload() {
        load();
    }

    /** 将当前映射写回配置文件；编码失败时保留原文件。 */
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

    // ---- JSON 解析 -----------------------------------------------------------------

    /** 构造默认颜色映射的 JSON 表示。 */
    public static JsonObject defaultColorJson() {
        JsonObject json = new JsonObject();
        DEFAULT_DAMAGE_COLORS.forEach(json::addProperty);
        return json;
    }

    /** 解析并应用颜色映射；失败时保留旧映射。 */
    public boolean apply(JsonElement json) {
        if (json == null) return false;
        var parsed = CODEC.parse(JsonOps.INSTANCE, json).result();
        if (parsed.isEmpty()) return false;
        replaceAll(parsed.get());
        return true;
    }

    // ---- 查询 ----------------------------------------------------------------------

    /** 按伤害类型查找颜色，未命中时返回默认颜色。 */
    public TextColor getColor(String typeKey) {
        TextColor color = map.get(typeKey);
        return color != null ? color : DEFAULT_COLOR;
    }

    /** 返回可变映射，供命令展示和修改。 */
    public Object2ObjectOpenHashMap<String, TextColor> getMap() {
        return map;
    }

    // ---- 修改 ----------------------------------------------------------------------

    /** 整体替换内存映射，不自动写入文件。 */
    public void replaceAll(Map<String, TextColor> source) {
        map.clear();
        map.putAll(source);
    }

    /** 添加或覆盖一个类型颜色，并立即保存。 */
    public void put(String key, TextColor color) {
        map.put(key, color);
        save();
    }

    /** 移除一个类型颜色；成功移除时保存配置。 */
    public boolean remove(String key) {
        boolean existed = map.remove(key) != null;
        if (existed) save();
        return existed;
    }
}
