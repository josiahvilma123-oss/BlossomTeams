package com.blossomsmp.teams;

import com.blossomsmp.teams.commands.TeamChatCommand;
import com.blossomsmp.teams.commands.TeamCommand;
import com.blossomsmp.teams.hooks.BankService;
import com.blossomsmp.teams.listeners.ChatListener;
import com.blossomsmp.teams.listeners.CombatListener;
import com.blossomsmp.teams.listeners.MenuListener;
import com.blossomsmp.teams.listeners.PlayerListener;
import com.blossomsmp.teams.menus.MenuHolder;
import com.blossomsmp.teams.menus.Menus;
import com.blossomsmp.teams.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BlossomTeams - pink teams for Blossom SMP.
 * Team menu, invites, roles, team chat, team home, team bank, friendly fire and team kills.
 */
public final class BlossomTeams extends JavaPlugin {

    private TeamManager teams;
    private TeamActions actions;
    private Menus menus;
    private ChatPrompts prompts;
    private HomeTeleports homes;
    private BankService bank;
    private final Set<UUID> teamChat = ConcurrentHashMap.newKeySet();
    private final Set<UUID> spies = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        teams = new TeamManager(this);
        teams.load();
        actions = new TeamActions(this);
        menus = new Menus(this);
        prompts = new ChatPrompts(this);
        homes = new HomeTeleports(this);

        PluginManager pm = getServer().getPluginManager();
        if (pm.getPlugin("Vault") != null) {
            try {
                bank = new BankService();
                getLogger().info("Hooked into Vault (team bank is on).");
            } catch (Throwable t) {
                bank = null;
                getLogger().warning("Could not hook into Vault, the team bank is off: " + t);
            }
        } else {
            getLogger().info("Vault not found, the team bank is off.");
        }

        register("team", new TeamCommand(this));
        register("tc", new TeamChatCommand(this));

        pm.registerEvents(new MenuListener(this), this);
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new CombatListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(homes, this);

        // Save every 5 minutes if something changed
        getServer().getScheduler().runTaskTimer(this, teams::saveIfDirty, 20L * 300, 20L * 300);

        if (pm.getPlugin("PlaceholderAPI") != null) {
            try {
                new com.blossomsmp.teams.hooks.TeamPlaceholders(this).register();
                getLogger().info("Hooked into PlaceholderAPI (%blossomteams_...% placeholders).");
            } catch (Throwable t) {
                getLogger().warning("Could not register placeholders: " + t);
            }
        }
        getLogger().info("BlossomTeams is enabled. ❀");
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) {
                player.closeInventory();
            }
        }
        if (teams != null) {
            teams.save();
        }
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Command '" + name + "' is missing from plugin.yml!");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    /** A message with the BlossomTeams prefix. */
    public Component msg(String text) {
        return Text.component(getConfig().getString("prefix", "&#FF69B4&l❀ &8» &r") + text);
    }

    public TeamManager getTeams() {
        return teams;
    }

    public TeamActions getActions() {
        return actions;
    }

    public Menus getMenus() {
        return menus;
    }

    public ChatPrompts getPrompts() {
        return prompts;
    }

    public HomeTeleports getHomes() {
        return homes;
    }

    /** The money hook, or null when Vault isn't installed. */
    public BankService getBank() {
        return bank;
    }

    public boolean isTeamChat(UUID player) {
        return teamChat.contains(player);
    }

    public void setTeamChat(UUID player, boolean on) {
        if (on) {
            teamChat.add(player);
        } else {
            teamChat.remove(player);
        }
    }

    public Set<UUID> getSpies() {
        return spies;
    }

    /** Returns true if spy is now on. */
    public boolean toggleSpy(UUID player) {
        if (spies.remove(player)) {
            return false;
        }
        spies.add(player);
        return true;
    }
}
