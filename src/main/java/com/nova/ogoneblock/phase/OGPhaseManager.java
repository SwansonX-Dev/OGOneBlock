package com.nova.ogoneblock.phase;

import com.nova.ogoneblock.OGOneBlockPlugin;
import com.nova.ogoneblock.island.OGIsland;
import com.nova.ogoneblock.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class OGPhaseManager {

    private final OGOneBlockPlugin plugin;
    private final List<OGPhase> phases = new ArrayList<>();

    public OGPhaseManager(OGOneBlockPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        phases.clear();
        for (var raw : plugin.getConfig().getMapList("phases")) {
            if (!(raw instanceof java.util.Map<?, ?> map)) continue;
            Object rawId = map.containsKey("id") ? map.get("id") : "phase-" + phases.size();
            String id = String.valueOf(rawId);
            Object rawDisplay = map.containsKey("display") ? map.get("display") : id;
            String display = String.valueOf(rawDisplay);
            long start = number(map.get("start-blocks"), 0L);
            List<OGPhase.BlockEntry> blocks = blocks(map.get("blocks"));
            List<EntityType> passive = entities(map.get("passive-spawns"));
            List<EntityType> hostile = entities(map.get("hostile-spawns"));
            List<OGPhase.LootEntry> loot = loot(map.get("loot"));
            double passiveChance = decimal(map.get("passive-chance"), 0.0D);
            double hostileChance = decimal(map.get("hostile-chance"), 0.0D);
            if (!blocks.isEmpty()) {
                phases.add(new OGPhase(id, display, Math.max(0L, start), blocks, passive, hostile, loot,
                        Math.max(0.0D, passiveChance), Math.max(0.0D, hostileChance)));
            }
        }
        phases.sort(Comparator.comparingLong(OGPhase::startBlocks));
        if (phases.isEmpty()) phases.add(defaultPhase());
    }

    public OGPhase current(long blocksBroken) {
        OGPhase selected = phases.getFirst();
        for (OGPhase phase : phases) {
            if (blocksBroken < phase.startBlocks()) break;
            selected = phase;
        }
        return selected;
    }

    public OGPhase previous(long blocksBroken) {
        OGPhase current = current(blocksBroken);
        int index = phases.indexOf(current);
        return index <= 0 ? current : phases.get(index - 1);
    }

    public void maybeAnnouncePhase(Player player, long before, long after) {
        OGPhase old = current(before);
        OGPhase now = current(after);
        if (old == now) return;
        Text.send(player, "<gold>OG phase unlocked: <yellow>" + now.display());
    }

    public void rollSpawn(Player player, OGIsland island) {
        OGPhase phase = current(island.data().blocksBroken());
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (!phase.passiveSpawns().isEmpty() && rng.nextDouble() < phase.passiveChance()) {
            spawn(island, phase.passiveSpawns().get(rng.nextInt(phase.passiveSpawns().size())));
        }
        if (!phase.hostileSpawns().isEmpty() && rng.nextDouble() < phase.hostileChance()) {
            spawn(island, phase.hostileSpawns().get(rng.nextInt(phase.hostileSpawns().size())));
        }
    }

    public void fillLootChest(OGIsland island, org.bukkit.block.Block block) {
        if (!(block.getState() instanceof Chest chest)) return;
        OGPhase phase = current(island.data().blocksBroken());
        if (phase.loot().isEmpty()) return;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int rolls = rng.nextInt(3, 7);
        for (int i = 0; i < rolls; i++) {
            OGPhase.LootEntry entry = pickLoot(phase.loot(), rng);
            if (entry == null) continue;
            int amount = rng.nextInt(entry.min(), entry.max() + 1);
            ItemStack stack = new ItemStack(entry.material(), amount);
            int slot;
            int guard = 0;
            do {
                slot = rng.nextInt(chest.getBlockInventory().getSize());
                guard++;
            } while (chest.getBlockInventory().getItem(slot) != null && guard < 50);
            if (chest.getBlockInventory().getItem(slot) == null) chest.getBlockInventory().setItem(slot, stack);
            else chest.getBlockInventory().addItem(stack);
        }
    }

    private void spawn(OGIsland island, EntityType type) {
        Location center = island.centerBlock();
        World world = center.getWorld();
        if (world == null) return;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Location spawn = center.clone().add(rng.nextInt(-1, 2) + 0.5, 1.0, rng.nextInt(-1, 2) + 0.5);
        world.spawnEntity(spawn, type);
    }

    private List<OGPhase.BlockEntry> blocks(Object value) {
        List<OGPhase.BlockEntry> out = new ArrayList<>();
        if (!(value instanceof List<?> list)) return out;
        for (Object item : list) {
            String raw = String.valueOf(item);
            String[] parts = raw.split(":", 2);
            Material material = Material.matchMaterial(parts[0].trim());
            int weight = parts.length >= 2 ? (int) number(parts[1], 1L) : 1;
            if (material == null || !material.isBlock() || weight <= 0) continue;
            out.add(new OGPhase.BlockEntry(material, weight));
        }
        return out;
    }

    private List<EntityType> entities(Object value) {
        List<EntityType> out = new ArrayList<>();
        if (!(value instanceof List<?> list)) return out;
        for (Object item : list) {
            try {
                EntityType type = EntityType.valueOf(String.valueOf(item).trim().toUpperCase(java.util.Locale.ROOT));
                if (type.isAlive()) out.add(type);
            } catch (IllegalArgumentException ignored) {
                // Skip invalid config entries.
            }
        }
        return out;
    }

    private List<OGPhase.LootEntry> loot(Object value) {
        List<OGPhase.LootEntry> out = new ArrayList<>();
        if (!(value instanceof List<?> list)) return out;
        for (Object item : list) {
            String[] parts = String.valueOf(item).split(":");
            if (parts.length < 2) continue;
            Material material = Material.matchMaterial(parts[0].trim());
            if (material == null || !material.isItem()) continue;
            int min = (int) number(parts[1], 1L);
            int max = parts.length >= 3 ? (int) number(parts[2], min) : min;
            int weight = parts.length >= 4 ? (int) number(parts[3], 1L) : 1;
            min = Math.max(1, min);
            max = Math.max(min, max);
            weight = Math.max(1, weight);
            out.add(new OGPhase.LootEntry(material, min, max, weight));
        }
        return out;
    }

    private OGPhase.LootEntry pickLoot(List<OGPhase.LootEntry> entries, ThreadLocalRandom rng) {
        int total = 0;
        for (OGPhase.LootEntry entry : entries) total += entry.weight();
        int pick = rng.nextInt(Math.max(1, total));
        int cursor = 0;
        for (OGPhase.LootEntry entry : entries) {
            cursor += entry.weight();
            if (pick < cursor) return entry;
        }
        return entries.isEmpty() ? null : entries.getFirst();
    }

    private long number(Object value, long fallback) {
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private double decimal(Object value, double fallback) {
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private OGPhase defaultPhase() {
        return new OGPhase("plains", "Plains", 0L,
                List.of(new OGPhase.BlockEntry(Material.DIRT, 10),
                        new OGPhase.BlockEntry(Material.STONE, 10),
                        new OGPhase.BlockEntry(Material.OAK_LOG, 3)),
                List.of(EntityType.CHICKEN, EntityType.COW),
                List.of(EntityType.ZOMBIE),
                List.of(new OGPhase.LootEntry(Material.WHEAT_SEEDS, 2, 5, 4),
                        new OGPhase.LootEntry(Material.WATER_BUCKET, 1, 1, 1)),
                0.02D,
                0.015D);
    }
}
