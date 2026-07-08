package net.yiran.damagerender.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

public record DamageInfoData(String damageType, String msgId, Vec3 pos, double amount) {
    public static Codec<DamageInfoData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("damageType").forGetter(DamageInfoData::damageType),
            Codec.STRING.fieldOf("msgId").forGetter(DamageInfoData::msgId),
            Vec3.CODEC.fieldOf("pos").forGetter(DamageInfoData::pos),
            Codec.DOUBLE.fieldOf("amount").forGetter(DamageInfoData::amount)
    ).apply(instance, DamageInfoData::new));
}
