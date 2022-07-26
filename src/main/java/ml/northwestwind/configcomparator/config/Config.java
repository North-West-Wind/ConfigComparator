package ml.northwestwind.configcomparator.config;

import com.google.common.collect.Lists;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

public class Config {
    private static final ForgeConfigSpec COMMON;
    private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();

    private static ForgeConfigSpec.LongValue TIMEOUT, CHECK_INTERVAL;
    private static ForgeConfigSpec.ConfigValue<List<String>> CONFIG_FILES;
    private static ForgeConfigSpec.EnumValue<Action> ACTION;
    private static ForgeConfigSpec.EnumValue<HashAlgorithm> ALGORITHM;

    static {
        init();
        COMMON = COMMON_BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON);
    }

    private static void init() {
        TIMEOUT = COMMON_BUILDER.comment("The duration (in milliseconds) before the server kicks the client for not sending the digest of config files.").defineInRange("timeout", 30000L, 1000, Long.MAX_VALUE);
        CHECK_INTERVAL = COMMON_BUILDER.comment("The interval (in milliseconds) between each file check.", "0 to disable the interval and only check once (login).").defineInRange("check_interval", 600000L, 0, Long.MAX_VALUE);
        CONFIG_FILES = COMMON_BUILDER.comment("Files to be included for comparison.", "The game directory will be the root directory.", "For example: Putting \"config/configcomparator-common.toml\" here will compare this file.").define("files", Lists.newArrayList());
        ACTION = COMMON_BUILDER.comment("The action to take if the digests don't match.").defineEnum("action", Action.KICK);
        ALGORITHM = COMMON_BUILDER.comment("The algorithm to use for hashing files.").defineEnum("algorithm", HashAlgorithm.MD5);
    }

    public static long getTimeout() {
        return TIMEOUT.get();
    }

    public static long getCheckInterval() {
        return CHECK_INTERVAL.get();
    }

    public static List<String> getFiles() {
        return CONFIG_FILES.get();
    }

    public static Action getAction() {
        return ACTION.get();
    }

    public static HashAlgorithm getAlgorithm() {
        return ALGORITHM.get();
    }

    public enum Action {
        KICK,
        WARN_ADMIN,
        LOG
    }

    public enum HashAlgorithm {
        MD5("MD5"),
        SHA1("SHA-1"),
        SHA256("SHA-256");

        final String algor;
        HashAlgorithm(String algor) {
            this.algor = algor;
        }

        public String getString() {
            return algor;
        }
    }
}
