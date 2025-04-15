package dev.neylz.gitpuller.util;

import dev.neylz.gitpuller.command.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ModRegistries {
    public static void registerAll() {
        ModConfig.register();

        registerCommands();
    }

    private static void registerCommands() {

        // Register commands
        CommandRegistrationCallback.EVENT.register(GitCheckoutCommand::register);
        CommandRegistrationCallback.EVENT.register(GitCloneCommand::register);
        CommandRegistrationCallback.EVENT.register(GitInfoCommand::register);
        CommandRegistrationCallback.EVENT.register(GitPullCommand::register);
        CommandRegistrationCallback.EVENT.register(GitTokenCommand::register);

    }
}
