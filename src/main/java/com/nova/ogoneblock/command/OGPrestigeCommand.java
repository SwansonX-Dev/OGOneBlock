package com.nova.ogoneblock.command;

import com.nova.ogoneblock.OGOneBlockPlugin;
import com.nova.ogoneblock.island.OGIsland;
import com.nova.ogoneblock.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class OGPrestigeCommand implements CommandExecutor {

    private final OGOneBlockPlugin plugin;

    public OGPrestigeCommand(OGOneBlockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Text.send(sender, "<red>Players only.");
            return true;
        }
        OGIsland island = plugin.islands().of(player);
        if (island == null) {
            Text.send(player, "<red>You don't have an OG island yet.");
            return true;
        }

        long broken = island.data().blocksBroken();
        long required = plugin.phases().prestigeThreshold();
        int level = island.data().prestigeLevel();

        if (args.length == 0) {
            Text.send(player, "<gold>OG Prestige <gray>— level <yellow>" + level);
            Text.send(player, "<gray>Blocks broken: <white>" + broken + " <dark_gray>/ <white>" + required);
            if (broken >= required) {
                Text.send(player, "<green>Eligible! Run <yellow>/ogprestige confirm<green> to reset progress and gain a level.");
            } else {
                Text.send(player, "<red>Not yet. Reach the final phase (<yellow>" + required + "<red> blocks) to prestige.");
            }
            return true;
        }

        if (!args[0].equalsIgnoreCase("confirm")) {
            Text.send(player, "<yellow>/ogprestige <gray>- view status, <yellow>/ogprestige confirm <gray>- prestige now.");
            return true;
        }

        if (broken < required) {
            Text.send(player, "<red>You need <yellow>" + (required - broken) + "<red> more blocks broken.");
            return true;
        }

        island.data().prestige();
        plugin.islands().save(island);

        int newLevel = island.data().prestigeLevel();
        String roman = roman(newLevel);
        String tagId = "og_prestige_" + newLevel;
        String display = "OG Prestige " + roman;
        String format = prestigeTagFormat(newLevel, roman);
        plugin.milestones().grantTag(player, tagId, display, format);

        Text.send(player, "<gold><bold>PRESTIGE!</bold> <yellow>Level " + newLevel + " <gray>achieved. Progress reset.");
        Text.send(player, "<gray>Tag unlocked: <reset>" + format);
        Bukkit.broadcast(Text.mm("<dark_gray>[<gold>OGOB</gold>]</dark_gray> <yellow>"
                + player.getName() + "<gold> hit prestige <yellow>" + newLevel + "<gold>!"));
        return true;
    }

    private String prestigeTagFormat(int level, String roman) {
        String body = "[Prestige " + roman + "]";
        if (level >= 10) return "<gradient:#FFD700:#FF4DFF:#7DD3FC:#FFD700><bold>" + body + "</bold></gradient>";
        if (level >= 7)  return "<gradient:#FF4DFF:#FFD700:#FF4DFF><bold>" + body + "</bold></gradient>";
        if (level >= 4)  return "<gradient:#FF6B35:#FFE066:#FF6B35><bold>" + body + "</bold></gradient>";
        return "<gradient:#FFE066:#FFB300:#FFE066>" + body + "</gradient>";
    }

    private String roman(int n) {
        if (n <= 0) return String.valueOf(n);
        String[] M = {"", "M", "MM", "MMM"};
        String[] C = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] X = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] I = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return M[Math.min(3, n / 1000)] + C[(n % 1000) / 100] + X[(n % 100) / 10] + I[n % 10];
    }
}
