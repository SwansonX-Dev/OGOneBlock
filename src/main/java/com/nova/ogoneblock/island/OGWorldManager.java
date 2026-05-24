package com.nova.ogoneblock.island;

import com.nova.ogoneblock.OGOneBlockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.List;
import java.util.Random;

public final class OGWorldManager {

    private final OGOneBlockPlugin plugin;
    private String worldName;
    private int slotSize;
    private int centerY;
    private World world;

    public OGWorldManager(OGOneBlockPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        worldName = plugin.getConfig().getString("world.name", "OGOBworld");
        if (worldName == null || worldName.isBlank()) worldName = "OGOBworld";
        slotSize = Math.max(128, plugin.getConfig().getInt("world.slot-size", 256));
        centerY = Math.max(32, Math.min(250, plugin.getConfig().getInt("world.center-y", 80)));
        world = ensureWorld(worldName);
    }

    public World ensureWorld(String name) {
        World existing = Bukkit.getWorld(name);
        if (existing != null) return existing;
        World created = new WorldCreator(name)
                .generator(new VoidGenerator())
                .biomeProvider(new SingleBiomeProvider(Biome.PLAINS))
                .generateStructures(false)
                .createWorld();
        if (created != null) configure(created);
        return created;
    }

    private void configure(World world) {
        world.setSpawnFlags(true, true);
        world.setDifficulty(Difficulty.HARD);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, true);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        world.setGameRule(GameRule.KEEP_INVENTORY, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, true);
        world.setGameRule(GameRule.MOB_GRIEFING, true);
    }

    public String worldName() { return worldName; }
    public int slotSize() { return slotSize; }
    public int centerY() { return centerY; }
    public World world() { return world; }

    public static final class VoidGenerator extends ChunkGenerator {
        @Override public void generateNoise(WorldInfo w, Random r, int cx, int cz, ChunkData d) {}
        @Override public void generateSurface(WorldInfo w, Random r, int cx, int cz, ChunkData d) {}
        @Override public void generateBedrock(WorldInfo w, Random r, int cx, int cz, ChunkData d) {}
        @Override public void generateCaves(WorldInfo w, Random r, int cx, int cz, ChunkData d) {}
        @Override public boolean shouldGenerateNoise() { return false; }
        @Override public boolean shouldGenerateSurface() { return false; }
        @Override public boolean shouldGenerateBedrock() { return false; }
        @Override public boolean shouldGenerateCaves() { return false; }
        @Override public boolean shouldGenerateDecorations() { return false; }
        @Override public boolean shouldGenerateMobs() { return false; }
        @Override public boolean shouldGenerateStructures() { return false; }
    }

    public static final class SingleBiomeProvider extends BiomeProvider {
        private final Biome biome;
        public SingleBiomeProvider(Biome biome) { this.biome = biome; }
        @Override public Biome getBiome(WorldInfo w, int x, int y, int z) { return biome; }
        @Override public List<Biome> getBiomes(WorldInfo w) { return List.of(biome); }
    }
}
