package net.yiran.damagerender.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * 一次伤害/治疗的信息，服务端发往客户端用于渲染飘字。
 *
 * <p>不再区分 ResourceLocation 与 fallbackKey，统一使用 {@link #typeKey} 字符串。
 * 伤害类型使用 {@code DamageSource.getMsgId()} 的结果（如 "inFire"、"mob"），
 * 治疗使用固定字符串 {@code "heal"}。
 *
 * @param entityId 受击/治疗实体的网络 id，客户端据此判断飘字是否可合并到同一实体
 * @param typeKey  伤害类型标识（msgId），如 "inFire"、"mob"、"heal"
 * @param pos      伤害发生的位置（实体头部高度）
 * @param amount   伤害/治疗数值
 */
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
}