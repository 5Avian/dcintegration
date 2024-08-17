# DCIntegration

[Requires Halplibe](https://github.com/Turnip-Labs/bta-halplibe)

A Discord integration mod that links your Minecraft chat to a Discord channel.

## Usage

First of all, run the server once with this mod installed, so a config file gets generated at `.minecraft/config/dcintegration.properties`.
When that's done, you should configure this mod by providing a bot token, a guild id, and a channel id.

For the bot token, you must first create a new [Discord application](https://discord.com/developers/applications).
On the `Bot` page, click `Reset Token` and copy the newly generated token into the config file as the `botToken`.
On the same page, ensure that the `Message Content Intent` is turned on.
You can add this bot to your server using the following steps:

1. Go to the `OAuth2` page.
2. Tick the `bot` checkbox in the `OAuth2 URL Generator` under `scopes`.
3. Tick the `Manage Webhooks` and `Send Messages` checkboxes under `Bot Permissions`.
4. Copy the `Generated URL` into your browser, and follow the setup there.

For the guild id and the channel id, you need to enable Developer Mode.

1. Go to the Settings menu of your Discord client.
2. Under `App Settings` -> `Advanced`, ensure `Developer Mode` is turned on.

Right-click the server you want to use, and copy the ID into the config file as the `guildId`.
Right-click the channel you want to use, and copy the ID into the config file as the `channelId`.
