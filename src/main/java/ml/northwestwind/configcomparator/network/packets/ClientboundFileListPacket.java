package ml.northwestwind.configcomparator.network.packets;

import com.google.common.collect.Sets;
import ml.northwestwind.configcomparator.ConfigComparator;
import ml.northwestwind.configcomparator.network.Communicator;
import ml.northwestwind.configcomparator.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

public class ClientboundFileListPacket implements IPacket {
    private static MessageDigest digest = null;
    private final Set<String> configFiles = Sets.newHashSet();

    static {
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ignored) { }
    }

    public ClientboundFileListPacket(Set<String> configFiles) {
        this.configFiles.addAll(configFiles);
    }

    public ClientboundFileListPacket() {

    }

    @Override
    public void handle(NetworkEvent.Context context) {
        String hash = "";
        for (String path : configFiles) {
            File file = FMLPaths.GAMEDIR.get().resolve(path).toFile();
            if (file.isFile()) hash = hashFile(file, hash);
            else if (file.isDirectory()) hash = hashDirectory(file, hash);
        }
        ConfigComparator.LOGGER.info("Client: SHA1 digest of files: {}", hash);
        Communicator.sendToServer(new ServerboundHashPacket(hash));
    }

    private static String hashFile(File file, String hash) {
        try (InputStream is = new FileInputStream(file)) {
            // fk windows
            return bytesToHex(digest.digest(new String(is.readAllBytes()).replaceAll("\r\n", "\n").getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hash;
    }

    private static String hashDirectory(File dir, String hash) {
        for (File file : dir.listFiles()) {
            if (file.isFile()) hash = hashFile(file, hash);
            else if (file.isDirectory()) hash = hashDirectory(file, hash);
        }
        return hash;
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

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(configFiles.size());
        for (String file : configFiles) buffer.writeUtf(file);
    }

    @Override
    public <P extends IPacket> P decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        for (int ii = 0; ii < size; ii++) this.configFiles.add(buffer.readUtf());
        return (P) this;
    }
}
