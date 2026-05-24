package com.nova.ogoneblock.island;

import com.nova.ogoneblock.OGOneBlockPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OGIslandManager {

    private final OGOneBlockPlugin plugin;
    private final OGIslandStorage storage;
    private final Map<UUID, OGIsland> byOwner = new HashMap<>();
    private final Map<UUID, OGIsland> byId = new HashMap<>();
    private int nextSlotX;
    private int nextSlotZ;

    public OGIslandManager(OGOneBlockPlugin plugin, OGIslandStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void loadAll() {
        byOwner.clear();
        byId.clear();
        for (OGIslandData data : storage.loadAll()) {
            plugin.worlds().ensureWorld(data.worldName());
            register(new OGIsland(plugin, data));
        }
        recalculateNextSlot();
    }

    public void saveAll() {
        for (OGIsland island : byId.values()) storage.save(island.data());
    }

    public OGIsland of(Player player) {
        return byOwner.get(player.getUniqueId());
    }

    public OGIsland at(Location location) {
        if (location.getWorld() == null) return null;
        int slotX = nearestSlot(location.getBlockX());
        int slotZ = nearestSlot(location.getBlockZ());
        for (OGIsland island : byId.values()) {
            if (!location.getWorld().getName().equals(island.data().worldName())) continue;
            if (island.data().slotX() == slotX && island.data().slotZ() == slotZ) return island;
        }
        return null;
    }

    public OGIsland getOrCreate(Player owner) {
        OGIsland existing = of(owner);
        if (existing != null) return existing;
        String worldName = plugin.worlds().worldName();
        plugin.worlds().ensureWorld(worldName);
        int[] slot = nextSlot();
        OGIslandData data = new OGIslandData(OGIsland.newId(), owner.getUniqueId(), worldName, slot[0], slot[1]);
        OGIsland island = new OGIsland(plugin, data);
        island.ensurePlatform();
        island.fillQueue(plugin.blocks().starterQueue());
        register(island);
        storage.save(data);
        return island;
    }

    public void save(OGIsland island) {
        storage.save(island.data());
    }

    public int count() {
        return byId.size();
    }

    private void register(OGIsland island) {
        byId.put(island.data().id(), island);
        byOwner.put(island.data().owner(), island);
    }

    private void recalculateNextSlot() {
        int max = 0;
        for (OGIsland island : byId.values()) {
            max = Math.max(max, Math.max(Math.abs(island.data().slotX()), Math.abs(island.data().slotZ())));
        }
        nextSlotX = max + 1;
        nextSlotZ = 0;
    }

    private int nearestSlot(int blockCoord) {
        int halfSlot = plugin.worlds().slotSize() / 2;
        return Math.floorDiv(blockCoord + halfSlot, plugin.worlds().slotSize());
    }

    private int[] nextSlot() {
        int[] slot = {nextSlotX, nextSlotZ};
        nextSlotZ++;
        if (nextSlotZ > nextSlotX) {
            nextSlotX++;
            nextSlotZ = 0;
        }
        return slot;
    }
}
