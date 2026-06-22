package com.nova.ogoneblock.scoreboard;

import com.nova.ogoneblock.OGOneBlockPlugin;
import com.nova.ogoneblock.island.OGIsland;
import com.nova.ogoneblock.phase.OGPhase;
import com.nova.ogoneblock.tag.Milestone;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sidebar shown to players inside the OG OneBlock world. Renders using the
 * team-prefix trick so it carries over cleanly to Bedrock via Geyser.
 */
public final class OGScoreboardManager {

    private final OGOneBlockPlugin plugin;
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private BukkitTask ticker;

    public OGScoreboardManager(OGOneBlockPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (ticker != null) ticker.cancel();
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (ticker != null) { ticker.cancel(); ticker = null; }
        for (Player p : Bukkit.getOnlinePlayers()) clear(p);
        boards.clear();
    }

    private void tick() {
        String ogWorld = plugin.worlds().worldName();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getName().equals(ogWorld)) {
                render(p);
            } else if (boards.containsKey(p.getUniqueId())) {
                clear(p);
            }
        }
    }

    private void render(Player player) {
        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(),
                u -> Bukkit.getScoreboardManager().getNewScoreboard());
        Objective obj = board.getObjective("ogob");
        if (obj != null) obj.unregister();
        obj = board.registerNewObjective("ogob", Criteria.DUMMY,
                mm("<gradient:#FFB300:#FF5722><bold>OG OneBlock"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = buildLines(player);
        int score = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            String text = lines.get(i);
            String teamName = "ogob_" + i;
            String entry = uniqueEntry(i);
            Team team = board.getTeam(teamName);
            if (team == null) team = board.registerNewTeam(teamName);
            team.prefix(mm(text));
            if (!team.hasEntry(entry)) {
                for (String e : new ArrayList<>(team.getEntries())) team.removeEntry(e);
                team.addEntry(entry);
            }
            obj.getScore(entry).setScore(score - i);
        }
        player.setScoreboard(board);
    }

    private List<String> buildLines(Player player) {
        List<String> lines = new ArrayList<>();
        OGIsland island = plugin.islands().homeIsland(player);
        if (island == null) {
            lines.add("<gray>No OG island yet.");
            lines.add("<gray>Talk to the spawn NPC.");
            lines.add(" ");
            lines.add("<gray>Online: <green>" + Bukkit.getOnlinePlayers().size());
            return lines;
        }

        int prestige = island.data().prestigeLevel();
        if (prestige > 0) {
            lines.add("<gold>Prestige: <yellow>" + prestige);
        }

        long broken = island.data().blocksBroken();
        OGPhase phase = plugin.phases().current(broken);
        OGPhase next = plugin.phases().next(broken);
        lines.add("<gray>Phase: <gold>" + phase.display());
        if (next == null) {
            lines.add("<gray>Phase progress: <green>MAX");
        } else {
            long inPhase = broken - phase.startBlocks();
            long span = next.startBlocks() - phase.startBlocks();
            lines.add("<gray>Next: <white>" + inPhase + " <dark_gray>/ <white>" + span
                    + " <dark_gray>(<yellow>" + next.display() + "<dark_gray>)");
        }
        lines.add("<gray>Broken: <white>" + broken);

        long threshold = plugin.phases().prestigeThreshold();
        if (broken >= threshold) {
            lines.add("<green>Prestige ready! <yellow>/ogprestige");
        }

        Milestone milestone = plugin.milestones().next(island);
        if (prestige == 0 && milestone != null) {
            long need = Math.max(0L, milestone.blocks() - broken);
            lines.add("<gray>Tag: <aqua>" + milestone.display() + " <dark_gray>(<white>" + need + "<dark_gray>)");
        }

        lines.add(" ");
        lines.add("<gray>Online: <green>" + Bukkit.getOnlinePlayers().size());
        return lines;
    }

    private Component mm(String raw) {
        return MM.deserialize(raw == null ? "" : raw);
    }

    private String uniqueEntry(int i) {
        char[] hex = "0123456789abcdef".toCharArray();
        return "§" + hex[i % 16] + "§r" + (i / 16 == 0 ? "" : Character.toString((char) ('a' + (i / 16))));
    }

    public void clear(Player p) {
        Scoreboard b = boards.remove(p.getUniqueId());
        if (b != null) p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }
}
