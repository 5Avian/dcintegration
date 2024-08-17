package fiveavian.dcintegration.api;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.net.handler.NetServerHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ServerEvents {
    public static final List<Consumer<MinecraftServer>> START = new ArrayList<>();
    public static final List<Consumer<MinecraftServer>> STOP = new ArrayList<>();
    public static final List<BiConsumer<NetServerHandler, String>> CHAT_MESSAGE = new ArrayList<>();
}
