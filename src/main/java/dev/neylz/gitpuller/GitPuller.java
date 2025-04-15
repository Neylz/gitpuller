package dev.neylz.gitpuller;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.neylz.gitpuller.util.GitUtil;
import dev.neylz.gitpuller.util.ModConfig;
import dev.neylz.gitpuller.util.TokenManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

import static dev.neylz.gitpuller.util.ModRegistries.registerAll;

public class GitPuller implements ModInitializer {
    public static final String MOD_ID = "gitpuller";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {



        registerAll();

        if (!ModConfig.isMonoRepo()) {
            LOGGER.info("GitPuller is running in multi repo mode (default)");
        } else {
            LOGGER.info("GitPuller is running in mono repo mode!");
            if (!GitUtil.URL_PATTERN.matcher(ModConfig.getMonoRepoUrl()).matches()) {
                LOGGER.error("Provided URL is invalid: {}", ModConfig.getMonoRepoUrl());
                System.exit(-1);
                return;
            }

            LOGGER.info("Using {} as the mono repo URL", ModConfig.getMonoRepoUrl());


            File dp = FabricLoader.getInstance().getConfigDir().getParent().resolve("world/datapacks/").toFile();
            if (!GitUtil.isGitRepoRemote(dp, ModConfig.getMonoRepoUrl())) { // if the repo is not already cloned
                LOGGER.info("Datapacks folder does not tracks {}. Cloning mono repo into {}", ModConfig.getMonoRepoUrl(), dp.getAbsolutePath());

                // check if the folder exists and is empty
                if (dp.exists() && Objects.requireNonNull(dp.listFiles()).length > 0) {
                    LOGGER.error("Datapacks folder is not empty! Please remove all files from it before using monorepo mode.");
                    System.exit(-1);
                    return;
                } else if (!dp.exists()) {
                    if (!dp.mkdirs()) {
                        LOGGER.error("Could not create datapacks folder! Please check your permissions.");
                        return;
                    }
                }

                // clone the repo

                try {
                    Git.cloneRepository()
                            .setURI(ModConfig.getMonoRepoUrl())
                            .setDirectory(dp)
                            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(TokenManager.getInstance().getToken(), ""))
                            .call();
                } catch (Exception e) {
                    LOGGER.error("Could not clone the mono repo! Please check your permissions.");
                    System.exit(-1);
                    return;
                }

                LOGGER.info("Successfully cloned monorepo into {}", dp.getAbsolutePath());
            }
        }

        LOGGER.info("GitPuller initialized!");
    }
}
