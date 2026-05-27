package com.nova.ogoneblock.island;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class OGIslandData {

    private final UUID id;
    private final UUID owner;
    private final String worldName;
    private final int slotX;
    private final int slotZ;
    private long blocksBroken;
    private int prestigeLevel;
    private final Set<String> claimedMilestones = new HashSet<>();

    public OGIslandData(UUID id, UUID owner, String worldName, int slotX, int slotZ) {
        this.id = id;
        this.owner = owner;
        this.worldName = worldName;
        this.slotX = slotX;
        this.slotZ = slotZ;
    }

    public UUID id() { return id; }
    public UUID owner() { return owner; }
    public String worldName() { return worldName; }
    public int slotX() { return slotX; }
    public int slotZ() { return slotZ; }
    public long blocksBroken() { return blocksBroken; }
    public void blocksBroken(long blocksBroken) { this.blocksBroken = blocksBroken; }
    public void incrementBlocksBroken() { this.blocksBroken++; }
    public int prestigeLevel() { return prestigeLevel; }
    public void prestigeLevel(int prestigeLevel) { this.prestigeLevel = Math.max(0, prestigeLevel); }
    public Set<String> claimedMilestones() { return claimedMilestones; }

    /** Increment prestige and reset run-state. Caller persists. */
    public void prestige() {
        this.prestigeLevel++;
        this.blocksBroken = 0L;
        this.claimedMilestones.clear();
    }

    public Location centerBlock(int slotSize, int centerY) {
        World world = Bukkit.getWorld(worldName);
        return new Location(world, slotX * slotSize + 0.5, centerY, slotZ * slotSize + 0.5);
    }

    public Location spawnLocation(int slotSize, int centerY) {
        Location center = centerBlock(slotSize, centerY);
        return new Location(center.getWorld(), center.getX(), center.getY() + 1, center.getZ() + 1.5, 180f, 0f);
    }
}
