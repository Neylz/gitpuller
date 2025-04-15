package dev.neylz.gitpuller.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.neylz.gitpuller.GitPuller;
import dev.neylz.gitpuller.util.GitUtil;
import dev.neylz.gitpuller.util.ModConfig;
import dev.neylz.gitpuller.util.TokenManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;

public class GitPullCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> pullCommand = CommandManager.literal("pull").requires((source) -> source.hasPermissionLevel(2));

        if (!ModConfig.isMonoRepo()) {
            pullCommand = pullCommand.then(CommandManager.argument("pack name", StringArgumentType.word()).suggests(
                    (ctx, builder) -> CommandSource.suggestMatching(GitUtil.getTrackedDatapacks(ctx.getSource().getServer().getSavePath(WorldSavePath.DATAPACKS).toFile()), builder))
                .executes((ctx) -> pullPack(ctx, StringArgumentType.getString(ctx, "pack name")))
            );
        } else {
            pullCommand = pullCommand.executes(GitPullCommand::pullMonoPack);
        }

        dispatcher.register(CommandManager.literal("git").then(pullCommand));
    }


    private static int pullMonoPack(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ctx.getSource().sendFeedback(() -> Text.empty()
                .append(Text.literal("Pulling changes from remote repository").formatted(Formatting.GREEN)), true);

        File file = ctx.getSource().getServer().getSavePath(WorldSavePath.DATAPACKS).toFile();

        // git pull -f --all
        String sha1 = GitUtil.getCurrentHeadSha1(file, 7);
        if (!gitPull(ctx.getSource(), file)) {
            throw new CommandSyntaxException(null, () -> "Failed to pull changes from distant repository");
        }

        String newSha1 = GitUtil.getCurrentHeadSha1(file, 7);
        if (!sha1.equals(newSha1)) {
            ctx.getSource().sendFeedback(
                () -> Text.empty()
                        .append(Text.literal("Pulled changes").formatted(Formatting.RESET))
                        .append(Text.literal(" (").formatted(Formatting.RESET))
                        .append(Text.literal(sha1).formatted(Formatting.AQUA))
                        .append(Text.literal(" -> ").formatted(Formatting.RESET))
                        .append(Text.literal(newSha1).formatted(Formatting.LIGHT_PURPLE))
                        .append(Text.literal(")").formatted(Formatting.RESET)),
                true
            );
        }
        return 1;
    }

    private static int pullPack(CommandContext<ServerCommandSource> ctx, String packName) throws CommandSyntaxException {
        // perform git pull -f --all on the specified pack

        File file = new File(ctx.getSource().getServer().getSavePath(WorldSavePath.DATAPACKS).toFile(), packName);

        if (!file.exists()) {
            throw new CommandSyntaxException(null, () -> "Datapack " + packName + " does not exist");
        } else if (!GitUtil.isGitRepo(file)) {
            throw new CommandSyntaxException(null, () -> "Datapack " + packName + " is not a git repository");
        }

        // git pull -f --all
        String sha1 = GitUtil.getCurrentHeadSha1(file, 7);
        if (!gitPull(ctx.getSource(), file)) {
            throw new CommandSyntaxException(null, () -> "Failed to pull changes from " + packName);
        }

        String newSha1 = GitUtil.getCurrentHeadSha1(file, 7);
        if (!sha1.equals(newSha1)) {
            ctx.getSource().sendFeedback(
                () -> Text.empty()
                        .append(Text.literal("Pulled changes from ").formatted(Formatting.RESET))
                        .append(Text.literal("[" + packName + "]").formatted(Formatting.YELLOW))
                        .append(Text.literal(" (").formatted(Formatting.RESET))
                        .append(Text.literal(sha1).formatted(Formatting.AQUA))
                        .append(Text.literal(" -> ").formatted(Formatting.RESET))
                        .append(Text.literal(newSha1).formatted(Formatting.LIGHT_PURPLE))
                        .append(Text.literal(")").formatted(Formatting.RESET)),
                true
            );
        }



        return 1;
    }



    private static boolean gitPull(ServerCommandSource sender, File repoDir) throws CommandSyntaxException {
        try {
            Git git = Git.open(repoDir);

            git.fetch()
                    .setRemoveDeletedRefs(true)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(TokenManager.getInstance().getToken(), ""))
                    .call();

            git.pull()
                    .setRebase(true)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(TokenManager.getInstance().getToken(), ""))
                    .call();

            GitPuller.LOGGER.info("Fetched changes from remote repository");
            sender.sendFeedback(
                    () -> Text.empty()
                            .append("Fetched changes from remote repository").formatted(Formatting.GREEN),
                    true
            );


            return true;
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
            throw new CommandSyntaxException(null, () -> "Failed to pull changes from remote repository: " + e.getMessage());
        }
    }
}
