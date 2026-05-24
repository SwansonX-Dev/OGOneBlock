package com.nova.ogoneblock.island;

import com.nova.ogoneblock.OGOneBlockPlugin;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class OGBlockTable {

    private final OGOneBlockPlugin plugin;
    private final List<Entry> entries = new ArrayList<>();
    private int totalWeight = 0;
    private int starterQueue = 12;

    public OGBlockTable(OGOneBlockPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        entries.clear();
        totalWeight = 0;
        starterQueue = Math.max(1, plugin.getConfig().getInt("blocks.starter-queue", 12));
        for (String raw : plugin.getConfig().getStringList("blocks.rolls")) {
            String[] parts = raw.split(":", 2);
            Material material = Material.matchMaterial(parts[0].trim());
            int weight = parts.length >= 2 ? parseWeight(parts[1]) : 1;
            if (material == null || !material.isBlock() || !material.isSolid() || weight <= 0) continue;
            entries.add(new Entry(material, weight));
            totalWeight += weight;
        }
        if (entries.isEmpty()) {
            add(Material.DIRT, 10);
            add(Material.STONE, 10);
            add(Material.OAK_LOG, 3);
        }
    }

    private void add(Material material, int weight) {
        entries.add(new Entry(material, weight));
        totalWeight += weight;
    }

    private int parseWeight(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    public Material roll() {
        int pick = ThreadLocalRandom.current().nextInt(Math.max(1, totalWeight));
        int cursor = 0;
        for (Entry entry : entries) {
            cursor += entry.weight;
            if (pick < cursor) return entry.material;
        }
        return entries.getFirst().material;
    }

    public int starterQueue() { return starterQueue; }

    private record Entry(Material material, int weight) {}
}
