package com.blossomsmp.teams.listeners;

import com.blossomsmp.teams.BlossomTeams;
import com.blossomsmp.teams.Team;
import com.blossomsmp.teams.util.Text;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Friendly fire and team kill counting. */
public class CombatListener implements Listener {

    private final BlossomTeams plugin;
    private final Map<UUID, Long> lastWarning = new HashMap<>();

    public CombatListener(BlossomTeams plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = attackerOf(event.getDamager());
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        Team team = plugin.getTeams().getTeam(attacker.getUniqueId());
        if (team == null || team.isFriendlyFire() || !team.isMember(victim.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        long now = System.currentTimeMillis();
        Long last = lastWarning.get(attacker.getUniqueId());
        if (last == null || now - last > 1500) {
            lastWarning.put(attacker.getUniqueId(), now);
            attacker.sendActionBar(Text.component("&#FFB6C1✿ &f" + victim.getName()
                    + " &#FFB6C1is your teammate! &7(friendly fire is off)"));
        }
    }

    private Player attackerOf(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) {
            return player;
        }
        if (damager instanceof AreaEffectCloud cloud && cloud.getSource() instanceof Player player) {
            return player;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) {
            return;
        }
        Team team = plugin.getTeams().getTeam(killer.getUniqueId());
        if (team != null && !team.isMember(victim.getUniqueId())) {
            team.addKill();
            plugin.getTeams().markDirty();
        }
    }
}
