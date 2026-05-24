package com.nova.ogoneblock.paxel;

import com.nova.ogoneblock.OGOneBlockPlugin;
import com.nova.ogoneblock.util.Text;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.TriState;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.BlockType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** OG-only fixed-tier paxel. It behaves as a universal tool but never levels up. */
public final class OGPaxelManager implements Listener {

    private static final int PAXEL_VERSION = 1;
    private static final Material MATERIAL = Material.IRON_PICKAXE;

    private final OGOneBlockPlugin plugin;
    private final NamespacedKey ownerKey;
    private final NamespacedKey versionKey;

    public OGPaxelManager(OGOneBlockPlugin plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "og_paxel_owner");
        this.versionKey = new NamespacedKey(plugin, "og_paxel_version");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void give(Player player) {
        PlayerInventory inv = player.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (isOwner(item, player)) return;
        }
        inv.addItem(build(player));
    }

    public boolean isPaxel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING);
    }

    public boolean isOwner(ItemStack item, Player player) {
        if (!isPaxel(item)) return false;
        String owner = item.getItemMeta().getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        return player.getUniqueId().toString().equals(owner);
    }

    private ItemStack build(Player owner) {
        ItemStack stack = new ItemStack(MATERIAL);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Text.mm("<gold><bold>OG Paxel <gray>(" + owner.getName() + ")")
                .decoration(TextDecoration.ITALIC, false));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(Text.mm("<gray>Shovel, axe, pickaxe, and hoe in one.").decoration(TextDecoration.ITALIC, false));
        lore.add(Text.mm("<gray>Fixed OG tier. This paxel does not level up.").decoration(TextDecoration.ITALIC, false));
        lore.add(Text.mm("").decoration(TextDecoration.ITALIC, false));
        lore.add(Text.mm("<gold>Soulbound to " + owner.getName()).decoration(TextDecoration.ITALIC, false));
        lore.add(Text.mm("<dark_gray>Only usable inside OG OneBlock.").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.EFFICIENCY, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        meta.getPersistentDataContainer().set(versionKey, PersistentDataType.INTEGER, PAXEL_VERSION);
        stack.setItemMeta(meta);

        List<Tool.Rule> rules = new ArrayList<>();
        rules.add(Tool.rule(blocksFromTag(Tag.MINEABLE_PICKAXE), 8.0F, TriState.TRUE));
        rules.add(Tool.rule(blocksFromTag(Tag.MINEABLE_AXE), 8.0F, TriState.TRUE));
        rules.add(Tool.rule(blocksFromTag(Tag.MINEABLE_SHOVEL), 8.0F, TriState.TRUE));
        rules.add(Tool.rule(blocksFromTag(Tag.MINEABLE_HOE), 8.0F, TriState.TRUE));
        stack.setData(DataComponentTypes.TOOL, Tool.tool()
                .addRules(rules)
                .defaultMiningSpeed(1.5F)
                .damagePerBlock(0)
                .build());
        return stack;
    }

    private static RegistryKeySet<BlockType> blocksFromTag(Tag<Material> tag) {
        List<TypedKey<BlockType>> keys = new ArrayList<>(tag.getValues().size());
        for (Material material : tag.getValues()) {
            keys.add(TypedKey.create(RegistryKey.BLOCK, material.getKey()));
        }
        return RegistrySet.keySet(RegistryKey.BLOCK, keys);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (!isPaxel(event.getItemDrop().getItemStack())) return;
        event.setCancelled(true);
        event.getPlayer().sendActionBar(Text.mm("<red>You can't drop the OG Paxel."));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isPaxel);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent event) {
        ItemStack stack = event.getItem().getItemStack();
        if (!isPaxel(stack)) return;
        if (event.getEntity() instanceof Player player && isOwner(stack, player)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        ItemStack moved = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean blocked = false;
        if (event.getClickedInventory() != null
                && event.getClickedInventory().getType() != InventoryType.PLAYER
                && event.getClickedInventory().getType() != InventoryType.CRAFTING) {
            blocked = isPaxel(cursor) || isPaxel(moved);
        }
        if (event.isShiftClick() && isPaxel(moved)
                && event.getView().getTopInventory().getType() != InventoryType.CRAFTING
                && event.getView().getTopInventory().getType() != InventoryType.PLAYER) {
            blocked = true;
        }
        if (blocked) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!isPaxel(event.getOldCursor())) return;
        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()
                    && event.getView().getTopInventory().getType() != InventoryType.PLAYER
                    && event.getView().getTopInventory().getType() != InventoryType.CRAFTING) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
