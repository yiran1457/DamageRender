package net.yiran.damagerender.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.client.ClientDamageInfoManager;
import net.yiran.damagerender.client.DamageString;
import se.mickelus.mutil.network.AbstractPacket;

public class DamageInfoPacket extends AbstractPacket {
    public DamageInfoData data;

    public DamageInfoPacket() {

    }

    public DamageInfoPacket(DamageInfoData damageInfoData) {
        this.data = damageInfoData;
    }

    @Override
    public void toBytes(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeJsonWithCodec(DamageInfoData.CODEC, data);
    }

    @Override
    public void fromBytes(FriendlyByteBuf friendlyByteBuf) {
        this.data = friendlyByteBuf.readJsonWithCodec(DamageInfoData.CODEC);
    }

    @Override
    public void handle(Player player) {
        double amount = data.amount();
        if (Math.abs(amount) < ClientConfig.MIN_VALUE_DISPLAY.get()) return;

        var vec3 = this.data.pos();
        int color = ClientDamageInfoManager.getInstance().getColor(this.data).getValue();

        // 确定用于合并的伤害类型键（与 getColor 使用相同的优先级）
        String typeKey = data.damageType() != null ? data.damageType() : data.msgId();

        DamageString damageString = new DamageString(
                (float) vec3.x, (float) vec3.y, (float) vec3.z,
                (float) amount,
                color,
                typeKey
        );
        ClientDamageInfoManager.getInstance().add(damageString);
    }
}
