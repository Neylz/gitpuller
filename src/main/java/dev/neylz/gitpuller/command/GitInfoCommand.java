package dev.neylz.gitpuller.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.neylz.gitpuller.util.GitUtil;
import dev.neylz.gitpuller.util.ModConfig;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import org.eclipse.jgit.api.Git;

import java.io.File;
import java.io.IOException;

public class GitInfoCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> infoCommand = CommandManager.literal("info");

        if (!ModConfig.isMonoRepo()) {
            infoCommand = infoCommand.executes(GitInfoCommand::datapackInfo);
        } else {
            infoCommand = infoCommand.executes(GitInfoCommand::datapackMonoInfo);
        }

        dispatcher.register(CommandManager.literal("git")
            .then(infoCommand)
        );
    }

    private static int datapackMonoInfo(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        File file = ctx.getSource().getServer().getSavePath(WorldSavePath.DATAPACKS).toFile();
        String remote = "";

        try (Git git = Git.open(file)) {
            remote = git.getRepository().getConfig().getString("remote", "origin", "url");
        } catch (IOException e) {
            throw new CommandSyntaxException(null, () -> "Failed to open git repository: " + e.getMessage());
        }

        String finalRemote = remote;
        ctx.getSource().sendFeedback(() -> {
            return Text.empty()
                    .append(Text.literal("Currently tracking as monorepo ")
                    .append(Text.literal(finalRemote).formatted(Formatting.AQUA))
                    .append(Text.literal("\n  (").formatted(Formatting.RESET))
                    .append(Text.literal(GitUtil.getCurrentBranch(file)).formatted(Formatting.DARK_GREEN))
                    .append(Text.literal("-").formatted(Formatting.RESET))
                    .append(Text.literal(GitUtil.getCurrentHeadSha1(file, 7)).formatted(Formatting.AQUA))
                    .append(Text.literal(")").formatted(Formatting.RESET))
            );
        }, false);

        return 1;
    }

    private static int datapackInfo(CommandContext<ServerCommandSource> ctx) {
        MinecraftServer server = ctx.getSource().getServer();

        File file = server.getSavePath(WorldSavePath.DATAPACKS).toFile();

        // list all files
        File[] files = file.listFiles();
        if (files != null) {
            ctx.getSource().sendFeedback(() -> {
                MutableText text = Text.empty()
                        .append(Text.literal("Available datapacks:").formatted(Formatting.UNDERLINE));
                for (File f : files) {
                    if (!f.isDirectory()) continue;

                    text.append(Text.literal("\n   ").formatted(Formatting.RESET))
                        .append(Text.literal("[" + f.getName() + "]").formatted(Formatting.YELLOW));

                    if (GitUtil.isGitRepo(f)) {
                        text.append(Text.literal("  (").formatted(Formatting.RESET))
                            .append(Text.literal(GitUtil.getCurrentBranch(f)).formatted(Formatting.DARK_GREEN))
                            .append(Text.literal("-").formatted(Formatting.RESET))
                            .append(Text.literal(GitUtil.getCurrentHeadSha1(f, 7)).formatted(Formatting.AQUA))
                            .append(Text.literal(")").formatted(Formatting.RESET));
                    } else {
                        text.append(Text.literal("  (untracked)").formatted(Formatting.RED).formatted(Formatting.ITALIC));
                    }
                }

                return text;
            }, false);
        }


        return 1;
    }

}
