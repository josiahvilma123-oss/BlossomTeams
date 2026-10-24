package com.blossomsmp.teams;

import com.blossomsmp.teams.hooks.BankService;
import com.blossomsmp.teams.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyFormat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Everything players can do with teams. Both the commands and the menus call these,
 * so they always behave the same way and check the same permissions.
 */
public class TeamActions {

    private final BlossomTeams plugin;

    public TeamActions(BlossomTeams plugin) {
        this.plugin = plugin;
    }

    private TeamManager teams() {
        return plugin.getTeams();
    }

    private void send(CommandSender to, String text) {
        to.sendMessage(plugin.msg(text));
    }

    private void sound(Player player, Sound sound, float pitch) {
        player.playSound(player.getLocation(), sound, 0.8f, pitch);
    }

    private void fail(Player player, String text) {
        send(player, "&c" + text);
        sound(player, Sound.ENTITY_VILLAGER_NO, 1.0f);
    }

    private void teamMessage(Team team, String text) {
        team.broadcast(plugin.msg("&#FF69B4[" + team.getName() + "] &r" + text));
    }

    /** The player's team, or null (and a message) if they don't have one. */
    public Team requireTeam(Player player) {
        Team team = teams().getTeam(player.getUniqueId());
        if (team == null) {
            fail(player, "You're not in a team. Make one with /team create <name> or open /team.");
        }
        return team;
    }

    private boolean requireRole(Player player, Team team, Team.Role needed) {
        Team.Role role = team.getRole(player.getUniqueId());
        if (role == null || !role.atLeast(needed)) {
            fail(player, needed == Team.Role.OWNER ? "Only the team owner can do that."
                    : "Only the team owner and admins can do that.");
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Create, join, leave
    // ------------------------------------------------------------------

    public boolean create(Player player, String name) {
        if (teams().getTeam(player.getUniqueId()) != null) {
            fail(player, "You're already in a team. Leave it first.");
            return false;
        }
        String error = teams().checkName(name, null);
        if (error != null) {
            fail(player, error);
            return false;
        }
        double cost = plugin.getConfig().getDouble("teams.create-cost", 0);
        BankService bank = plugin.getBank();
        if (cost > 0 && bank != null && bank.isReady()) {
            if (!bank.has(player, cost)) {
                fail(player, "Making a team costs " + Text.money(cost) + ".");
                return false;
            }
            bank.take(player, cost);
        }
        Team team = teams().create(player.getUniqueId(), player.getName(), name);
        send(player, "&aYou made the team " + team.colored() + "&a! Invite friends with &f/team invite <player>&a.");
        sound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.2f);
        if (plugin.getConfig().getBoolean("teams.announce-new-teams", true)) {
            Bukkit.broadcast(plugin.msg("&f" + player.getName() + " &7started a new team: " + team.colored() + "&7!"));
        }
        return true;
    }

    public void invite(Player player, Player target) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        boolean membersCanInvite = plugin.getConfig().getBoolean("teams.members-can-invite", false);
        if (!membersCanInvite && !requireRole(player, team, Team.Role.ADMIN)) {
            return;
        }
        if (target == null || !target.isOnline()) {
            fail(player, "That player isn't online.");
            return;
        }
        if (target.equals(player)) {
            fail(player, "You can't invite yourself.");
            return;
        }
        if (teams().getTeam(target.getUniqueId()) != null) {
            fail(player, target.getName() + " is already in a team.");
            return;
        }
        if (team.size() >= teams().maxMembers()) {
            fail(player, "Your team is full (" + teams().maxMembers() + " players max).");
            return;
        }
        if (teams().hasInvite(target.getUniqueId(), team)) {
            fail(player, target.getName() + " already has an invite from your team.");
            return;
        }
        teams().invite(team, target.getUniqueId());
        teamMessage(team, "&f" + player.getName() + " &7invited &f" + target.getName() + "&7.");
        sound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f);

        long seconds = plugin.getConfig().getLong("teams.invite-expire-seconds", 120);
        Component accept = Text.component("&a&l[ACCEPT]")
                .clickEvent(ClickEvent.runCommand("/team accept " + team.getName()))
                .hoverEvent(HoverEvent.showText(Text.component("&aClick to join " + team.colored())));
        Component deny = Text.component("&c&l[DENY]")
                .clickEvent(ClickEvent.runCommand("/team deny " + team.getName()))
                .hoverEvent(HoverEvent.showText(Text.component("&cClick to say no")));
        target.sendMessage(plugin.msg("&f" + player.getName() + " &7invited you to join " + team.colored()
                + "&7! &8(" + seconds + "s)"));
        target.sendMessage(plugin.msg("").append(accept).append(Component.text("  ")).append(deny));
        sound(target, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f);
    }

    /** Join a team you were invited to (or an open team). */
    public boolean accept(Player player, String teamName) {
        if (teams().getTeam(player.getUniqueId()) != null) {
            fail(player, "You're already in a team.");
            return false;
        }
        Team team = teams().getByName(teamName);
        if (team == null) {
            fail(player, "There's no team called " + teamName + ".");
            return false;
        }
        if (!teams().hasInvite(player.getUniqueId(), team) && !team.isOpen()) {
            fail(player, team.getName() + " is invite only, and your invite has run out or doesn't exist.");
            return false;
        }
        if (team.size() >= teams().maxMembers()) {
            fail(player, team.getName() + " is full.");
            return false;
        }
        teams().addMember(team, player.getUniqueId(), player.getName());
        teamMessage(team, "&f" + player.getName() + " &ajoined the team! &7(" + team.size() + "/"
                + teams().maxMembers() + ")");
        for (Player member : team.online()) {
            sound(member, Sound.ENTITY_PLAYER_LEVELUP, 1.5f);
        }
        return true;
    }

    public void deny(Player player, String teamName) {
        Team team = teams().getByName(teamName);
        if (team == null || !teams().hasInvite(player.getUniqueId(), team)) {
            fail(player, "You don't have an invite from that team.");
            return;
        }
        teams().removeInvite(player.getUniqueId(), team);
        send(player, "&7You said no to " + team.colored() + "&7.");
        teamMessage(team, "&f" + player.getName() + " &7said no to the invite.");
    }

    public void leave(Player player) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        if (team.getRole(player.getUniqueId()) == Team.Role.OWNER) {
            if (team.size() == 1) {
                disband(player);
            } else {
                fail(player, "You're the owner. Give the team away with /team transfer <player> first, or disband it.");
            }
            return;
        }
        teams().removeMember(team, player.getUniqueId());
        plugin.setTeamChat(player.getUniqueId(), false);
        send(player, "&7You left " + team.colored() + "&7.");
        teamMessage(team, "&f" + player.getName() + " &cleft the team.");
    }

    public void disband(Player player) {
        Team team = requireTeam(player);
        if (team == null || !requireRole(player, team, Team.Role.OWNER)) {
            return;
        }
        disbandTeam(team, player.getName());
    }

    /** Removes a team. The bank money goes to the owner. */
    public void disbandTeam(Team team, String byWho) {
        BankService bank = plugin.getBank();
        UUID owner = team.getOwner();
        if (team.getBank() > 0 && owner != null && bank != null && bank.isReady()) {
            bank.give(Bukkit.getOfflinePlayer(owner), team.getBank());
            Player ownerPlayer = Bukkit.getPlayer(owner);
            if (ownerPlayer != null) {
                send(ownerPlayer, "&aThe team bank (" + Text.money(team.getBank()) + ") was given to you.");
            }
        }
        teamMessage(team, "&cThe team was disbanded by " + byWho + ".");
        for (UUID member : team.getMembers().keySet()) {
            plugin.setTeamChat(member, false);
        }
        teams().disband(team);
    }

    // ------------------------------------------------------------------
    // Members
    // ------------------------------------------------------------------

    public void kick(Player player, UUID target) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        Team.Role mine = team.getRole(player.getUniqueId());
        Team.Role theirs = team.getRole(target);
        if (theirs == null) {
            fail(player, "That player isn't in your team.");
            return;
        }
        if (target.equals(player.getUniqueId())) {
            fail(player, "Use /team leave to leave.");
            return;
        }
        boolean allowed = mine == Team.Role.OWNER || (mine == Team.Role.ADMIN && theirs == Team.Role.MEMBER);
        if (!allowed) {
            fail(player, "You can't kick that player.");
            return;
        }
        String name = team.nameOf(target);
        teams().removeMember(team, target);
        plugin.setTeamChat(target, false);
        teamMessage(team, "&f" + name + " &cwas kicked by " + player.getName() + ".");
        Player kicked = Bukkit.getPlayer(target);
        if (kicked != null) {
            send(kicked, "&cYou were kicked from " + team.colored() + "&c.");
        }
    }

    public void promote(Player player, UUID target) {
        Team team = requireTeam(player);
        if (team == null || !requireRole(player, team, Team.Role.OWNER)) {
            return;
        }
        Team.Role theirs = team.getRole(target);
        if (theirs == null) {
            fail(player, "That player isn't in your team.");
            return;
        }
        if (theirs != Team.Role.MEMBER) {
            fail(player, team.nameOf(target) + " is already an admin. Use transfer to make them owner.");
            return;
        }
        team.setRole(target, Team.Role.ADMIN);
        teams().markDirty();
        teamMessage(team, "&f" + team.nameOf(target) + " &ais now a team admin &e★");
    }

    public void demote(Player player, UUID target) {
        Team team = requireTeam(player);
        if (team == null || !requireRole(player, team, Team.Role.OWNER)) {
            return;
        }
        if (team.getRole(target) != Team.Role.ADMIN) {
            fail(player, "Only admins can be demoted.");
            return;
        }
        team.setRole(target, Team.Role.MEMBER);
        teams().markDirty();
        teamMessage(team, "&f" + team.nameOf(target) + " &7is now a normal member.");
    }

    public void transfer(Player player, UUID target) {
        Team team = requireTeam(player);
        if (team == null || !requireRole(player, team, Team.Role.OWNER)) {
            return;
        }
        if (target.equals(player.getUniqueId()) || !team.isMember(target)) {
            fail(player, "Pick another player in your team.");
            return;
        }
        team.setRole(player.getUniqueId(), Team.Role.ADMIN);
        team.setRole(target, Team.Role.OWNER);
        teams().markDirty();
        teamMessage(team, "&f" + team.nameOf(target) + " &dis the new team owner &6♛");
    }

    // ------------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------------

    public void setHome(Player player) {
        Team team = requireTeam(player);
        if (team == null || !requireRole(player, team, Team.Role.ADMIN)) {
            return;
        }
        team.setHome(player.getLocation());
        teams().markDirty();
        teamMessage(team, "&f" + player.getName() + " &aset the team home.");
        sound(player, Sound.BLOCK_BEACON_ACTIVATE, 1.5f);
    }

    public void delHome(Player player) {
        Team team = requireTeam(player);
        if (team == null || !requireRole(player, team, Team.Role.ADMIN)) {
            return;
        }
        team.clearHome();
        teams().markDirty();
        teamMessage(team, "&f" + player.getName() + " &cremoved the team home.");
    }

    public void home(Player player) {
        Team team = requireTeam(player);
        if (team != null) {
            plugin.getHomes().start(player, team.getHome());
        }
    }

    public void toggleFriendlyFire(Player player) {
        Team team = requireTeam(player);
        if (team == null || !requireRole(player, team, Team.Role.ADMIN)) {
            return;
        }
        team.setFriendlyFire(!team.isFriendlyFire());
        teams().markDirty();
        teamMessage(team, "&f" + player.getName() + " &7turned friendly fire "
                + (team.isFriendlyFire() ? "&cON &7(you can hurt each other)" : "&aOFF &7(you can't hurt each other)"));
    }

    public void toggleOpen(Player player) {
        Team team = requireTeam(player);
        if (team == null || !requireRole(player, team, Team.Role.OWNER)) {
            return;
        }
        team.setOpen(!team.isOpen());
        teams().markDirty();
        teamMessage(team, "&7The team is now " + (team.isOpen()
                ? "&aOPEN &7(anyone can join)" : "&dINVITE ONLY"));
    }

    public void setColor(Player player, String hex, String colorName) {
        Team team = requireTeam(player);
        if (team == null || !requireRole(player, team, Team.Role.OWNER)) {
            return;
        }
        if (hex == null || !hex.matches("#[A-Fa-f0-9]{6}")) {
            fail(player, "That isn't a colour.");
            return;
        }
        team.setColor(hex.toUpperCase());
        teams().markDirty();
        teamMessage(team, "&7Team colour changed to " + team.colored() + " &8(" + colorName + ")");
    }

    public void rename(Player player, String newName) {
        Team team = requireTeam(player);
        if (team == null || !requireRole(player, team, Team.Role.OWNER)) {
            return;
        }
        String error = teams().checkName(newName, team);
        if (error != null) {
            fail(player, error);
            return;
        }
        String old = team.getName();
        teams().rename(team, newName);
        teamMessage(team, "&7The team was renamed from &f" + old + " &7to " + team.colored() + "&7.");
    }

    // ------------------------------------------------------------------
    // Chat
    // ------------------------------------------------------------------

    public void toggleChat(Player player) {
        Team team = requireTeam(player);
        if (team == null) {
            return;
        }
        boolean on = !plugin.isTeamChat(player.getUniqueId());
        plugin.setTeamChat(player.getUniqueId(), on);
        String global = plugin.getConfig().getString("chat.global-prefix", "!");
        if (on) {
            send(player, "&aTeam chat ON. &7Everything you type goes to your team."
                    + (global.isEmpty() ? "" : " Start with &f" + global + " &7to talk in normal chat."));
        } else {
            send(player, "&7Team chat OFF. You're talking in normal chat.");
        }
    }

    /** Sends a team chat message. Must run on the main thread. */
    public void teamChat(Player player, String message) {
        Team team = teams().getTeam(player.getUniqueId());
        if (team == null) {
            plugin.setTeamChat(player.getUniqueId(), false);
            fail(player, "You're not in a team.");
            return;
        }
        Team.Role role = team.getRole(player.getUniqueId());
        String format = plugin.getConfig().getString("chat.format",
                "&#FF1493&l[TEAM] &#FFB6C1%role%%player%&8: &#FFE4EC%message%")
                .replace("%role%", role == null ? "" : role.icon())
                .replace("%player%", player.getName())
                .replace("%team%", team.getName());
        Component line = withMessage(format, message);
        team.broadcast(line);

        String spyFormat = plugin.getConfig().getString("chat.spy-format",
                "&8[SPY] &#FF69B4%team% &7%player%&8: &7%message%")
                .replace("%player%", player.getName())
                .replace("%team%", team.getName());
        Component spyLine = withMessage(spyFormat, message);
        for (UUID spy : plugin.getSpies()) {
            Player spyPlayer = Bukkit.getPlayer(spy);
            if (spyPlayer != null && !team.isMember(spy)) {
                spyPlayer.sendMessage(spyLine);
            }
        }
        plugin.getLogger().info("[TeamChat] [" + team.getName() + "] " + player.getName() + ": " + message);
    }

    /** Puts the player's message into the format as plain text, so players can't use colour codes. */
    private Component withMessage(String format, String message) {
        int index = format.indexOf("%message%");
        if (index < 0) {
            return Text.component(format).append(Component.text(" " + message));
        }
        String before = format.substring(0, index);
        String after = format.substring(index + "%message%".length());
        // The message gets the colour that was set just before %message%
        Component msg = Component.text(message);
        TextColor color = lastColor(before);
        if (color != null) {
            msg = msg.color(color);
        }
        return Component.empty().append(Text.component(before)).append(msg).append(Text.component(after));
    }

    /** The last colour (&a or &#RRGGBB) in a format string, or null. */
    private TextColor lastColor(String text) {
        int hex = text.lastIndexOf("&#");
        int legacy = -1;
        for (int i = text.length() - 2; i >= 0; i--) {
            if (text.charAt(i) == '&' && "0123456789abcdefABCDEF".indexOf(text.charAt(i + 1)) >= 0) {
                legacy = i;
                break;
            }
        }
        if (hex >= 0 && hex + 8 <= text.length() && hex >= legacy) {
            return TextColor.fromHexString(text.substring(hex + 1, hex + 8));
        }
        if (legacy >= 0) {
            LegacyFormat format = LegacyComponentSerializer.parseChar(text.charAt(legacy + 1));
            return format == null ? null : format.color();
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Bank
    // ------------------------------------------------------------------

    private BankService bankOrFail(Player player) {
        BankService bank = plugin.getBank();
        if (bank == null || !bank.isReady()) {
            fail(player, "The team bank needs an economy plugin (BlossomEconomy + Vault).");
            return null;
        }
        return bank;
    }

    public void deposit(Player player, double amount) {
        Team team = requireTeam(player);
        BankService bank = team == null ? null : bankOrFail(player);
        if (bank == null) {
            return;
        }
        if (amount <= 0) {
            fail(player, "Pick an amount above $0.");
            return;
        }
        if (!bank.has(player, amount)) {
            fail(player, "You only have " + Text.money(bank.balance(player)) + ".");
            return;
        }
        if (!bank.take(player, amount)) {
            fail(player, "The payment didn't go through.");
            return;
        }
        team.setBank(team.getBank() + amount);
        teams().markDirty();
        teamMessage(team, "&f" + player.getName() + " &aput &f" + Text.money(amount)
                + " &ain the team bank. &7(Total: " + Text.money(team.getBank()) + ")");
        sound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f);
    }

    public void withdraw(Player player, double amount) {
        Team team = requireTeam(player);
        if (team == null || !requireRole(player, team, Team.Role.ADMIN)) {
            return;
        }
        BankService bank = bankOrFail(player);
        if (bank == null) {
            return;
        }
        if (amount <= 0) {
            fail(player, "Pick an amount above $0.");
            return;
        }
        if (amount > team.getBank()) {
            fail(player, "The team bank only has " + Text.money(team.getBank()) + ".");
            return;
        }
        team.setBank(team.getBank() - amount);
        teams().markDirty();
        if (!bank.give(player, amount)) {
            team.setBank(team.getBank() + amount);
            fail(player, "The payment didn't go through.");
            return;
        }
        teamMessage(team, "&f" + player.getName() + " &etook &f" + Text.money(amount)
                + " &efrom the team bank. &7(Left: " + Text.money(team.getBank()) + ")");
        sound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f);
    }

    // ------------------------------------------------------------------
    // Info
    // ------------------------------------------------------------------

    public void info(CommandSender sender, Team team) {
        if (team == null) {
            send(sender, "&cThat team doesn't exist.");
            return;
        }
        String date = new SimpleDateFormat("dd MMM yyyy").format(new Date(team.getCreated()));
        sender.sendMessage(Text.component("&#FF1493&m                                "));
        sender.sendMessage(Text.component("  &#FF69B4&l✿ " + team.colored() + " &#FF69B4&l✿"));
        UUID owner = team.getOwner();
        sender.sendMessage(Text.component("  &7Owner: &f" + (owner == null ? "-" : team.nameOf(owner))));
        sender.sendMessage(Text.component("  &7Members: &f" + team.size() + "/" + teams().maxMembers()
                + " &8(" + team.online().size() + " online)"));
        StringBuilder list = new StringBuilder();
        for (UUID uuid : team.sortedMembers()) {
            if (list.length() > 0) {
                list.append("&7, ");
            }
            list.append(Bukkit.getPlayer(uuid) != null ? "&a" : "&7").append(team.getRole(uuid).icon())
                    .append(team.nameOf(uuid));
        }
        sender.sendMessage(Text.component("  " + list));
        sender.sendMessage(Text.component("  &7Kills: &f" + team.getKills()));
        if (plugin.getBank() != null) {
            sender.sendMessage(Text.component("  &7Bank: &a" + Text.money(team.getBank())));
        }
        sender.sendMessage(Text.component("  &7Friendly fire: " + (team.isFriendlyFire() ? "&cON" : "&aOFF")
                + "  &7Joining: " + (team.isOpen() ? "&aOpen" : "&dInvite only")));
        sender.sendMessage(Text.component("  &7Made on: &f" + date));
        sender.sendMessage(Text.component("&#FF1493&m                                "));
    }

    public void top(CommandSender sender) {
        List<Team> list = teams().sorted();
        if (list.isEmpty()) {
            send(sender, "&7There are no teams yet. Make one with &f/team create <name>&7!");
            return;
        }
        sender.sendMessage(Text.component("&#FF1493&l✿ TOP TEAMS ✿"));
        String[] medals = {"&#FFD700", "&#C0C0C0", "&#CD7F32"};
        for (int i = 0; i < Math.min(10, list.size()); i++) {
            Team t = list.get(i);
            String num = (i < 3 ? medals[i] + "&l" : "&#FFB6C1") + "#" + (i + 1);
            sender.sendMessage(Text.component(num + " " + t.colored() + " &8- &f" + t.getKills() + " kills &8| &7"
                    + t.size() + " members"));
        }
    }

    public OfflinePlayer offline(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }
}
