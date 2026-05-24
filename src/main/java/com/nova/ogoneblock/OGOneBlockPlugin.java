package com.nova.ogoneblock;

import com.nova.ogoneblock.command.OGOneBlockCommand;
import com.nova.ogoneblock.island.OGBlockTable;
import com.nova.ogoneblock.island.OGIslandManager;
import com.nova.ogoneblock.island.OGIslandStorage;
import com.nova.ogoneblock.island.OGNpcManager;
import com.nova.ogoneblock.island.OGWorldManager;
import com.nova.ogoneblock.inventory.OGInventoryService;
import com.nova.ogoneblock.listener.OGGameplayListener;
import com.nova.ogoneblock.paxel.OGPaxelManager;
import com.nova.ogoneblock.phase.OGPhaseManager;
import com.nova.ogoneblock.tag.MilestoneManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class OGOneBlockPlugin extends JavaPlugin {

    private OGWorldManager worldManager;
    private OGBlockTable blockTable;
    private OGIslandStorage islandStorage;
    private OGIslandManager islandManager;
    private OGNpcManager npcManager;
    private MilestoneManager milestoneManager;
    private OGGameplayListener gameplayListener;
    private OGInventoryService inventoryService;
    private OGPaxelManager paxelManager;
    private OGPhaseManager phaseManager;
    private NamespacedKey ogInventoryKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ogInventoryKey = new NamespacedKey(this, "og_inventory_active");

        worldManager = new OGWorldManager(this);
        worldManager.load();

        blockTable = new OGBlockTable(this);
        phaseManager = new OGPhaseManager(this);
        phaseManager.load();
        blockTable.load();

        islandStorage = new OGIslandStorage(this);
        islandStorage.init();

        islandManager = new OGIslandManager(this, islandStorage);
        islandManager.loadAll();

        milestoneManager = new MilestoneManager(this);
        milestoneManager.load();

        npcManager = new OGNpcManager(this);
        npcManager.spawnConfigured();

        paxelManager = new OGPaxelManager(this);
        inventoryService = new OGInventoryService(this);

        gameplayListener = new OGGameplayListener(this);
        getServer().getPluginManager().registerEvents(gameplayListener, this);

        OGOneBlockCommand command = new OGOneBlockCommand(this);
        getCommand("ogoneblock").setExecutor(command);
        getCommand("ogoneblock").setTabCompleter(command);

        getLogger().info("OGOneBlock enabled with " + islandManager.count()
                + " island(s) in " + worldManager.worldName() + ".");
    }

    @Override
    public void onDisable() {
        if (islandManager != null) islandManager.saveAll();
        if (npcManager != null) npcManager.shutdown();
    }

    public void reloadRuntime() {
        reloadConfig();
        worldManager.load();
        phaseManager.load();
        blockTable.load();
        milestoneManager.load();
        gameplayListener.reload();
        npcManager.spawnConfigured();
    }

    public void allowOgEntry(Player player) {
        gameplayListener.allowNextEntry(player.getUniqueId());
    }

    public OGWorldManager worlds() { return worldManager; }
    public OGBlockTable blocks() { return blockTable; }
    public OGIslandManager islands() { return islandManager; }
    public OGNpcManager npc() { return npcManager; }
    public MilestoneManager milestones() { return milestoneManager; }
    public OGInventoryService inventories() { return inventoryService; }
    public OGPaxelManager paxels() { return paxelManager; }
    public OGPhaseManager phases() { return phaseManager; }
    public NamespacedKey ogInventoryKey() { return ogInventoryKey; }
}
