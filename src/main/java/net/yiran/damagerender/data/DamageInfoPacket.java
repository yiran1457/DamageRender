package net.yiran.damagerender.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.yiran.damagerender.client.ClientDamageInfoManager;
import net.yiran.damagerender.client.DamageString;
import net.yiran.damagerender.ClientConfig;
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
        if (amount < ClientConfig.MIN_VALUE_DISPLAY.get()) return; 
        var vec3 = this.data.pos();
        DamageString damageString = new DamageString((float) vec3.x, (float) vec3.y, (float) vec3.z,
                (float) amount,
                ClientDamageInfoManager.instance.getColor(this.data).getValue()
        );
        ClientDamageInfoManager.instance.add(damageString);

    }
}
