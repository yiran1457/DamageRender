//? if forge {
//? if =1.19.2 {
/*package net.yiran.damagerender.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

 // 一次伤害/治疗的信息，服务端发往客户端用于渲染飘字。
 //
 // <p>不再区分 ResourceLocation 与 fallbackKey，统一使用 {@link #typeKey} 字符串。
 // 伤害类型使用 {@code DamageSource.getMsgId()} 的结果（如 "inFire"、"mob"），
 // 治疗使用固定字符串 {@code "heal"}。
 //
 // @param entityId 受击/治疗实体的网络 id，客户端据此判断飘字是否可合并到同一实体
 // @param typeKey  伤害类型标识（msgId），如 "inFire"、"mob"、"heal"
 // @param pos      伤害发生的位置（实体头部高度）
 // @param amount   伤害/治疗数值
 //
public record DamageInfoData(int entityId, String typeKey, Vec3 pos, double amount) {

    public static void toBytes(DamageInfoData data, FriendlyByteBuf buf) {
        buf.writeVarInt(data.entityId);
        buf.writeUtf(data.typeKey);
        buf.writeDouble(data.pos.x);
        buf.writeDouble(data.pos.y);
        buf.writeDouble(data.pos.z);
        buf.writeDouble(data.amount);
    }

    public static DamageInfoData fromBytes(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        String typeKey = buf.readUtf();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double amount = buf.readDouble();
        return new DamageInfoData(entityId, typeKey, pos, amount);
    }

    public String damageTypeKey() {
        return typeKey;
    }

    public String msgId() {
        return typeKey;
    }
}
*///?} else {
package net.yiran.damagerender.data;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

 // 一次伤害/治疗的信息，服务端发往客户端用于渲染飘字。
 // 
 // <p>正常伤害传 {@link #damageTypeLocation}（来自 {@link net.minecraft.world.damagesource.DamageSource#typeHolder()}
 // 的 registry key），网络上发 {@link ResourceLocation}（注册表 key，稳定）；
 // {@link #fallbackKey} 为 null。
 // 
 // <p>治疗（heal）不是真实注册的 DamageType，没有合法 location，因此走 {@link #fallbackKey} 旁路
 // （如 "heal"），此时 {@link #damageTypeLocation} 为 null。
 // 
 // <p>{@link #entityId} 为受击/治疗实体的网络 id，客户端据此判断飘字是否可合并到同一实体。
 //
public record DamageInfoData(int entityId, @Nullable ResourceLocation damageTypeLocation, @Nullable String fallbackKey,
                             Vec3 pos, double amount) {

    public static void toBytes(DamageInfoData data, FriendlyByteBuf buf) {
        buf.writeVarInt(data.entityId);
        buf.writeNullable(data.damageTypeLocation, FriendlyByteBuf::writeResourceLocation);
        buf.writeNullable(data.fallbackKey, FriendlyByteBuf::writeUtf);
        buf.writeDouble(data.pos.x);
        buf.writeDouble(data.pos.y);
        buf.writeDouble(data.pos.z);
        buf.writeDouble(data.amount);
    }

    public static DamageInfoData fromBytes(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        ResourceLocation damageTypeLocation = buf.readNullable(FriendlyByteBuf::readResourceLocation);
        String fallbackKey = buf.readNullable(FriendlyByteBuf::readUtf);
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double amount = buf.readDouble();
        return new DamageInfoData(entityId, damageTypeLocation, fallbackKey, pos, amount);
    }

         // 颜色映射/合并用的 type key：优先取 fallbackKey（heal），否则取 damage_type 的 registry key 字符串
     // （如 "minecraft:in_fire"）。
     //
    public String damageTypeKey() {
        if (fallbackKey != null) return fallbackKey;
        return damageTypeLocation != null ? damageTypeLocation.toString() : "unknown";
    }

         // 伤害类型的 msgId 兜底（1.20.1 网络上不再传 Holder，无法从 location 精确反查原 getMsgId()）。
     // heal 走 fallbackKey；正常伤害用 location 的 path 作为兜底，与 {@link #damageTypeKey()} 互补。
     //
    public String msgId() {
        if (fallbackKey != null) return fallbackKey;
        if (damageTypeLocation == null) return "unknown";

        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return damageTypeLocation.getPath();

        var damageType = connection.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .get(damageTypeLocation);
        return damageType != null ? damageType.msgId() : damageTypeLocation.getPath();
    }
}
//?}
//?} else {
/*package net.yiran.damagerender.data;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

 // 一次伤害/治疗的信息，服务端发往客户端用于渲染飘字。
 //
 // <p>正常伤害传 {@link #typeHolder}（来自 {@link net.minecraft.world.damagesource.DamageSource#typeHolder()}），
 // 网络上只发一个 damage_type registry 的 int id，比发两个字符串省流量；{@link #fallbackKey} 为 null。
 //
 // <p>治疗（heal）不是真实注册的 DamageType，没有合法 Holder，因此走 {@link #fallbackKey} 旁路
 // （如 "heal"），此时 {@link #typeHolder} 为 null。
 //
 // <p>{@link #entityId} 为受击/治疗实体的网络 id，客户端据此判断飘字是否可合并到同一实体。
 //
public record DamageInfoData(int entityId, @Nullable Holder<DamageType> typeHolder, @Nullable String fallbackKey,
                             Vec3 pos, double amount) {

    private static final StreamCodec<ByteBuf, Vec3> VEC_3_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            Vec3::x,
            ByteBufCodecs.DOUBLE,
            Vec3::y,
            ByteBufCodecs.DOUBLE,
            Vec3::z,
            Vec3::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DamageInfoData> STREAM_CODEC = StreamCodec.of(
            DamageInfoData::write,
            DamageInfoData::read
    );

    private static void write(RegistryFriendlyByteBuf buf, DamageInfoData data) {
        buf.writeVarInt(data.entityId);
        FriendlyByteBuf.writeNullable(buf, data.typeHolder, DamageType.STREAM_CODEC);
        FriendlyByteBuf.writeNullable(buf, data.fallbackKey, ByteBufCodecs.STRING_UTF8);
        VEC_3_STREAM_CODEC.encode(buf, data.pos);
        buf.writeDouble(data.amount);
    }

    private static DamageInfoData read(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        Holder<DamageType> typeHolder = FriendlyByteBuf.readNullable(buf, DamageType.STREAM_CODEC);
        String fallbackKey =  FriendlyByteBuf.readNullable(buf,ByteBufCodecs.STRING_UTF8);
        Vec3 pos = VEC_3_STREAM_CODEC.decode(buf);
        double amount = buf.readDouble();
        return new DamageInfoData(entityId, typeHolder, fallbackKey, pos, amount);
    }

         // 颜色映射/合并用的 type key：优先取 fallbackKey（heal），否则取 damage_type 的 registry key 字符串
     // （如 "minecraft:in_fire"），与原 1.20.1 行为一致。
     //
    public String damageTypeKey() {
        if (fallbackKey != null) return fallbackKey;
        return typeHolder != null && typeHolder.unwrapKey().isPresent()
                ? typeHolder.unwrapKey().get().location().toString()
                : "unknown";
    }

         // 伤害类型的 msgId（原 DamageSource.getMsgId()）。正常伤害从 Holder.value().msgId() 取，
     // heal 走 fallbackKey。
     //
    public String msgId() {
        if (fallbackKey != null) return fallbackKey;
        return typeHolder != null ? typeHolder.value().msgId() : "unknown";
    }
}
*///?}
