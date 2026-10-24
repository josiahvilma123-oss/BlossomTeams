package com.blossomsmp.teams;

import com.blossomsmp.teams.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** /team home with a warmup (stand still) and a cooldown. */
public class HomeTeleports implements Listener {

    private final BlossomTeams plugin;
    private final Map<UUID, BukkitTask> pending = new HashMap<>();
    private final Map<UUID, Long> lastUse = new HashMap<>();

    public HomeTeleports(BlossomTeams plugin) {
        this.plugin = plugin;
    }

    public void start(Player player, Location destination) {
        UUID uuid = player.getUniqueId();
        if (destination == null) {
            player.sendMessage(plugin.msg("&cYour team has no home yet. The owner or an admin can set it with &f/team sethome&c."));
            return;
        }
        if (plugin.getConfig().getBoolean("home.block-in-combat", true) && inCombat(player)) {
            player.sendMessage(plugin.msg("&cYou can't go to your team home while in combat!"));
            return;
        }
        long cooldown = plugin.getConfig().getLong("home.cooldown-seconds", 30) * 1000L;
        Long last = lastUse.get(uuid);
        if (last != null && System.currentTimeMillis() - last < cooldown) {
            long left = (cooldown - (System.currentTimeMillis() - last) + 999) / 1000;
            player.sendMessage(plugin.msg("&cWait &f" + left + "s &cbefore using team home again."));
            return;
        }
        cancel(player, false);

        int warmup = plugin.getConfig().getInt("home.warmup-seconds", 5);
        if (warmup <= 0 || player.hasPermission("blossomteams.bypasswarmup")) {
            teleport(player, destination);
            return;
        }
        player.sendMessage(plugin.msg("&7Teleporting to your team home in &f" + warmup + "s&7. Don't move!"));
        BukkitTask task = new BukkitRunnable() {
            int left = warmup;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    pending.remove(uuid);
                    cancel();
                    return;
                }
                if (left <= 0) {
                    pending.remove(uuid);
                    cancel();
                    teleport(player, destination);
                    return;
                }
                player.sendActionBar(Text.component("&#FF69B4✿ &fTeleporting in &#FF69B4&l" + left + "s &#FF69B4✿"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.0f + (warmup - left) * 0.1f);
                left--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        pending.put(uuid, task);
    }

    private void teleport(Player player, Location destination) {
        lastUse.put(player.getUniqueId(), System.currentTimeMillis());
        player.teleportAsync(destination).thenAccept(success -> {
            if (success) {
                player.sendMessage(plugin.msg("&aWelcome to your team home!"));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
            }
        });
    }

    public void cancel(Player player, boolean tell) {
        BukkitTask task = pending.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            if (tell) {
                player.sendMessage(plugin.msg("&cTeleport cancelled."));
                player.sendActionBar(Text.component(""));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!pending.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            cancel(event.getPlayer(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && pending.containsKey(player.getUniqueId())) {
            cancel(player, true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer(), false);
    }

    /** Asks BlossomCombat (if installed) whether this player is in combat. */
    public static boolean inCombat(Player player) {
        try {
            org.bukkit.plugin.Plugin combat = Bukkit.getPluginManager().getPlugin("BlossomCombat");
            if (combat == null || !combat.isEnabled()) {
                return false;
            }
            Object manager = combat.getClass().getMethod("combat").invoke(combat);
            Object tagged = manager.getClass().getMethod("isTagged", Player.class).invoke(manager, player);
            return Boolean.TRUE.equals(tagged);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
