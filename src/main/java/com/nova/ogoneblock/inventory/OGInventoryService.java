package com.nova.ogoneblock.inventory;

import com.nova.ogoneblock.OGOneBlockPlugin;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/** Swaps a player's normal inventory with their OG-only inventory at the OG world boundary. */
public final class OGInventoryService {

    private final OGOneBlockPlugin plugin;
    private final File folder;

    public OGInventoryService(OGOneBlockPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "inventories");
    }

    public void enter(Player player) {
        if (isInOgMode(player)) return;
        saveSnapshot(player, "normal");
        loadSnapshot(player, "og", true);
        player.getPersistentDataContainer().set(plugin.ogInventoryKey(), org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        plugin.paxels().give(player);
    }

    public void leave(Player player) {
        if (!isInOgMode(player)) return;
        saveSnapshot(player, "og");
        loadSnapshot(player, "normal", false);
        player.getPersistentDataContainer().remove(plugin.ogInventoryKey());
    }

    public void saveCurrent(Player player) {
        saveSnapshot(player, isInOgMode(player) ? "og" : "normal");
    }

    public boolean isInOgMode(Player player) {
        return player.getPersistentDataContainer().has(plugin.ogInventoryKey(), org.bukkit.persistence.PersistentDataType.BYTE);
    }

    private void saveSnapshot(Player player, String key) {
        YamlConfiguration yaml = loadFile(player.getUniqueId());
        ConfigurationSection section = yaml.createSection(key);
        section.set("contents", player.getInventory().getContents());
        section.set("armor", player.getInventory().getArmorContents());
        section.set("extra", player.getInventory().getExtraContents());
        section.set("held-slot", player.getInventory().getHeldItemSlot());
        section.set("level", player.getLevel());
        section.set("exp", player.getExp());
        section.set("total-exp", player.getTotalExperience());
        section.set("food", player.getFoodLevel());
        section.set("saturation", player.getSaturation());
        section.set("health", Math.max(0.0D, player.getHealth()));
        section.set("gamemode", player.getGameMode().name());
        saveFile(player.getUniqueId(), yaml);
    }

    private void loadSnapshot(Player player, String key, boolean clearIfMissing) {
        YamlConfiguration yaml = loadFile(player.getUniqueId());
        ConfigurationSection section = yaml.getConfigurationSection(key);
        if (section == null) {
            if (clearIfMissing) clear(player);
            return;
        }
        player.getInventory().setContents(list(section, "contents", player.getInventory().getSize()));
        player.getInventory().setArmorContents(list(section, "armor", 4));
        player.getInventory().setExtraContents(list(section, "extra", player.getInventory().getExtraContents().length));
        player.getInventory().setHeldItemSlot(Math.max(0, Math.min(8, section.getInt("held-slot", 0))));
        player.setLevel(Math.max(0, section.getInt("level", 0)));
        player.setExp((float) Math.max(0.0D, Math.min(1.0D, section.getDouble("exp", 0.0D))));
        player.setTotalExperience(Math.max(0, section.getInt("total-exp", 0)));
        player.setFoodLevel(Math.max(0, Math.min(20, section.getInt("food", 20))));
        player.setSaturation((float) Math.max(0.0D, section.getDouble("saturation", 5.0D)));
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) == null
                ? 20.0D
                : player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.max(1.0D, Math.min(maxHealth, section.getDouble("health", maxHealth))));
        try {
            player.setGameMode(GameMode.valueOf(section.getString("gamemode", player.getGameMode().name())));
        } catch (IllegalArgumentException ignored) {
            // Keep current mode if the saved value is invalid.
        }
    }

    private void clear(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setExtraContents(new ItemStack[player.getInventory().getExtraContents().length]);
        player.setLevel(0);
        player.setExp(0.0F);
        player.setTotalExperience(0);
        player.setFoodLevel(20);
        player.setSaturation(5.0F);
    }

    private ItemStack[] list(ConfigurationSection section, String path, int size) {
        ItemStack[] out = new ItemStack[size];
        java.util.List<?> values = section.getList(path, java.util.List.of());
        for (int i = 0; i < Math.min(size, values.size()); i++) {
            if (values.get(i) instanceof ItemStack stack) out[i] = stack;
        }
        return out;
    }

    private YamlConfiguration loadFile(UUID playerId) {
        return YamlConfiguration.loadConfiguration(file(playerId));
    }

    private void saveFile(UUID playerId, YamlConfiguration yaml) {
        try {
            if (!folder.exists() && !folder.mkdirs()) {
                plugin.getLogger().warning("Could not create OG inventory folder.");
            }
            yaml.save(file(playerId));
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save OG inventory for " + playerId + ": " + e.getMessage());
        }
    }

    private File file(UUID playerId) {
        return new File(folder, playerId + ".yml");
    }
}
