package fiveavian.dcintegration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class DCIConfig {
    public String botToken = "";
    public String guildId = "";
    public String channelId = "";
    public String avatarUrl = "https://mc-heads.net/head/%s";

    public void load(Path path) throws IOException {
        Properties properties = new Properties();
        properties.load(Files.newBufferedReader(path));
        botToken = properties.getProperty("botToken", botToken);
        guildId = properties.getProperty("guildId", guildId);
        channelId = properties.getProperty("channelId", channelId);
        avatarUrl = properties.getProperty("avatarUrl", avatarUrl);
    }

    public void save(Path path) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("botToken", botToken);
        properties.setProperty("guildId", guildId);
        properties.setProperty("channelId", channelId);
        properties.setProperty("avatarUrl", avatarUrl);
        properties.store(Files.newBufferedWriter(path), null);
    }
}
