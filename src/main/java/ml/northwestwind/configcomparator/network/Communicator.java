package ml.northwestwind.configcomparator.network;

import ml.northwestwind.configcomparator.ConfigComparator;
import ml.northwestwind.configcomparator.network.packets.ClientboundFileListPacket;
import ml.northwestwind.configcomparator.network.packets.ServerboundHashPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public class Communicator {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel MAIN = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ConfigComparator.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int index = 0;

    public static void registerPackets() {
        registerPacket(ClientboundFileListPacket.class, ClientboundFileListPacket::new, NetworkDirection.PLAY_TO_CLIENT);
        registerPacket(ServerboundHashPacket.class, ServerboundHashPacket::new, NetworkDirection.PLAY_TO_SERVER);
    }

    private static <MSG extends IPacket> void registerPacket(Class<MSG> packet, Supplier<MSG> constructor) {
        registerPacket(packet, constructor, null);
    }

    private static <MSG extends IPacket> void registerPacket(Class<MSG> packet, Supplier<MSG> constructor, NetworkDirection direction) {
        MAIN.registerMessage(++index, packet, IPacket::encode, friendlyByteBuf -> constructor.get().decode(friendlyByteBuf), (p, contextSupplier) -> {
            contextSupplier.get().enqueueWork(() -> p.handle(contextSupplier.get()));
            contextSupplier.get().setPacketHandled(true);
        }, direction == null ? Optional.empty() : Optional.of(direction));
    }

    public static <MSG> void sendToClient(PacketDistributor.PacketTarget target, MSG message) {
        MAIN.send(target, message);
    }

    public static <MSG> void sendToServer(MSG message) {
        MAIN.sendToServer(message);
    }
}
