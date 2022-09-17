package ml.northwestwind.configcomparator.events;

import com.google.common.collect.Sets;
import ml.northwestwind.configcomparator.ConfigComparator;
import ml.northwestwind.configcomparator.config.Config;
import ml.northwestwind.configcomparator.network.Communicator;
import ml.northwestwind.configcomparator.network.packets.ClientboundFileListPacket;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ConfigComparator.MOD_ID, value = Dist.DEDICATED_SERVER)
public class ServerEvents {
    private static MessageDigest digest = null;
    private static final Set<String> CONFIG_FILES = Sets.newHashSet();
    private static final Set<String> ABSOLUTE_PATHS = Sets.newHashSet();
    private static final Set<UUID> verified = Sets.newHashSet();
    private static String hash = "";
    private static Thread fileChecker = null;

    static {
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ignored) { }
    }

    @SubscribeEvent
    public static void serverStarting(final ServerStartingEvent event) {
        for (String path : Config.getFiles()) {
            File file = FMLPaths.GAMEDIR.get().resolve(path).toFile();
            if (file.isFile()) hashFile(file);
            else if (file.isDirectory()) hashDirectory(file);
        }
        ConfigComparator.LOGGER.info("Server: SHA1 digest of files: {}", hash);
        if (Config.getCheckInterval() > 0) {
            fileChecker = new Thread(() -> {
                while (!Thread.interrupted()) {
                    if (Config.getCheckInterval() <= 0) Thread.currentThread().interrupt();
                    else {
                        try {
                            Thread.sleep(Config.getCheckInterval());
                            verified.clear();
                            Communicator.sendToClient(PacketDistributor.ALL.noArg(), new ClientboundFileListPacket(CONFIG_FILES));
                            new Thread(() -> {
                                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                                if (server == null) return;
                                int acc = 0;
                                try {
                                    while (acc < Config.getTimeout() && !Thread.interrupted()) {
                                        if (verified.containsAll(server.getPlayerList().getPlayers().stream().map(Entity::getUUID).toList())) Thread.currentThread().interrupt();
                                        else {
                                            Thread.sleep(100);
                                            acc += 100;
                                        }
                                    }
                                    for (ServerPlayer player : server.getPlayerList().getPlayers())
                                        if (!verified.contains(player.getUUID())) player.connection.disconnect(MutableComponent.create(new TranslatableContents("configcomparator.timeout.kick")));
                                } catch (InterruptedException ignored) { }
                            }).start();
                        } catch (InterruptedException ignored) { }
                    }
                }
            });
            fileChecker.start();
        }
    }

    @SubscribeEvent
    public static void serverStopping(final ServerStoppingEvent event) {
        hash = "";
        if (fileChecker != null) {
            fileChecker.interrupt();
            fileChecker = null;
        }
    }

    private static void hashFile(File file) {
        if (ABSOLUTE_PATHS.contains(file.getAbsolutePath())) return;
        try (InputStream is = new FileInputStream(file)) {
            // fk windows
            hash = bytesToHex(digest.digest(new String(is.readAllBytes()).replaceAll("\r\n", "\n").getBytes()));
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
        if (hash.isEmpty() || !(event.getEntity() instanceof ServerPlayer player)) return;
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
                if (!verified.contains(player.getUUID())) player.connection.disconnect(MutableComponent.create(new TranslatableContents("configcomparator.timeout.kick")));
            } catch (InterruptedException ignored) { }
        }).start();
    }

    @SubscribeEvent
    public static void playerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (hash.isEmpty() || !(event.getEntity() instanceof ServerPlayer player)) return;
        verified.remove(player.getUUID());
    }

    public static void verify(UUID uuid) {
        verified.add(uuid);
    }
}
