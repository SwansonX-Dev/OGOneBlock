package com.nova.ogoneblock.prestige;

import com.nova.ogoneblock.OGOneBlockPlugin;
import com.nova.ogoneblock.island.OGIsland;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

/**
 * Per-prestige stat modifiers, scoped to the OG world only. Modifiers are
 * attached with stable {@link NamespacedKey}s so re-applying is idempotent —
 * an existing modifier with the same key is replaced rather than stacked.
 */
public final class OGPrestigeBonuses {

    private final OGOneBlockPlugin plugin;
    private final NamespacedKey healthKey;

    public OGPrestigeBonuses(OGOneBlockPlugin plugin) {
        this.plugin = plugin;
        this.healthKey = new NamespacedKey(plugin, "prestige_health_bonus");
    }

    /** Re-derive and apply bonuses based on the player's current prestige level. */
    public void apply(Player player) {
        OGIsland island = plugin.islands().of(player);
        int level = island == null ? 0 : island.data().prestigeLevel();
        int cap = Math.max(0, plugin.getConfig().getInt("prestige.stat-bonus-cap", 10));
        int effective = Math.min(level, cap);
        double healthBonus = effective * plugin.getConfig().getDouble("prestige.max-health-per-level", 1.0);
        applyModifier(player, Attribute.MAX_HEALTH, healthKey, healthBonus);
    }

    /** Remove all prestige bonuses from the player. */
    public void clear(Player player) {
        removeModifier(player, Attribute.MAX_HEALTH, healthKey);
        // Clamp current health back to the (now possibly lower) max so we don't leave
        // the client showing more hearts than the attribute supports.
        double max = player.getAttribute(Attribute.MAX_HEALTH) == null
                ? 20.0
                : player.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (player.getHealth() > max) player.setHealth(max);
    }

    private void applyModifier(Player player, Attribute attribute, NamespacedKey key, double amount) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        inst.getModifiers().stream()
                .filter(m -> key.equals(m.getKey()))
                .toList()
                .forEach(inst::removeModifier);
        if (amount <= 0) return;
        inst.addModifier(new AttributeModifier(
                key, amount, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
    }

    private void removeModifier(Player player, Attribute attribute, NamespacedKey key) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        inst.getModifiers().stream()
                .filter(m -> key.equals(m.getKey()))
                .toList()
                .forEach(inst::removeModifier);
    }
}
