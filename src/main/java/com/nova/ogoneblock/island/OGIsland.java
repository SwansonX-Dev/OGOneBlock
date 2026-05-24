package com.nova.ogoneblock.island;

import com.nova.ogoneblock.OGOneBlockPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public final class OGIsland {

    private final OGOneBlockPlugin plugin;
    private final OGIslandData data;
    private final Deque<Material> upcoming = new ArrayDeque<>();

    public OGIsland(OGOneBlockPlugin plugin, OGIslandData data) {
        this.plugin = plugin;
        this.data = data;
    }

    public OGIslandData data() { return data; }
    public Deque<Material> upcoming() { return upcoming; }

    public Location centerBlock() {
        return data.centerBlock(plugin.worlds().slotSize(), plugin.worlds().centerY());
    }

    public void ensurePlatform() {
        Location center = centerBlock();
        if (center.getWorld() == null) return;
        if (center.getBlock().getType() == Material.AIR) center.getBlock().setType(Material.GRASS_BLOCK, false);
        center.clone().add(0, -1, 0).getBlock().setType(Material.BEDROCK, false);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                Location base = center.clone().add(dx, -1, dz);
                if (base.getBlock().getType() == Material.AIR) base.getBlock().setType(Material.BEDROCK, false);
            }
        }
    }

    public void fillQueue(int target) {
        while (upcoming.size() < target) upcoming.addLast(plugin.blocks().roll(data.blocksBroken()));
    }

    public Material nextBlock() {
        Material next = upcoming.pollFirst();
        if (next == null) next = plugin.blocks().roll();
        fillQueue(plugin.blocks().starterQueue());
        return next;
    }

    public void teleport(Player player) {
        Location spawn = data.spawnLocation(plugin.worlds().slotSize(), plugin.worlds().centerY());
        if (spawn.getWorld() == null) return;
        spawn.getChunk().load();
        player.teleportAsync(spawn);
    }

    public static UUID newId() {
        return UUID.randomUUID();
    }
}
