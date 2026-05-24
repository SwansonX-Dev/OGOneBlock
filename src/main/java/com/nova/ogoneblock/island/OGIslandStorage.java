package com.nova.ogoneblock.island;

import com.nova.ogoneblock.OGOneBlockPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class OGIslandStorage {

    private final OGOneBlockPlugin plugin;
    private File islandDir;

    public OGIslandStorage(OGOneBlockPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        islandDir = new File(plugin.getDataFolder(), "islands");
        if (!islandDir.exists()) islandDir.mkdirs();
    }

    public Collection<OGIslandData> loadAll() {
        List<OGIslandData> result = new ArrayList<>();
        File[] files = islandDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return result;
        for (File file : files) {
            try {
                YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
                OGIslandData data = new OGIslandData(
                        UUID.fromString(y.getString("id")),
                        UUID.fromString(y.getString("owner")),
                        y.getString("world", plugin.worlds().worldName()),
                        y.getInt("slot.x"),
                        y.getInt("slot.z"));
                data.blocksBroken(y.getLong("blocksBroken", 0L));
                data.claimedMilestones().addAll(y.getStringList("claimedMilestones"));
                result.add(data);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load OG island " + file.getName() + ": " + ex.getMessage());
            }
        }
        return result;
    }

    public void save(OGIslandData data) {
        File file = new File(islandDir, data.id() + ".yml");
        YamlConfiguration y = new YamlConfiguration();
        y.set("id", data.id().toString());
        y.set("owner", data.owner().toString());
        y.set("world", data.worldName());
        y.set("slot.x", data.slotX());
        y.set("slot.z", data.slotZ());
        y.set("blocksBroken", data.blocksBroken());
        y.set("claimedMilestones", new ArrayList<>(data.claimedMilestones()));
        try {
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save OG island " + data.id() + ": " + ex.getMessage());
        }
    }
}
