package com.blossomsmp.teams.commands;

import com.blossomsmp.teams.BlossomTeams;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/** /tc [message] - send one team message, or switch team chat on/off. */
public class TeamChatCommand implements TabExecutor {

    private final BlossomTeams plugin;

    public TeamChatCommand(BlossomTeams plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("&cOnly players can use team chat."));
            return true;
        }
        if (args.length == 0) {
            plugin.getActions().toggleChat(player);
        } else {
            plugin.getActions().teamChat(player, String.join(" ", args));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
