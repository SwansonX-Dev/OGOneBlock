package com.nova.ogoneblock.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String PREFIX = "<dark_gray>[<gold>OGOB</gold>]</dark_gray> ";

    private Text() {}

    public static Component mm(String raw) {
        return MM.deserialize(raw == null ? "" : raw);
    }

    public static void send(CommandSender sender, String raw) {
        sender.sendMessage(mm(PREFIX + raw));
    }
}
