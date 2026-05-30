package com.nova.ogoneblock.listener;

import com.nova.ogoneblock.OGOneBlockPlugin;
import com.nova.ogoneblock.island.OGIsland;
import com.nova.ogoneblock.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class OGGameplayListener implements Listener {

    private final OGOneBlockPlugin plugin;
    private Set<String> allowedCommands = Set.of();
    private final Set<UUID> allowedEntries = new HashSet<>();

    public OGGameplayListener(OGOneBlockPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        Set<String> allowed = new HashSet<>();
        for (String command : plugin.getConfig().getStringList("gameplay.allowed-commands")) {
            String clean = command.toLowerCase(Locale.ROOT).replace("/", "").trim().replaceAll("\\s+", " ");
            if (!clean.isBlank()) allowed.add(clean);
        }
        allowedCommands = allowed;
    }

    public void allowNextEntry(UUID playerId) {
        allowedEntries.add(playerId);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> allowedEntries.remove(playerId), 100L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!inOgWorld(player)) return;
        OGIsland island = plugin.islands().at(event.getBlock().getLocation());
        if (!canUseIsland(player, island)) {
            event.setCancelled(true);
            Text.send(player, "<red>This is not your OG island.");
            return;
        }
        Location center = island.centerBlock();
        Location block = event.getBlock().getLocation();
        if (block.getBlockX() == center.getBlockX()
                && block.getBlockZ() == center.getBlockZ()
                && block.getBlockY() == center.getBlockY() - 1) {
            event.setCancelled(true);
            return;
        }
        if (block.getBlockX() != center.getBlockX()
                || block.getBlockZ() != center.getBlockZ()
                || block.getBlockY() != center.getBlockY()) {
            return;
        }
        long before = island.data().blocksBroken();
        island.data().incrementBlocksBroken();
        long count = island.data().blocksBroken();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Material next = island.nextBlock();
            center.getBlock().setType(next, false);
            if (next == Material.CHEST) {
                plugin.phases().fillLootChest(island, center.getBlock());
                // Follow-up next tick — the chest's tile entity may not be ready on the
                // same tick as setType, especially on Bedrock/Geyser. fillLootChest is
                // idempotent (PDC marker), so the second call no-ops if the first worked.
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> plugin.phases().fillLootChest(island, center.getBlock()));
            }
            island.ensurePlatform();
            plugin.islands().save(island);
            plugin.phases().maybeAnnouncePhase(player, before, count);
            plugin.phases().rollSpawn(player, island);
            plugin.milestones().check(player, island);
            if (count % 100 == 0) {
                Text.send(player, "<gray>OG blocks broken: <yellow>" + count);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!inOgWorld(event.getPlayer())) return;
        OGIsland island = plugin.islands().at(event.getBlockPlaced().getLocation());
        if (!canUseIsland(event.getPlayer(), island)) {
            event.setCancelled(true);
            Text.send(event.getPlayer(), "<red>This is not your OG island.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!inOgWorld(player) || player.hasPermission("ogoneblock.command.bypass")) return;
        String[] parts = event.getMessage().substring(1).split("\\s+", 3);
        String first = parts[0].toLowerCase(Locale.ROOT);
        int colon = first.indexOf(':');
        String label = colon >= 0 ? first.substring(colon + 1) : first;
        if (allowedCommands.contains(label)) return;
        if (parts.length >= 2) {
            String sub = parts[1].toLowerCase(Locale.ROOT);
            if (allowedCommands.contains(label + " " + sub)) return;
        }
        event.setCancelled(true);
        Text.send(player, "<red>That command is disabled on OG OneBlock. Use <yellow>/spawn <red>to leave.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNpcLeftClick(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!plugin.npc().isNpc(event.getEntity())) return;
        event.setCancelled(true);
        plugin.npc().handle(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        if (!plugin.npc().isNpc(event.getRightClicked())) return;
        event.setCancelled(true);
        plugin.npc().handle(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (!inOgWorld(event.getEntity())) return;
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equals(plugin.worlds().worldName())) return;
        OGIsland island = plugin.islands().of(player);
        if (island == null) return;
        Location spawn = island.data().spawnLocation(plugin.worlds().slotSize(), plugin.worlds().centerY());
        if (spawn.getWorld() == null) return;
        spawn.getChunk().load();
        event.setRespawnLocation(spawn);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null || event.getTo().getWorld() == null) return;
        boolean fromOg = event.getFrom().getWorld() != null
                && event.getFrom().getWorld().getName().equals(plugin.worlds().worldName());
        boolean toOg = event.getTo().getWorld().getName().equals(plugin.worlds().worldName());
        if (fromOg && !toOg) {
            plugin.inventories().leave(event.getPlayer());
            return;
        }
        if (!toOg || fromOg) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("ogoneblock.command.bypass") && !allowedEntries.remove(player.getUniqueId())) {
            event.setCancelled(true);
            Text.send(player, "<red>Use the OG OneBlock NPC at spawn to enter hard mode.");
            return;
        }
        plugin.inventories().enter(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!inOgWorld(player)) return;
        player.getPersistentDataContainer().set(plugin.ogInventoryKey(), org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        org.bukkit.World main = plugin.getServer().getWorlds().isEmpty() ? null : plugin.getServer().getWorlds().getFirst();
        if (main == null) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> player.teleportAsync(main.getSpawnLocation()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.inventories().saveCurrent(event.getPlayer());
    }

    private boolean inOgWorld(Player player) {
        return player.getWorld().getName().equals(plugin.worlds().worldName());
    }

    private boolean canUseIsland(Player player, OGIsland island) {
        return island != null && island.data().owner().equals(player.getUniqueId());
    }
}
