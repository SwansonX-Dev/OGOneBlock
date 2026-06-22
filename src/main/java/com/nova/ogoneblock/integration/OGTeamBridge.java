package com.nova.ogoneblock.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Coop access bridge to the xTeams plugin. Lets OG OneBlock treat a player's
 * xTeams teammates as trusted builders on each other's islands, without a
 * compile-time dependency on xTeams (the two plugins are built separately).
 *
 * <p>Reflection-only, mirroring {@link NovaQuestBridge}: every call degrades to
 * {@code false} when xTeams is absent, disabled, or has changed its API, so OG
 * gameplay simply falls back to owner-only access.
 */
public final class OGTeamBridge {

    private static Plugin xteams;
    private static Method teamsAccessor; // XTeamsPlugin#teams() -> TeamService
    private static Method areTeammates;  // TeamService#areTeammates(UUID, UUID)

    private OGTeamBridge() {}

    /** True only when {@code a} and {@code b} are distinct members of the same xTeams team. */
    public static boolean areTeammates(UUID a, UUID b) {
        if (a == null || b == null || a.equals(b)) return false;
        try {
            if (!ready()) resolve();
            if (!ready()) return false;
            Object service = teamsAccessor.invoke(xteams);
            if (service == null) return false;
            return (boolean) areTeammates.invoke(service, a, b);
        } catch (Throwable ignored) {
            // Never let an integration hiccup disrupt OG gameplay.
            return false;
        }
    }

    private static boolean ready() {
        return xteams != null && xteams.isEnabled() && teamsAccessor != null && areTeammates != null;
    }

    private static void resolve() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("xTeams");
        if (plugin == null) { reset(); return; }
        try {
            Method accessor = plugin.getClass().getMethod("teams");
            Object service = accessor.invoke(plugin);
            if (service == null) { reset(); return; }
            Method check = service.getClass().getMethod("areTeammates", UUID.class, UUID.class);
            xteams = plugin;
            teamsAccessor = accessor;
            areTeammates = check;
        } catch (Throwable t) {
            reset();
        }
    }

    private static void reset() {
        xteams = null;
        teamsAccessor = null;
        areTeammates = null;
    }
}
