package com.nova.ogoneblock.phase;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.List;

public record OGPhase(String id,
                      String display,
                      long startBlocks,
                      List<BlockEntry> blocks,
                      List<EntityType> passiveSpawns,
                      List<EntityType> hostileSpawns,
                      List<LootEntry> loot,
                      double passiveChance,
                      double hostileChance) {

    public record BlockEntry(Material material, int weight) {}
    public record LootEntry(Material material, int min, int max, int weight) {}
}
