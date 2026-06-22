package com.nova.ogoneblock.island;

import com.nova.ogoneblock.OGOneBlockPlugin;
import com.nova.ogoneblock.integration.OGTeamBridge;
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

    /**
     * The island this player belongs to: the one they own, or — when coop is on
     * and they own none — a teammate's island shared through xTeams. Used for
     * teleport, void-respawn and the sidebar so coop members resolve to the
     * shared island instead of nothing.
     */
    public OGIsland homeIsland(Player player) {
        OGIsland own = byOwner.get(player.getUniqueId());
        if (own != null) return own;
        if (!coopEnabled()) return null;
        for (OGIsland island : byId.values()) {
            if (OGTeamBridge.areTeammates(player.getUniqueId(), island.data().owner())) return island;
        }
        return null;
    }

    /**
     * Whether {@code player} may build/break on {@code island}: they own it, or
     * (with coop enabled) they share an xTeams team with the owner.
     */
    public boolean canUse(Player player, OGIsland island) {
        if (island == null) return false;
        UUID owner = island.data().owner();
        if (owner.equals(player.getUniqueId())) return true;
        return coopEnabled() && OGTeamBridge.areTeammates(player.getUniqueId(), owner);
    }

    private boolean coopEnabled() {
        return plugin.getConfig().getBoolean("coop.enabled", true);
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
