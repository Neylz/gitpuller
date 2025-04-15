# GitPuller





## Private repository

If you want to use a private repository, you need to set up a token. You can easily generate a token on GitHub [here](https://github.com/settings/tokens).
Make sure to select the `repo` scope.

More information on how to set up a token can be found [on the GitHub help page](https://docs.github.com/fr/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens).

The mod first tries to use the token from the environment, then the config file. If the token is provided through the command, it will be used for the current session and override the other methods.

### Environment token

You can use a (system) environment variable to set a token for all sessions.

#### Unix/Linux/macOS:
```bash
export GITPULLER_TOKEN=<token>
```

#### Windows:
CMD:
```cmd
set GITPULLER_TOKEN=<token>
```
Powershell:
```powershell
$Env:GITPULLER_TOKEN = "<token>"
```

### Config file

You can also set up a token in the config file.

```properties
gitpuller.key=<token>
```

### Temporary token

You can set up in game a token for your current session (will be lost after server restart).

```
/git token <token>
```

## Commands Usage

### Classic
```hs
# Checkouts to a branch, commit, tag or ref
git checkout <pack name> <branch>

# Clone a new repository into the world/datapacks folder. <pack name> is the name of the folder that will be created.
git clone <pack name> <url>

# Gets informations about the current repositories
git info

# Fetches changes for the selected pack. If the HEAD was the last available commit of the branch but new ones got added, it will checkout to the latest available commit. 
git pull <pack name>

# Use a temporary token (not recommended)
git token <token>
```

### MonoRepo mode
MonoRepo mode disables the ``git clone`` command and changes the syntax of others. More infos about mono repos in [the monorepo section](#monorepo).

```hs
# Checkouts to a branch, commit, tag or ref
git checkout <branch>

# Gets informations about the current repository
git info

# Fetches changes. If the HEAD was the last available commit of the branch but new ones got added, it will checkout to the latest available commit. 
git pull

# Use a temporary token (not recommended)
git token <token>
```



## MonoRepo

Starting from versions 1.1.0+, GitPuller supports a monorepo mode.

This mode considers the ``/world/datapacks/`` folder as a single repository instead of a collection of repositories. This means that the ``/git`` commands now interact only with the monorepo and not with the individual repositories. The syntax of some commands has changed to reflect this more infos in [the commands section](#monorepo-mode). 

To enable this mode, you need to set the ``gitpuller.monorepo`` option by the URL of your distant repository in the config.

```properties
gitpuller.monorepo=<url>
```

Leaving this option empty or removing the entry from the config will disable the monorepo mode.

⚠️ **WARNING: your ``/world/datapacks/`` folder must be empty before enabling this mode.**
