package com.nova.ogoneblock.island;

import com.nova.ogoneblock.OGOneBlockPlugin;
import com.nova.ogoneblock.phase.OGPhase;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class OGBlockTable {

    private final OGOneBlockPlugin plugin;
    private final List<OGPhase.BlockEntry> fallbackEntries = new ArrayList<>();
    private int fallbackWeight = 0;
    private int starterQueue = 12;

    public OGBlockTable(OGOneBlockPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        fallbackEntries.clear();
        fallbackWeight = 0;
        starterQueue = Math.max(1, plugin.getConfig().getInt("blocks.starter-queue", 12));
        for (String raw : plugin.getConfig().getStringList("blocks.rolls")) {
            String[] parts = raw.split(":", 2);
            Material material = Material.matchMaterial(parts[0].trim());
            int weight = parts.length >= 2 ? parseWeight(parts[1]) : 1;
            if (material == null || !material.isBlock() || !material.isSolid() || weight <= 0) continue;
            fallbackEntries.add(new OGPhase.BlockEntry(material, weight));
            fallbackWeight += weight;
        }
        if (fallbackEntries.isEmpty()) {
            add(Material.DIRT, 10);
            add(Material.STONE, 10);
            add(Material.OAK_LOG, 3);
        }
    }

    private void add(Material material, int weight) {
        fallbackEntries.add(new OGPhase.BlockEntry(material, weight));
        fallbackWeight += weight;
    }

    private int parseWeight(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    public Material roll() {
        return rollEntries(fallbackEntries, fallbackWeight);
    }

    public Material roll(long blocksBroken) {
        OGPhase phase = plugin.phases().current(blocksBroken);
        int total = 0;
        for (OGPhase.BlockEntry entry : phase.blocks()) total += entry.weight();
        return total <= 0 ? roll() : rollEntries(phase.blocks(), total);
    }

    private Material rollEntries(List<OGPhase.BlockEntry> entries, int totalWeight) {
        int pick = ThreadLocalRandom.current().nextInt(Math.max(1, totalWeight));
        int cursor = 0;
        for (OGPhase.BlockEntry entry : entries) {
            cursor += entry.weight();
            if (pick < cursor) return entry.material();
        }
        return entries.getFirst().material();
    }

    public int starterQueue() { return starterQueue; }
}
