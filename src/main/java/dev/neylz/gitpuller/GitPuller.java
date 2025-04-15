package dev.neylz.gitpuller;

import dev.neylz.gitpuller.util.ModConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.neylz.gitpuller.util.ModRegistries.registerAll;

public class GitPuller implements ModInitializer {
    public static final String MOD_ID = "gitpuller";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {



        registerAll();

        if (ModConfig.isMonoRepo()) {
            LOGGER.info("GitPuller is running in mono repo mode!");
            LOGGER.info("Using {} as the mono repo URL", ModConfig.getMonoRepoUrl());
            LOGGER.info("Commands syntax have been modified.");
        } else {
            LOGGER.info("GitPuller is running in multi repo mode (default)");
        }

        LOGGER.info("GitPuller initialized!");
    }
}
