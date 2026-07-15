//? if forge {
//? if =1.19.2 {
/*package net.yiran.damagerender.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

 // 1.19.2 使用伤害类型的 msgId；治疗固定为 "heal"。
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

 // 服务端发送给客户端的伤害或治疗数据。
 // 正常伤害使用注册表位置；治疗等无注册表类型的事件使用 fallbackKey。
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

    // 用于颜色映射和合并的稳定类型键。
    public String damageTypeKey() {
        if (fallbackKey != null) return fallbackKey;
        return damageTypeLocation != null ? damageTypeLocation.toString() : "unknown";
    }

    /** 尽力恢复伤害类型的 msgId；注册表未就绪时使用资源路径。 */
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

 // NeoForge 使用 DamageType 的 Holder 进行紧凑网络编码；治疗使用 fallbackKey。
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

    // 用于颜色映射和合并的稳定类型键。
    public String damageTypeKey() {
        if (fallbackKey != null) return fallbackKey;
        return typeHolder != null && typeHolder.unwrapKey().isPresent()
                ? ResourceKeyCompat.identifierString(typeHolder.unwrapKey().get())
                : "unknown";
    }

    // 返回伤害类型的显示键；治疗等特殊事件返回 fallbackKey。
    public String msgId() {
        if (fallbackKey != null) return fallbackKey;
        return typeHolder != null ? typeHolder.value().msgId() : "unknown";
    }
}
*///?}
