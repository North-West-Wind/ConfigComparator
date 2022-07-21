package ml.northwestwind.configcomparator.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public interface IPacket {
    void handle(NetworkEvent.Context context);

    void encode(FriendlyByteBuf buffer);
    <P extends IPacket> P decode(FriendlyByteBuf buffer);
}
