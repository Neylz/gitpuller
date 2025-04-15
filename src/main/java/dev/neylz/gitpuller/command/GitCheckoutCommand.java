package dev.neylz.gitpuller.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class GitCheckoutCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> checkoutCommand = CommandManager.literal("checkout").requires((source) -> source.hasPermissionLevel(2));
        RequiredArgumentBuilder<ServerCommandSource, String> branchArg = CommandManager.argument("branch", StringArgumentType.greedyString());

        if (!ModConfig.isMonoRepo()) {
            checkoutCommand = checkoutCommand
                .then(CommandManager.argument("pack name", StringArgumentType.word()).suggests(
                    (ctx, builder) -> CommandSource.suggestMatching(GitUtil.getTrackedDatapacks(ctx.getSource().getServer().getSavePath(WorldSavePath.DATAPACKS).toFile()), builder))
                .then(branchArg.suggests(
                    (ctx, builder) -> CommandSource.suggestMatching(GitUtil.getBranches(new File(ctx.getSource().getServer().getSavePath(WorldSavePath.DATAPACKS).toFile(), StringArgumentType.getString(ctx, "pack name"))), builder))
                .executes(
                    (ctx) -> checkout(ctx, StringArgumentType.getString(ctx, "pack name"), StringArgumentType.getString(ctx, "branch"))
            )));
            
        } else {
            checkoutCommand = checkoutCommand
                .then(branchArg
                .executes(
                    (ctx) -> checkoutMono(ctx, StringArgumentType.getString(ctx, "branch"))
                ));
        }

        dispatcher.register(CommandManager.literal("git")
            .then(checkoutCommand)
        );
    }

    private static int checkoutMono(CommandContext<ServerCommandSource> ctx, String branch) throws CommandSyntaxException {
        ctx.getSource().sendFeedback(() -> Text.empty()
                .append(Text.literal("Checking out branch ").formatted(Formatting.RESET))
                .append(Text.literal(branch).formatted(Formatting.DARK_GREEN))
                .append(Text.literal(" in the mono repo").formatted(Formatting.RESET)),
            true);
        return 1;
    }

    private static int checkout(CommandContext<ServerCommandSource> ctx, String pack, String branch) throws CommandSyntaxException {

        File packDir = new File(ctx.getSource().getServer().getSavePath(WorldSavePath.DATAPACKS).toFile(), pack);
        if (!packDir.exists()) {
            throw new CommandSyntaxException(null, () -> "Datapack " + pack + " does not exist");
        } else if (!GitUtil.isGitRepo(packDir)) {
            throw new CommandSyntaxException(null, () -> "Datapack " + pack + " is not a git repository");
        }

        gitCheckout(ctx.getSource(), packDir, branch);

//        if (!gitCheckout(packDir, branch)) {
//            throw new CommandSyntaxException(null, () -> "Failed to checkout branch " + branch + " in " + pack);
//        } else {
//            ctx.getSource()
//        }

        return 1;

    }

    private static void gitCheckout(ServerCommandSource source, File file, String ref) throws CommandSyntaxException {
        try (Git git = Git.open(file)) {
            // Fetch all branches from remote
            git.fetch()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(TokenManager.getInstance().getToken(), ""))
                    .call();

            // Determine if ref is a SHA-1 hash or a branch name
            if (isSHA1(ref)) {
                // Checkout to the specific commit
                Repository repository = git.getRepository();
                try (RevWalk revWalk = new RevWalk(repository)) {
                    RevCommit commit = revWalk.parseCommit(repository.resolve(ref));
                    git.checkout()
                            .setName(ref)
                            .setCreateBranch(true)
                            .setStartPoint(commit)
                            .call();

                    source.sendFeedback(
                        () -> Text.empty()
                                .append(Text.literal("Checked out commit ").formatted(Formatting.RESET))
                                .append(Text.literal(ref).formatted(Formatting.LIGHT_PURPLE)),
                            true);
                } catch (IOException e) {
//                    e.printStackTrace();
                    throw new CommandSyntaxException(null, () -> "Failed to checkout commit " + ref);

                }
            } else {
                // Check if the branch exists locally
                List<Ref> branchList = git.branchList().call();
                boolean branchExists = branchList.stream().anyMatch(branch -> branch.getName().equals("refs/heads/" + ref));

                if (!branchExists) {
                    // Create a new branch tracking the remote branch
                    git.checkout()
                            .setCreateBranch(true)
                            .setName(ref)
                            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                            .setStartPoint("origin/" + ref)
                            .call();
                } else {
                    // Checkout to the existing branch
                    git.checkout()
                            .setName(ref)
                            .call();
                }

                source.sendFeedback(
                        () -> Text.empty()
                                .append(Text.literal("Checked out branch ").formatted(Formatting.RESET))
                                .append(Text.literal(ref).formatted(Formatting.DARK_GREEN))
                                .append(Text.literal(" in ").formatted(Formatting.RESET))
                                .append(Text.literal("[" + file.getName() + "]").formatted(Formatting.YELLOW)),
                        true);
            }

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }


    private static boolean isSHA1(String ref) {
        return Pattern.matches("^[a-fA-F0-9]{40}$", ref);
    }

}
