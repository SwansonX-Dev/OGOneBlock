package com.nova.ogoneblock;

import com.nova.ogoneblock.command.OGOneBlockCommand;
import com.nova.ogoneblock.island.OGBlockTable;
import com.nova.ogoneblock.island.OGIslandManager;
import com.nova.ogoneblock.island.OGIslandStorage;
import com.nova.ogoneblock.island.OGNpcManager;
import com.nova.ogoneblock.island.OGWorldManager;
import com.nova.ogoneblock.listener.OGGameplayListener;
import com.nova.ogoneblock.tag.MilestoneManager;
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

    @Override
    public void onEnable() {
        saveDefaultConfig();

        worldManager = new OGWorldManager(this);
        worldManager.load();

        blockTable = new OGBlockTable(this);
        blockTable.load();

        islandStorage = new OGIslandStorage(this);
        islandStorage.init();

        islandManager = new OGIslandManager(this, islandStorage);
        islandManager.loadAll();

        milestoneManager = new MilestoneManager(this);
        milestoneManager.load();

        npcManager = new OGNpcManager(this);
        npcManager.spawnConfigured();

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
}
