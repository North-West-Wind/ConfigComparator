package ml.northwestwind.configcomparator;

import ml.northwestwind.configcomparator.config.Config;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ConfigComparator.MOD_ID)
public class ConfigComparator {
    public static final String MOD_ID = "configcomparator";
    public static final Logger LOGGER = LogManager.getLogger();
    // You better not be >:(
    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public ConfigComparator() {
        Config.register();
    }
}
