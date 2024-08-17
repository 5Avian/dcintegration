package fiveavian.dcintegration;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import fiveavian.dcintegration.api.ServerEvents;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.net.ChatEmotes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.net.PlayerList;
import net.minecraft.server.net.handler.NetServerHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DCIntegrationServer extends ListenerAdapter implements DedicatedServerModInitializer {
    public static final String MOD_ID = "dcintegration";

    private final DCIConfig config = new DCIConfig();
    private JDA jda;
    private PlayerList playerList;
    private TextChannel channel;
    private JDAWebhookClient webhookClient;

    @Override
    public void onInitializeServer() {
        Path configPath =  FabricLoader.getInstance()
                .getConfigDir()
                .resolve(MOD_ID + ".properties");
        try {
            config.load(configPath);
        } catch (IOException ignored) {}
        try {
            config.save(configPath);
        } catch (IOException ignored) {}
        if (config.botToken.isEmpty() || config.guildId.isEmpty() || config.channelId.isEmpty()) {
            throw new RuntimeException("DCIntegration is not configured correctly. Please ensure that a valid bot token, guild id and channel id have been set.");
        }

        ServerEvents.START.add(this::onStart);
        ServerEvents.STOP.add(this::onStop);
        ServerEvents.CHAT_MESSAGE.add(this::onChatMessage);
    }

    private void onStart(MinecraftServer server) {
        playerList = server.playerList;
        jda = JDABuilder.createLight(config.botToken, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_WEBHOOKS)
                .addEventListeners(this)
                .setAutoReconnect(true)
                .setEnableShutdownHook(true)
                .build();
    }

    private void onStop(MinecraftServer server) {
        jda.shutdown();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        Guild guild = jda.getGuildById(config.guildId);
        if (guild == null) {
            throw new RuntimeException("Guild with id " + config.guildId + " could not be found.");
        }
        channel = guild.getTextChannelById(config.channelId);
        if (channel == null) {
            throw new RuntimeException("Channel with id " + config.channelId + " could not be found.");
        }
        Webhook webhook = channel.retrieveWebhooks().complete()
                .stream()
                .filter(wh -> wh.getName().equals(MOD_ID))
                .findFirst()
                .orElseGet(() -> channel.createWebhook(MOD_ID).complete());
        webhookClient = new WebhookClientBuilder(webhook.getUrl())
                .setAllowedMentions(AllowedMentions.none())
                .buildJDA();
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        webhookClient.close();
    }

    private void onChatMessage(NetServerHandler handler, String content) {
        try {
            String prefix = handler.playerEntity.nickname.isEmpty() ? "" : "~";
            content = formatMinecraftToDiscord(content);
            if (content.isEmpty()) {
                return;
            }
            WebhookMessage message = new WebhookMessageBuilder()
                    .setUsername(prefix + removeSectionSymbols(handler.playerEntity.getDisplayName()))
                    .setAvatarUrl(String.format(config.avatarUrl, URLEncoder.encode(handler.playerEntity.username, "UTF-8")))
                    .setContent(content)
                    .build();
            webhookClient.send(message);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace(); // in any sane case, this should be impossible
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Member member = event.getMember();
        if (!event.getChannel().equals(channel) || member == null || member.getUser().isBot()) {
            return;
        }
        char prefixCode = member.getNickname() == null ? 'a' : 'o';
        playerList.sendEncryptedChatToAllPlayers("§a<§" + prefixCode + escapeSectionSymbols(member.getEffectiveName(), prefixCode) + "§r§a>§r " + formatDiscordToMinecraft(event.getMessage()));
    }

    private static final Pattern[] REPLACE_PATTERNS = {
            Pattern.compile("@everyone"),
            Pattern.compile("@here"),
            Pattern.compile("<@&[0-9]+>"),
    };
    private static final String[] REPLACE_VALUES = {
            "everyone",
            "here",
            "",
    };
    private String formatMinecraftToDiscord(String content) {
        // gotta make sure, at all costs, that users can't ping everyone
        while (true) {
            boolean matched = false;
            for (int i = 0; i < 3; i++) {
                Matcher matcher = REPLACE_PATTERNS[i].matcher(content);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    matcher.appendReplacement(sb, REPLACE_VALUES[i]);
                    matched = true;
                }
                matcher.appendTail(sb);
                content = sb.toString();
            }
            if (!matched) {
                return content;
            }
        }
    }

    @SuppressWarnings("StringConcatenationInLoop")
    private String formatDiscordToMinecraft(Message message) {
        String content = escapeSectionSymbols(ChatEmotes.process(message.getContentRaw()), 'r');
        Mentions mentions = message.getMentions();
        for (Member member : mentions.getMembers()) {
            char prefixCode = member.getNickname() == null ? 'r' : 'o';
            String name = member.getEffectiveName();
            content = content.replaceAll(member.getAsMention(), "@§" + prefixCode + escapeSectionSymbols(Matcher.quoteReplacement(name), prefixCode) + "§r");
        }
        for (CustomEmoji emoji : mentions.getCustomEmojis()) {
            content = content.replace(emoji.getAsMention(), ":" + escapeSectionSymbols(emoji.getName(), 'r') + ":");
        }
        for (GuildChannel channel : mentions.getChannels()) {
            content = content.replace(channel.getAsMention(), "#" + escapeSectionSymbols(channel.getName(), 'r'));
        }
        for (Role role : mentions.getRoles()) {
            content = content.replace(role.getAsMention(), "@" + escapeSectionSymbols(role.getName(), 'r'));
        }
        for (Message.Attachment attachment : message.getAttachments()) {
            if (!content.isEmpty()) {
                content += " ";
            }
            content += "§o" + escapeSectionSymbols(attachment.getFileName(), 'o') + "§r";
        }
        return content;
    }

    private String escapeSectionSymbols(String name, char code) {
        StringBuilder result = new StringBuilder();
        char[] chars = name.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            result.append(c);
            if (c != '§') {
                continue;
            }
            i++;
            if (i >= chars.length) {
                break;
            }
            c = chars[i];
            if ("0123456789abcdefklmnor".indexOf(c) != -1) {
                result.append('§');
                result.append(code);
            }
            result.append(c);
        }
        return result.toString();
    }

    private String removeSectionSymbols(String name) {
        StringBuilder result = new StringBuilder();
        char[] chars = name.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c != '§') {
                result.append(c);
                continue;
            }
            i++;
            if (i >= chars.length) {
                result.append('§');
                break;
            }
            c = chars[i];
            if ("0123456789abcdefklmnor".indexOf(c) == -1) {
                result.append('§');
                result.append(c);
            }
        }
        return result.toString();
    }
}
