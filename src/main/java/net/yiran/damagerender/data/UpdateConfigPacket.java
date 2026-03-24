package net.yiran.damagerender.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.yiran.damagerender.server.ServerDamageInfoManager;
import se.mickelus.mutil.network.AbstractPacket;

public class UpdateConfigPacket extends AbstractPacket {
    public int distance;

    public UpdateConfigPacket() {

    }

    public UpdateConfigPacket(int distance) {
        this.distance = distance;
    }

    @Override
    public void toBytes(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeInt(distance);
    }

    @Override
    public void fromBytes(FriendlyByteBuf friendlyByteBuf) {
        this.distance = friendlyByteBuf.readInt();
    }

    @Override
    public void handle(Player player) {
        ServerDamageInfoManager.instance.addDistanceConfig(player.getStringUUID(), distance);
    }
}
