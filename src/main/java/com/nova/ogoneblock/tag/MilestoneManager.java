package com.nova.ogoneblock.tag;

import com.nova.ogoneblock.OGOneBlockPlugin;
import com.nova.ogoneblock.island.OGIsland;
import com.nova.ogoneblock.util.Text;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class MilestoneManager {

    private final OGOneBlockPlugin plugin;
    private final List<Milestone> milestones = new ArrayList<>();

    public MilestoneManager(OGOneBlockPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        milestones.clear();
        for (var raw : plugin.getConfig().getMapList("milestones")) {
            long blocks = asLong(raw.get("blocks"));
            Object rawTagId = raw.get("tag-id");
            String tagId = (rawTagId == null ? "" : String.valueOf(rawTagId)).toLowerCase(Locale.ROOT);
            Object rawDisplay = raw.get("display");
            String display = rawDisplay == null ? tagId : String.valueOf(rawDisplay);
            Object rawFormat = raw.get("format");
            String format = rawFormat == null ? "<gray>[" + display + "]" : String.valueOf(rawFormat);
            if (blocks <= 0 || tagId.isBlank()) continue;
            milestones.add(new Milestone(blocks, tagId, display, format));
        }
        milestones.sort(Comparator.comparingLong(Milestone::blocks));
    }

    public void check(Player player, OGIsland island) {
        long broken = island.data().blocksBroken();
        for (Milestone milestone : milestones) {
            String key = milestone.tagId();
            if (broken < milestone.blocks() || island.data().claimedMilestones().contains(key)) continue;
            island.data().claimedMilestones().add(key);
            grantTag(player, milestone);
            Text.send(player, "<gold>Milestone reached: <yellow>" + milestone.blocks()
                    + " blocks<gold>. Tag unlocked: <reset>" + milestone.format());
        }
    }

    private void grantTag(Player player, Milestone milestone) {
        org.bukkit.plugin.Plugin xTags = plugin.getServer().getPluginManager().getPlugin("xTags");
        if (xTags == null) {
            plugin.getLogger().warning("xTags is not installed; cannot grant " + milestone.tagId());
            return;
        }
        try {
            Method tagManagerMethod = xTags.getClass().getMethod("tagManager");
            Object tagManager = tagManagerMethod.invoke(xTags);
            Method upsert = tagManager.getClass().getMethod("upsert", String.class, String.class, String.class, String.class);
            Method grant = tagManager.getClass().getMethod("grant", java.util.UUID.class, String.class);
            upsert.invoke(tagManager, milestone.tagId(), milestone.display(), milestone.format(), "");
            grant.invoke(tagManager, player.getUniqueId(), milestone.tagId());
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("Failed to grant xTags milestone " + milestone.tagId() + ": " + ex.getMessage());
        }
    }

    private long asLong(Object raw) {
        if (raw instanceof Number n) return n.longValue();
        if (raw instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }
}
