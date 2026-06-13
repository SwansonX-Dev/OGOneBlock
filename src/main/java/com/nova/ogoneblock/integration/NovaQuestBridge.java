package com.nova.ogoneblock.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Reports OG OneBlock activity to NovaBlock's daily-quest system when NovaBlock
 * is installed. Uses reflection so OG OneBlock keeps zero compile-time
 * dependency on NovaBlock (the two plugins are built separately); every call
 * degrades silently when NovaBlock is absent, disabled, or has changed its API.
 */
public final class NovaQuestBridge {

    private static Method method;
    private static Plugin novaBlock;

    private NovaQuestBridge() {}

    /** Record {@code amount} of activity {@code key} (e.g. "og_break") for the player. */
    public static void record(Player player, String key, int amount) {
        if (player == null || amount <= 0) return;
        try {
            if (method == null || novaBlock == null || !novaBlock.isEnabled()) resolve();
            if (method == null || novaBlock == null || !novaBlock.isEnabled()) return;
            method.invoke(novaBlock, player, key, amount);
        } catch (Throwable ignored) {
            // Never let an integration hiccup disrupt OG gameplay.
        }
    }

    private static void resolve() {
        Plugin nb = Bukkit.getPluginManager().getPlugin("NovaBlock");
        if (nb == null) { novaBlock = null; method = null; return; }
        try {
            method = nb.getClass().getMethod("onExternalActivity", Player.class, String.class, int.class);
            novaBlock = nb;
        } catch (NoSuchMethodException e) {
            method = null;
            novaBlock = null;
        }
    }
}
