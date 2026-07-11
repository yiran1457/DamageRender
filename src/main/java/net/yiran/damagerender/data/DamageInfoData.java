package net.yiran.damagerender.data;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 一次伤害/治疗的信息，服务端发往客户端用于渲染飘字。
 *
 * <p>正常伤害传 {@link #damageTypeLocation}（来自 {@link net.minecraft.world.damagesource.DamageSource#typeHolder()}
 * 的 registry key），网络上发 {@link ResourceLocation}（注册表 key，稳定）；
 * {@link #fallbackKey} 为 null。
 *
 * <p>治疗（heal）不是真实注册的 DamageType，没有合法 location，因此走 {@link #fallbackKey} 旁路
 * （如 "heal"），此时 {@link #damageTypeLocation} 为 null。
 *
 * <p>{@link #entityId} 为受击/治疗实体的网络 id，客户端据此判断飘字是否可合并到同一实体。
 */
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

    /**
     * 颜色映射/合并用的 type key：优先取 fallbackKey（heal），否则取 damage_type 的 registry key 字符串
     * （如 "minecraft:in_fire"）。
     */
    public String damageTypeKey() {
        if (fallbackKey != null) return fallbackKey;
        return damageTypeLocation != null ? damageTypeLocation.toString() : "unknown";
    }

    /**
     * 伤害类型的 msgId 兜底（1.20.1 网络上不再传 Holder，无法从 location 精确反查原 getMsgId()）。
     * heal 走 fallbackKey；正常伤害用 location 的 path 作为兜底，与 {@link #damageTypeKey()} 互补。
     */
    public String msgId() {
        if (fallbackKey != null) return fallbackKey;
        return damageTypeLocation != null ? Minecraft.getInstance().getConnection().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).get(damageTypeLocation).msgId() : "unknown";
    }
}
