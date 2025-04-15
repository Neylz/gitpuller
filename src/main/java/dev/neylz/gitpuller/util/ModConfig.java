package dev.neylz.gitpuller.util;

import com.mojang.datafixers.util.Pair;
import dev.neylz.gitpuller.GitPuller;

public class ModConfig {
    public static SimpleConfig CONFIG;
    private static ModConfigProvider configs;

    private static String GITPULLER_TOKEN;
    private static String MONOREPO;


    public static void register() {

        configs = new ModConfigProvider();
        createConfig();

        CONFIG = SimpleConfig.of(GitPuller.MOD_ID + "config").provider(configs).request();

        assignConfigs();

    }

    private static void createConfig() {
        configs.addKeyValuePair(new Pair<>("gitpuller.key", ""), "Provide your key here. You can also provide it via environment variable GITPULLER_TOKEN or in game with /gitpuller token <key>");

        configs.addKeyValuePair(new Pair<>("gitpuller.monorepo", ""), "Set a repository URL if you are using a monorepo. If true, world/datapacks/ will be considered as the monorepo root. Leave empty if you want to be able to use all mod's features.");
    }

    private static void assignConfigs() {
        GITPULLER_TOKEN = CONFIG.getOrDefault("gitpuller.key", null);
        MONOREPO = CONFIG.getOrDefault("gitpuller.monorepo", null);

        GitPuller.LOGGER.info("All " + configs.getConfigsList().size() + " have been set properly");

    }

    public static boolean isMonoRepo() {
        return !(MONOREPO == null || MONOREPO.isEmpty());
    }

    public static String getMonoRepoUrl() {
        if (MONOREPO == null) {
            return "";
        }
        return MONOREPO;
    }
}
