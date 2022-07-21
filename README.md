# ConfigComparator
Hosting a modded server but not wanting players to cheat by editing their configuration files?  
ConfigComparator does exactly that. This mod makes a SHA256 digest from both the server side and the client side and compare them.
If there is a mismatch, the server will take action.

## Configuration
The configuration file of this mod is located in `config/configcomparator-common.toml`. It is only used on the server side.

```toml
#The duration (in milliseconds) before the server kicks the client for not sending the digest of config files.
#Range: 1000 ~ 3600000
timeout = 30000
#Files to be included for comparison.
#The game directory will be the root directory.
#For example: Putting "config/configcomparator-common.toml" here will compare this file.
files = []
#The action to take if the digests don't match.
#Allowed Values: KICK, WARN_ADMIN, LOG
action = "KICK"
```
The most important part of the configuration file is `files`.  
They are the paths to the files you want to be compared.

The default action is `KICK`, which means the player will be kicked if the files don't match.  
`WARN_ADMIN` will warn the OPs in game and `LOG` will only log to the console.

`timeout` is just the time before the server kicks the client that refuses to send its digest back.