package com.blossomsmp.teams.listeners;

import com.blossomsmp.teams.BlossomTeams;
import com.blossomsmp.teams.Team;
import com.blossomsmp.teams.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Keeps member names up to date and tells teammates when someone joins. */
public class PlayerListener implements Listener {

    private final BlossomTeams plugin;

    public PlayerListener(BlossomTeams plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Team team = plugin.getTeams().getTeam(player.getUniqueId());
        if (team != null) {
            if (!player.getName().equals(team.nameOf(player.getUniqueId()))) {
                team.updateName(player.getUniqueId(), player.getName());
                plugin.getTeams().markDirty();
            }
            if (plugin.getConfig().getBoolean("notify-teammate-join", true)) {
                for (Player mate : team.online()) {
                    if (!mate.equals(player) && mate.canSee(player)) {
                        mate.sendMessage(Text.component("&#FF69B4✿ &fYour teammate &#FFB6C1" + player.getName()
                                + " &fis online!"));
                    }
                }
            }
            return;
        }
        int invites = plugin.getTeams().invitesFor(player.getUniqueId()).size();
        if (invites > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(plugin.msg("&7You have &f" + invites + " &7team invite"
                            + (invites == 1 ? "" : "s") + ". Open &f/team &7to see."));
                }
            }, 60L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPrompts().clear(event.getPlayer().getUniqueId());
    }
}
