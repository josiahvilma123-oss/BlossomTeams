package com.blossomsmp.teams.commands;

import com.blossomsmp.teams.BlossomTeams;
import com.blossomsmp.teams.Team;
import com.blossomsmp.teams.TeamActions;
import com.blossomsmp.teams.TeamManager;
import com.blossomsmp.teams.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** /team and all its sub-commands. */
public class TeamCommand implements TabExecutor {

    private static final List<String> SUBS = Arrays.asList(
            "help", "create", "invite", "accept", "deny", "leave", "kick", "promote", "demote", "transfer",
            "home", "sethome", "delhome", "chat", "ff", "open", "color", "rename", "info", "top", "list",
            "bank", "deposit", "withdraw", "disband");

    private final BlossomTeams plugin;

    public TeamCommand(BlossomTeams plugin) {
        this.plugin = plugin;
    }

    private TeamManager teams() {
        return plugin.getTeams();
    }

    private TeamActions actions() {
        return plugin.getActions();
    }

    private void send(CommandSender sender, String text) {
        sender.sendMessage(plugin.msg(text));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            admin(sender, args);
            return true;
        }
        if (args.length > 0 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("top")
                || args[0].equalsIgnoreCase("list"))) {
            // These also work from the console
            if (args[0].equalsIgnoreCase("info")) {
                Team team = args.length > 1 ? teams().getByName(args[1])
                        : sender instanceof Player p ? teams().getTeam(p.getUniqueId()) : null;
                if (team == null && args.length < 2) {
                    send(sender, "&cYou're not in a team. Use &f/team info <team>&c.");
                } else {
                    actions().info(sender, team);
                }
            } else {
                actions().top(sender);
            }
            return true;
        }
        if (!(sender instanceof Player player)) {
            send(sender, "&cOnly players can use that. Console can use /team info, /team top and /team admin.");
            return true;
        }
        if (args.length == 0) {
            plugin.getMenus().open(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> help(player);
            case "menu", "gui" -> plugin.getMenus().open(player);
            case "create" -> {
                if (args.length < 2) {
                    send(player, "&cUse: /team create <name>");
                } else {
                    actions().create(player, args[1]);
                }
            }
            case "invite", "add" -> {
                if (args.length < 2) {
                    plugin.getMenus().openInvite(player, 0);
                } else {
                    Player target = Bukkit.getPlayerExact(args[1]);
                    if (target == null || !player.canSee(target)) {
                        send(player, "&c" + args[1] + " isn't online.");
                    } else {
                        actions().invite(player, target);
                    }
                }
            }
            case "accept", "join" -> {
                if (args.length < 2) {
                    plugin.getMenus().openInvites(player);
                } else {
                    actions().accept(player, args[1]);
                }
            }
            case "deny", "decline" -> {
                if (args.length < 2) {
                    send(player, "&cUse: /team deny <team>");
                } else {
                    actions().deny(player, args[1]);
                }
            }
            case "leave" -> actions().leave(player);
            case "kick", "promote", "demote", "transfer" -> memberCommand(player, sub, args);
            case "home" -> actions().home(player);
            case "sethome" -> actions().setHome(player);
            case "delhome" -> actions().delHome(player);
            case "chat", "c" -> {
                if (args.length < 2) {
                    actions().toggleChat(player);
                } else {
                    actions().teamChat(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                }
            }
            case "ff", "friendlyfire", "pvp" -> actions().toggleFriendlyFire(player);
            case "open", "close" -> actions().toggleOpen(player);
            case "color", "colour" -> plugin.getMenus().openColors(player);
            case "rename" -> {
                if (args.length < 2) {
                    send(player, "&cUse: /team rename <new name>");
                } else {
                    actions().rename(player, args[1]);
                }
            }
            case "bank" -> {
                if (plugin.getBank() == null) {
                    send(player, "&cThe team bank needs an economy plugin.");
                } else if (actions().requireTeam(player) != null) {
                    plugin.getMenus().openBank(player);
                }
            }
            case "deposit", "withdraw" -> {
                Double amount = args.length < 2 ? null : Text.parseAmount(args[1]);
                if (amount == null) {
                    send(player, "&cUse: /team " + sub + " <amount> &7(like 500, 2k, 1m)");
                } else if (sub.equals("deposit")) {
                    actions().deposit(player, amount);
                } else {
                    actions().withdraw(player, amount);
                }
            }
            case "disband" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("confirm")) {
                    actions().disband(player);
                } else if (actions().requireTeam(player) != null) {
                    Component button = Text.component("&c&l[CLICK TO DISBAND]")
                            .clickEvent(ClickEvent.runCommand("/team disband confirm"));
                    send(player, "&cAre you sure? This deletes the team for everyone.");
                    player.sendMessage(plugin.msg("").append(button));
                }
            }
            default -> help(player);
        }
        return true;
    }

    private void memberCommand(Player player, String sub, String[] args) {
        if (args.length < 2) {
            send(player, "&cUse: /team " + sub + " <player>");
            return;
        }
        Team team = actions().requireTeam(player);
        if (team == null) {
            return;
        }
        UUID target = team.findMember(args[1]);
        if (target == null) {
            send(player, "&c" + args[1] + " isn't in your team.");
            return;
        }
        switch (sub) {
            case "kick" -> actions().kick(player, target);
            case "promote" -> actions().promote(player, target);
            case "demote" -> actions().demote(player, target);
            default -> actions().transfer(player, target);
        }
    }

    private void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blossomteams.admin")) {
            send(sender, "&cYou don't have permission.");
            return;
        }
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "reload" -> {
                plugin.reloadConfig();
                send(sender, "&aBlossomTeams config reloaded.");
            }
            case "disband" -> {
                Team team = args.length > 2 ? teams().getByName(args[2]) : null;
                if (team == null) {
                    send(sender, "&cUse: /team admin disband <team>");
                } else {
                    actions().disbandTeam(team, "staff");
                    send(sender, "&aDisbanded " + team.getName() + ".");
                }
            }
            case "spy" -> {
                if (!(sender instanceof Player player)) {
                    send(sender, "&cTeam chat is already shown in the console.");
                } else if (!player.hasPermission("blossomteams.spy")) {
                    send(player, "&cYou don't have permission.");
                } else {
                    boolean on = plugin.toggleSpy(player.getUniqueId());
                    send(player, on ? "&aTeam chat spy ON." : "&7Team chat spy OFF.");
                }
            }
            case "setkills" -> {
                Team team = args.length > 3 ? teams().getByName(args[2]) : null;
                if (team == null || !args[3].matches("\\d+")) {
                    send(sender, "&cUse: /team admin setkills <team> <number>");
                } else {
                    team.setKills(Integer.parseInt(args[3]));
                    teams().markDirty();
                    send(sender, "&a" + team.getName() + " now has " + team.getKills() + " kills.");
                }
            }
            case "save" -> {
                teams().save();
                send(sender, "&aTeams saved.");
            }
            default -> {
                send(sender, "&#FF69B4&lTeam admin commands:");
                send(sender, "&f/team admin reload &7- reload config.yml");
                send(sender, "&f/team admin disband <team> &7- delete a team");
                send(sender, "&f/team admin setkills <team> <number> &7- change team kills");
                send(sender, "&f/team admin spy &7- see all team chats");
                send(sender, "&f/team admin save &7- save teams now");
            }
        }
    }

    private void help(Player player) {
        player.sendMessage(Text.component("&#FF1493&m                                "));
        player.sendMessage(Text.component("  &#FF69B4&l✿ Blossom Teams ✿"));
        line(player, "/team", "open the team menu");
        line(player, "/team create <name>", "make a team");
        line(player, "/team invite <player>", "invite someone");
        line(player, "/team accept <team>", "join a team");
        line(player, "/team leave", "leave your team");
        line(player, "/team home", "go to your team home");
        line(player, "/tc <message>", "talk in team chat");
        line(player, "/team kick|promote|demote <player>", "manage members");
        line(player, "/team deposit|withdraw <amount>", "team bank");
        line(player, "/team ff", "friendly fire on/off");
        line(player, "/team info [team]", "team info");
        line(player, "/team top", "best teams");
        player.sendMessage(Text.component("&#FF1493&m                                "));
    }

    private void line(Player player, String cmd, String what) {
        player.sendMessage(Text.component("  &#FFB6C1" + cmd + " &8- &7" + what)
                .clickEvent(ClickEvent.suggestCommand(cmd.split(" ")[0] + " ")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(SUBS);
            if (sender.hasPermission("blossomteams.admin")) {
                options.add("admin");
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            Player player = sender instanceof Player p ? p : null;
            Team team = player == null ? null : teams().getTeam(player.getUniqueId());
            switch (sub) {
                case "invite", "add" -> {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if ((player == null || player.canSee(online)) && teams().getTeam(online.getUniqueId()) == null) {
                            options.add(online.getName());
                        }
                    }
                }
                case "kick", "promote", "demote", "transfer" -> {
                    if (team != null) {
                        for (UUID uuid : team.getMembers().keySet()) {
                            if (player == null || !uuid.equals(player.getUniqueId())) {
                                options.add(team.nameOf(uuid));
                            }
                        }
                    }
                }
                case "accept", "join", "deny", "decline" -> {
                    if (player != null) {
                        for (Team t : teams().invitesFor(player.getUniqueId())) {
                            options.add(t.getName());
                        }
                        if (!sub.startsWith("de")) {
                            for (Team t : teams().all()) {
                                if (t.isOpen() && !options.contains(t.getName())) {
                                    options.add(t.getName());
                                }
                            }
                        }
                    }
                }
                case "info" -> {
                    for (Team t : teams().all()) {
                        options.add(t.getName());
                    }
                }
                case "disband" -> options.add("confirm");
                case "admin" -> {
                    if (sender.hasPermission("blossomteams.admin")) {
                        options.addAll(Arrays.asList("reload", "disband", "setkills", "spy", "save"));
                    }
                }
                default -> {
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")
                && (args[1].equalsIgnoreCase("disband") || args[1].equalsIgnoreCase("setkills"))
                && sender.hasPermission("blossomteams.admin")) {
            for (Team t : teams().all()) {
                options.add(t.getName());
            }
        }
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(last)) {
                result.add(option);
            }
        }
        return result;
    }
}
