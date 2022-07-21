package ml.northwestwind.configcomparator.network.packets;

import ml.northwestwind.configcomparator.ConfigComparator;
import ml.northwestwind.configcomparator.config.Config;
import ml.northwestwind.configcomparator.events.ServerEvents;
import ml.northwestwind.configcomparator.network.IPacket;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.network.NetworkEvent;

public class ServerboundHashPacket implements IPacket {
    private String hash;

    public ServerboundHashPacket(String hash) {
        this.hash = hash;
    }

    public ServerboundHashPacket() {

    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        if (!this.hash.equals(ServerEvents.getHash())) {
            switch (Config.getAction()) {
                case KICK -> player.connection.disconnect(new TranslatableComponent("configcomparator.files_mismatch.kick"));
                case WARN_ADMIN -> {
                    PlayerList list = player.getServer().getPlayerList();
                    for (String name : list.getOpNames()) {
                        ServerPlayer p = list.getPlayerByName(name);
                        p.sendMessage(new TranslatableComponent("configcomparator.files_mismatch.warn", player.getDisplayName()), ChatType.SYSTEM, Util.NIL_UUID);
                    }
                }
                case LOG -> ConfigComparator.LOGGER.info(new TranslatableComponent("configcomparator.files_mismatch.warn", player.getDisplayName()).getString());
            }
        } else ServerEvents.verify(player.getUUID());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(hash);
    }

    @Override
    public <P extends IPacket> P decode(FriendlyByteBuf buffer) {
        this.hash = buffer.readUtf();
        return (P) this;
    }
}
