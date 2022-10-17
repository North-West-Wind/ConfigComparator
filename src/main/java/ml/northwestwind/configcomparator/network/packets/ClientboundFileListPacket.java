package ml.northwestwind.configcomparator.network.packets;

import com.google.common.collect.Sets;
import ml.northwestwind.configcomparator.ConfigComparator;
import ml.northwestwind.configcomparator.config.Config;
import ml.northwestwind.configcomparator.network.Communicator;
import ml.northwestwind.configcomparator.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkEvent;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;

public class ClientboundFileListPacket implements IPacket {
    private MessageDigest digest = null;
    private final Set<String> configFiles = Sets.newHashSet();
    private Config.HashAlgorithm algorithm;

    public ClientboundFileListPacket(Set<String> configFiles, Config.HashAlgorithm algorithm) {
        this.configFiles.addAll(configFiles);
        this.algorithm = algorithm;
    }

    public ClientboundFileListPacket() {
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        try {
            digest = MessageDigest.getInstance(algorithm.getString());
        } catch (NoSuchAlgorithmException ignored) { }
        for (String path : configFiles) {
            File file = FMLPaths.GAMEDIR.get().resolve(path).toFile();
            if (file.isFile()) hashFile(file);
            else if (file.isDirectory()) hashDirectory(file);
        }
        String hash = bytesToHex(digest.digest());
        ConfigComparator.LOGGER.debug("Client: {} digest of files {}", algorithm.getString(), hash);
        Communicator.sendToServer(new ServerboundHashPacket(hash));
    }

    private void hashFile(File file) {
        try {
            // fk windows
            String base64;
            if (ConfigComparator.IS_WINDOWS) base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
            else {
                String str = new String(Files.readAllBytes(file.toPath()));
                str = str.replaceAll("\n", "\r\n");
                base64 = Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
            }
            digest.update(base64.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void hashDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isFile()) hashFile(file);
            else if (file.isDirectory()) hashDirectory(file);
        }
    }

    private static String bytesToHex(byte[] hash) {
        return new BigInteger(1, hash).toString(16);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(configFiles.size());
        for (String file : configFiles) buffer.writeUtf(file);
        buffer.writeUtf(algorithm.getString());
    }

    @Override
    public <P extends IPacket> P decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        for (int ii = 0; ii < size; ii++) this.configFiles.add(buffer.readUtf());
        String alg = buffer.readUtf();
        this.algorithm = Arrays.stream(Config.HashAlgorithm.values()).filter(al -> al.getString().equals(alg)).findFirst().orElse(Config.HashAlgorithm.MD5);
        return (P) this;
    }
}
