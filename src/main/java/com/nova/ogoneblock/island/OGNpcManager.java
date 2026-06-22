package com.nova.ogoneblock.island;

import com.nova.ogoneblock.OGOneBlockPlugin;
import com.nova.ogoneblock.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

public final class OGNpcManager {

    private final OGOneBlockPlugin plugin;
    private final NamespacedKey npcKey;
    private Entity npc;

    public OGNpcManager(OGOneBlockPlugin plugin) {
        this.plugin = plugin;
        this.npcKey = new NamespacedKey(plugin, "og_npc");
    }

    public void spawnConfigured() {
        Location location = unpack(plugin.getConfig().getString("npc.location"));
        if (location == null || location.getWorld() == null) return;
        spawn(location);
    }

    public void setLocation(Location location) {
        plugin.getConfig().set("npc.location", pack(location));
        plugin.saveConfig();
        spawn(location);
    }

    public boolean isNpc(Entity entity) {
        return entity.getPersistentDataContainer().has(npcKey, PersistentDataType.BYTE);
    }

    public void handle(Player player) {
        // Coop: a teammate who owns no island of their own enters the team's
        // shared island rather than spawning a separate one. Otherwise the
        // player gets (or creates) their own.
        OGIsland island = plugin.islands().homeIsland(player);
        boolean joiningTeammate = island != null && !island.data().owner().equals(player.getUniqueId());
        if (island == null) island = plugin.islands().getOrCreate(player);
        island.ensurePlatform();
        plugin.allowOgEntry(player);
        island.teleport(player);
        Text.send(player, joiningTeammate
                ? "<green>Joining your team's OG OneBlock island. Use <yellow>/spawn <green>to leave."
                : "<green>Sending you to your OG OneBlock island. Use <yellow>/spawn <green>to leave.");
    }

    public void shutdown() {
        if (npc != null && npc.isValid()) npc.remove();
    }

    private void spawn(Location location) {
        removeExisting();
        EntityType type = parseEntityType();
        Entity entity = location.getWorld().spawnEntity(location, type);
        entity.getPersistentDataContainer().set(npcKey, PersistentDataType.BYTE, (byte) 1);
        entity.customName(Text.mm(plugin.getConfig().getString("npc.name", "<gold><bold>OG OneBlock")));
        entity.setCustomNameVisible(true);
        entity.setPersistent(true);
        if (entity instanceof LivingEntity living) {
            living.setAI(false);
            living.setInvulnerable(true);
            living.setSilent(true);
            living.setRemoveWhenFarAway(false);
            living.getEquipment().setHelmet(new org.bukkit.inventory.ItemStack(Material.GRASS_BLOCK));
        }
        if (entity instanceof Villager villager) {
            villager.setProfession(Villager.Profession.NITWIT);
            villager.setVillagerType(Villager.Type.PLAINS);
        }
        npc = entity;
    }

    private EntityType parseEntityType() {
        String raw = plugin.getConfig().getString("npc.entity-type", "VILLAGER");
        if (raw == null) return EntityType.VILLAGER;
        try {
            EntityType type = EntityType.valueOf(raw.toUpperCase(Locale.ROOT));
            return type.isAlive() ? type : EntityType.VILLAGER;
        } catch (IllegalArgumentException ex) {
            return EntityType.VILLAGER;
        }
    }

    private void removeExisting() {
        if (npc != null && npc.isValid()) npc.remove();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (isNpc(entity)) entity.remove();
            }
        }
    }

    private String pack(Location location) {
        World world = location.getWorld();
        if (world == null) return "";
        return world.getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ()
                + "," + location.getYaw() + "," + location.getPitch();
    }

    private Location unpack(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String[] parts = raw.split(",");
        if (parts.length != 6) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            return new Location(world,
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Float.parseFloat(parts[4]),
                    Float.parseFloat(parts[5]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
