//? if forge {
//? if =1.19.2 {
/*package net.yiran.damagerender.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

 // 1.19.2 使用伤害类型的 msgId；治疗固定为 "heal"。
public record DamageInfoData(int entityId, String typeKey, Vec3 pos, double amount) {

    public static void toBytes(DamageInfoData data, FriendlyByteBuf buf) {
        writeType(data, buf);
        writePayload(data, buf);
    }

    public static DamageInfoData fromBytes(FriendlyByteBuf buf) {
        TypeData type = readType(buf);
        return readPayload(buf, buf.readVarInt(), type);
    }

    static void writeType(DamageInfoData data, FriendlyByteBuf buf) {
        buf.writeUtf(data.typeKey);
    }

    static TypeData readType(FriendlyByteBuf buf) {
        return new TypeData(buf.readUtf());
    }

    static Object typeToken(DamageInfoData data) {
        return data.typeKey;
    }

    static void writePayload(DamageInfoData data, FriendlyByteBuf buf) {
        buf.writeVarInt(data.entityId);
        buf.writeFloat((float) data.pos.x);
        buf.writeFloat((float) data.pos.y);
        buf.writeFloat((float) data.pos.z);
        buf.writeFloat((float) data.amount);
    }

    static DamageInfoData readPayload(FriendlyByteBuf buf, int entityId, TypeData type) {
        Vec3 pos = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        return new DamageInfoData(entityId, type.typeKey, pos, buf.readFloat());
    }

    static record TypeData(String typeKey) {}

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
        writeType(data, buf);
        writePayload(data, buf);
    }

    public static DamageInfoData fromBytes(FriendlyByteBuf buf) {
        TypeData type = readType(buf);
        return readPayload(buf, buf.readVarInt(), type);
    }

    static void writeType(DamageInfoData data, FriendlyByteBuf buf) {
        buf.writeNullable(data.damageTypeLocation, FriendlyByteBuf::writeResourceLocation);
        buf.writeNullable(data.fallbackKey, FriendlyByteBuf::writeUtf);
    }

    static TypeData readType(FriendlyByteBuf buf) {
        return new TypeData(
                buf.readNullable(FriendlyByteBuf::readResourceLocation),
                buf.readNullable(FriendlyByteBuf::readUtf)
        );
    }

    static Object typeToken(DamageInfoData data) {
        return data.fallbackKey != null ? data.fallbackKey : data.damageTypeLocation;
    }

    static void writePayload(DamageInfoData data, FriendlyByteBuf buf) {
        buf.writeVarInt(data.entityId);
        buf.writeFloat((float) data.pos.x);
        buf.writeFloat((float) data.pos.y);
        buf.writeFloat((float) data.pos.z);
        buf.writeFloat((float) data.amount);
    }

    static DamageInfoData readPayload(FriendlyByteBuf buf, int entityId, TypeData type) {
        Vec3 pos = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        return new DamageInfoData(entityId, type.damageTypeLocation, type.fallbackKey, pos, buf.readFloat());
    }

    static record TypeData(@Nullable ResourceLocation damageTypeLocation, @Nullable String fallbackKey) {}

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
            ByteBufCodecs.FLOAT,
            vec -> (float) vec.x,
            ByteBufCodecs.FLOAT,
            vec -> (float) vec.y,
            ByteBufCodecs.FLOAT,
            vec -> (float) vec.z,
            (x, y, z) -> new Vec3(x, y, z)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DamageInfoData> STREAM_CODEC = StreamCodec.of(
            DamageInfoData::write,
            DamageInfoData::read
    );

    private static void write(RegistryFriendlyByteBuf buf, DamageInfoData data) {
        writeType(data, buf);
        writePayload(data, buf);
    }

    private static DamageInfoData read(RegistryFriendlyByteBuf buf) {
        TypeData type = readType(buf);
        return readPayload(buf, buf.readVarInt(), type);
    }

    static void writeType(DamageInfoData data, RegistryFriendlyByteBuf buf) {
        FriendlyByteBuf.writeNullable(buf, data.typeHolder, DamageType.STREAM_CODEC);
        FriendlyByteBuf.writeNullable(buf, data.fallbackKey, ByteBufCodecs.STRING_UTF8);
    }

    static TypeData readType(RegistryFriendlyByteBuf buf) {
        return new TypeData(
                FriendlyByteBuf.readNullable(buf, DamageType.STREAM_CODEC),
                FriendlyByteBuf.readNullable(buf, ByteBufCodecs.STRING_UTF8)
        );
    }

    static Object typeToken(DamageInfoData data) {
        return data.fallbackKey != null ? data.fallbackKey : data.typeHolder;
    }

    static void writePayload(DamageInfoData data, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(data.entityId);
        VEC_3_STREAM_CODEC.encode(buf, data.pos);
        buf.writeFloat((float) data.amount);
    }

    static DamageInfoData readPayload(RegistryFriendlyByteBuf buf, int entityId, TypeData type) {
        return new DamageInfoData(entityId, type.typeHolder, type.fallbackKey,
                VEC_3_STREAM_CODEC.decode(buf), buf.readFloat());
    }

    static record TypeData(@Nullable Holder<DamageType> typeHolder, @Nullable String fallbackKey) {}

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
