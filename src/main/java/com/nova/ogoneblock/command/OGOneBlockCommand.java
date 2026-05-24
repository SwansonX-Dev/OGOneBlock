package com.nova.ogoneblock.command;

import com.nova.ogoneblock.OGOneBlockPlugin;
import com.nova.ogoneblock.island.OGIsland;
import com.nova.ogoneblock.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class OGOneBlockCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT = List.of("npc", "reload", "info");
    private static final List<String> NPC = List.of("set");

    private final OGOneBlockPlugin plugin;

    public OGOneBlockCommand(OGOneBlockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("ogoneblock.admin")) {
            Text.send(sender, "<red>You don't have permission.");
            return true;
        }
        if (args.length == 0) {
            usage(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "npc" -> handleNpc(sender, args);
            case "reload" -> {
                plugin.reloadRuntime();
                Text.send(sender, "<green>Reloaded OG OneBlock.");
            }
            case "info" -> {
                Text.send(sender, "<gray>World: <yellow>" + plugin.worlds().worldName());
                Text.send(sender, "<gray>Islands: <yellow>" + plugin.islands().count());
            }
            default -> usage(sender);
        }
        return true;
    }

    private void handleNpc(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("set")) {
            Text.send(sender, "<yellow>/ogob npc set");
            return;
        }
        if (!(sender instanceof Player player)) {
            Text.send(sender, "<red>Players only.");
            return;
        }
        plugin.npc().setLocation(player.getLocation());
        Text.send(sender, "<green>OG OneBlock NPC set here.");
    }

    private void usage(CommandSender sender) {
        Text.send(sender, "<yellow>/ogob npc set <gray>- place the spawn NPC");
        Text.send(sender, "<yellow>/ogob reload <gray>- reload config");
        Text.send(sender, "<yellow>/ogob info <gray>- show runtime info");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("ogoneblock.admin")) return List.of();
        if (args.length == 1) return filter(ROOT, args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("npc")) return filter(NPC, args[1]);
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String p = prefix.toLowerCase();
        return values.stream().filter(value -> value.startsWith(p)).toList();
    }
}
