package ml.northwestwind.configcomparator.events;

import com.google.common.collect.Sets;
import ml.northwestwind.configcomparator.ConfigComparator;
import ml.northwestwind.configcomparator.config.Config;
import ml.northwestwind.configcomparator.network.Communicator;
import ml.northwestwind.configcomparator.network.packets.ClientboundFileListPacket;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = ConfigComparator.MOD_ID, value = Dist.DEDICATED_SERVER)
public class ServerEvents {
    private static MessageDigest digest = null;
    private static final Set<String> CONFIG_FILES = Sets.newHashSet();
    private static final Set<String> ABSOLUTE_PATHS = Sets.newHashSet();
    private static final Set<UUID> verified = Sets.newHashSet();
    private static String hash = "";

    static {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ignored) { }
    }

    @SubscribeEvent
    public static void serverStarting(final ServerStartingEvent event) {
        for (String path : Config.getFiles()) {
            File file = FMLPaths.GAMEDIR.get().resolve(path).toFile();
            if (file.isFile()) hashFile(file);
            else if (file.isDirectory()) hashDirectory(file);
        }
        ConfigComparator.LOGGER.info("Server: SHA256 digest of files: {}", hash);
    }

    @SubscribeEvent
    public static void serverStopping(final ServerStoppingEvent event) {
        hash = "";
    }

    private static void hashFile(File file) {
        if (ABSOLUTE_PATHS.contains(file.getAbsolutePath())) return;
        try (InputStream is = new FileInputStream(file)) {
            hash = bytesToHex(digest.digest(is.readAllBytes()));
            ABSOLUTE_PATHS.add(file.getAbsolutePath());
            CONFIG_FILES.add(file.getAbsolutePath().replace(FMLPaths.GAMEDIR.get().toFile().getAbsolutePath() + File.separator, ""));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void hashDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isFile()) hashFile(file);
            else if (file.isDirectory()) hashDirectory(file);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String getHash() {
        return hash;
    }

    @SubscribeEvent
    public static void playerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (hash.isEmpty() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        Communicator.sendToClient(PacketDistributor.PLAYER.with(() -> player), new ClientboundFileListPacket(CONFIG_FILES));
        new Thread(() -> {
            int acc = 0;
            try {
                while (acc < Config.getTimeout() && !Thread.interrupted()) {
                    if (verified.contains(player.getUUID())) Thread.currentThread().interrupt();
                    else {
                        Thread.sleep(100);
                        acc += 100;
                    }
                }
                if (!verified.contains(player.getUUID())) player.connection.disconnect(new TranslatableComponent("configcomparator.timeout.kick"));
            } catch (InterruptedException ignored) { }
        }).start();
    }

    @SubscribeEvent
    public static void playerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (hash.isEmpty() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        verified.remove(player.getUUID());
    }

    public static void verify(UUID uuid) {
        verified.add(uuid);
    }
}
