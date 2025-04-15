package dev.neylz.gitpuller.util;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GitUtil {
    public static final Pattern URL_PATTERN = Pattern.compile("^(https?|ftp)://[^\\s/$.?#].\\S*$");

    @NotNull
    public static String getCurrentBranch(File file) {
        if (!file.exists()) return "";

        try (Git git = Git.open(file)) {
            Ref head = git.getRepository().findRef("HEAD");
            if (head.isSymbolic()) {
                return head.getTarget().getName().substring("refs/heads/".length());
            }
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    @NotNull
    public static String getCurrentHeadSha1(File file, int length) {
        if (!file.exists()) return "";

        try (Git git = Git.open(file)) {
            ObjectId head = git.getRepository().resolve("HEAD");
            if (head != null) {
                return head.name().substring(0, Math.min(length, head.name().length()));
            }
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }


    public static boolean isGitRepo(File file) {
        if (!file.exists()) return false;

        try (Git ignored = Git.open(file)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isGitRepoRemote(File file, String remoteUrl) {
        if (!file.exists()) return false;

        try (Git git = Git.open(file)) {
            String remote = git.getRepository().getConfig().getString("remote", "origin", "url");
            return remote.equals(remoteUrl);
        } catch (IOException e) {
            return false;
        }
    }


    @NotNull
    public static List<String> getBranches(File file) {
        if (!file.exists()) return List.of();

        try (Git git = Git.open(file)) {
            return git.branchList().call().stream().map(Ref::getName).toList();
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @NotNull
    public static List<String> getTrackedDatapacks(File parentFolder) {
        if (!parentFolder.exists()) return List.of();

        File[] files = parentFolder.listFiles();
        if (files == null) return List.of();

        return Stream.of(files).filter(File::isDirectory).filter(GitUtil::isGitRepo).map(File::getName).toList();
    }




}
