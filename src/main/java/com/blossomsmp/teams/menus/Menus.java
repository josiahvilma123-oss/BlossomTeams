package com.blossomsmp.teams.menus;

import com.blossomsmp.teams.BlossomTeams;
import com.blossomsmp.teams.Team;
import com.blossomsmp.teams.TeamActions;
import com.blossomsmp.teams.TeamManager;
import com.blossomsmp.teams.hooks.BankService;
import com.blossomsmp.teams.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/** All the pink team menus. */
public class Menus {

    /** A colour teams can pick, from config.yml. */
    public record ColorOption(String name, String hex, Material item) {
    }

    private static final Material BORDER_A = Material.PINK_STAINED_GLASS_PANE;
    private static final Material BORDER_B = Material.MAGENTA_STAINED_GLASS_PANE;
    private static final String TITLE = "&#FF1493&l❀ &#FF69B4&l";
    private static final String NAME = "&#FF69B4&l";
    private static final String CLICK = "&#FFB6C1» ";

    private final BlossomTeams plugin;

    public Menus(BlossomTeams plugin) {
        this.plugin = plugin;
    }

    private TeamManager teams() {
        return plugin.getTeams();
    }

    private TeamActions actions() {
        return plugin.getActions();
    }

    // ------------------------------------------------------------------
    // Building blocks
    // ------------------------------------------------------------------

    private MenuHolder create(int rows, String title) {
        MenuHolder holder = new MenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, rows * 9, Text.component(title));
        holder.setInventory(inventory);
        for (int slot = 0; slot < rows * 9; slot++) {
            int row = slot / 9;
            int col = slot % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                inventory.setItem(slot, item((row + col) % 2 == 0 ? BORDER_A : BORDER_B, " "));
            }
        }
        return holder;
    }

    /** The slots inside the pink border. */
    private static List<Integer> inner(int rows) {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < 8; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots;
    }

    static ItemStack item(Material material, String name, String... lore) {
        return item(material, name, Arrays.asList(lore));
    }

    static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(name));
            if (!lore.isEmpty()) {
                List<net.kyori.adventure.text.Component> lines = new ArrayList<>();
                for (String line : lore) {
                    lines.add(Text.item(line));
                }
                meta.lore(lines);
            }
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    static ItemStack glow(ItemStack item, boolean glow) {
        if (glow) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setEnchantmentGlintOverride(true);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    static ItemStack head(UUID owner, String name, List<String> lore) {
        ItemStack item = item(Material.PLAYER_HEAD, name, lore);
        if (item.getItemMeta() instanceof SkullMeta skull) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
            item.setItemMeta(skull);
        }
        return item;
    }

    private ItemStack closeItem() {
        return item(Material.BARRIER, "&c&lClose");
    }

    private ItemStack backItem() {
        return item(Material.ARROW, "&#FFB6C1&l« Back");
    }

    private void open(Player player, MenuHolder holder) {
        player.openInventory(holder.getInventory());
    }

    public List<ColorOption> colors() {
        List<ColorOption> list = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("colors")) {
            String[] parts = line.split(";");
            if (parts.length < 3 || !parts[1].trim().matches("#[A-Fa-f0-9]{6}")) {
                continue;
            }
            Material material = Material.matchMaterial(parts[2].trim());
            if (material == null || !material.isItem()) {
                material = Material.PINK_DYE;
            }
            list.add(new ColorOption(parts[0].trim(), parts[1].trim().toUpperCase(Locale.ROOT), material));
        }
        if (list.isEmpty()) {
            list.add(new ColorOption("Pink", TeamManager.DEFAULT_COLOR, Material.PINK_DYE));
        }
        return list;
    }

    /** A banner in the team's colour (the closest one from the colour list). */
    private Material bannerFor(Team team) {
        for (ColorOption option : colors()) {
            if (option.hex().equalsIgnoreCase(team.getColor())) {
                String name = option.item().name();
                if (name.endsWith("_DYE")) {
                    Material banner = Material.matchMaterial(name.replace("_DYE", "_BANNER"));
                    if (banner != null) {
                        return banner;
                    }
                }
            }
        }
        return Material.PINK_BANNER;
    }

    private String onOff(boolean on) {
        return on ? "&a&lON" : "&c&lOFF";
    }

    private List<String> teamLore(Team team, Player viewer) {
        List<String> lore = new ArrayList<>();
        UUID owner = team.getOwner();
        lore.add("&7Owner: &f" + (owner == null ? "-" : team.nameOf(owner)));
        lore.add("&7Members: &f" + team.size() + "/" + teams().maxMembers()
                + " &8(" + team.online().size() + " online)");
        lore.add("&7Kills: &f" + team.getKills());
        if (plugin.getBank() != null) {
            lore.add("&7Bank: &a" + Text.money(team.getBank()));
        }
        lore.add("&7Friendly fire: " + onOff(team.isFriendlyFire()));
        lore.add("&7Joining: " + (team.isOpen() ? "&aOpen to all" : "&dInvite only"));
        lore.add("&7Made on: &f" + new SimpleDateFormat("dd MMM yyyy").format(new Date(team.getCreated())));
        Team.Role role = viewer == null ? null : team.getRole(viewer.getUniqueId());
        if (role != null) {
            lore.add("");
            lore.add("&7Your role: &#FF69B4" + role.icon() + role.label());
        }
        return lore;
    }

    // ------------------------------------------------------------------
    // Main menus
    // ------------------------------------------------------------------

    /** /team: the right menu depending on whether the player has a team. */
    public void open(Player player) {
        Team team = teams().getTeam(player.getUniqueId());
        if (team == null) {
            openNoTeam(player);
        } else {
            openTeam(player, team);
        }
    }

    public void openNoTeam(Player player) {
        MenuHolder h = create(3, TITLE + "Blossom Teams");
        int invites = teams().invitesFor(player.getUniqueId()).size();
        double cost = plugin.getConfig().getDouble("teams.create-cost", 0);

        List<String> createLore = new ArrayList<>(List.of(
                "&7Start your own team and", "&7invite your friends!", "",
                "&7Max players: &f" + teams().maxMembers()));
        if (cost > 0) {
            createLore.add("&7Cost: &a" + Text.money(cost));
        }
        createLore.add("");
        createLore.add(CLICK + "Click to make a team");
        h.set(11, item(Material.CHERRY_SAPLING, NAME + "✿ Make a Team", createLore), this::askCreate);

        h.set(13, glow(item(Material.PAPER, NAME + "✉ Your Invites",
                "&7You have &f" + invites + " &7invite" + (invites == 1 ? "" : "s") + ".", "",
                CLICK + "Click to see them"), invites > 0), this::openInvites);

        h.set(15, item(Material.BOOK, NAME + "☰ All Teams",
                "&7See every team on the server", "&7and join open ones.", "",
                CLICK + "Click to look"), p -> openBrowse(p, 0));

        h.set(22, closeItem(), Player::closeInventory);
        open(player, h);
    }

    private void askCreate(Player player) {
        int min = plugin.getConfig().getInt("teams.name-min-length", 3);
        int max = plugin.getConfig().getInt("teams.name-max-length", 12);
        plugin.getPrompts().ask(player, "&#FF69B4✿ &fType a name for your team &7(" + min + "-" + max
                + " letters, numbers or _)", name -> {
            if (actions().create(player, name)) {
                open(player);
            }
        });
    }

    public void openTeam(Player player, Team team) {
        MenuHolder h = create(5, "&" + team.getColor() + "&l❀ " + team.getName());
        UUID me = player.getUniqueId();
        Team.Role myRole = team.getRole(me);
        if (myRole == null) {
            openNoTeam(player);
            return;
        }
        boolean admin = myRole.atLeast(Team.Role.ADMIN);
        boolean owner = myRole == Team.Role.OWNER;

        // Team info at the top
        h.set(4, item(bannerFor(team), "&" + team.getColor() + "&l✿ " + team.getName() + " ✿",
                teamLore(team, player)), null);

        // Members row
        int max = teams().maxMembers();
        List<UUID> members = team.sortedMembers();
        int[] row = {10, 11, 12, 13, 14, 15, 16};
        boolean tooMany = Math.max(max, members.size()) > row.length;
        int shown = tooMany ? row.length - 1 : row.length;
        for (int i = 0; i < shown; i++) {
            int slot = row[i];
            if (i < members.size()) {
                UUID uuid = members.get(i);
                Team.Role role = team.getRole(uuid);
                boolean online = Bukkit.getPlayer(uuid) != null;
                boolean canManage = !uuid.equals(me) && role != null
                        && (owner || (myRole == Team.Role.ADMIN && role == Team.Role.MEMBER));
                List<String> lore = new ArrayList<>();
                lore.add("&7Role: &#FF69B4" + (role == null ? "" : role.icon() + role.label()));
                lore.add(online ? "&a● Online" : "&7● Offline");
                if (uuid.equals(me)) {
                    lore.add("");
                    lore.add("&#FFB6C1(that's you!)");
                } else if (canManage) {
                    lore.add("");
                    lore.add(CLICK + "Click to manage");
                }
                h.set(slot, head(uuid, (online ? "&a" : "&7") + (role == null ? "" : role.icon())
                        + team.nameOf(uuid), lore), canManage ? p -> openMember(p, uuid) : null);
            } else if (i < max) {
                h.set(slot, item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&7Empty spot",
                        "&7Invite someone to fill it!", "", CLICK + "Click to invite"),
                        p -> openInvite(p, 0));
            }
        }
        if (tooMany) {
            h.set(row[row.length - 1], item(Material.PLAYER_HEAD, NAME + "All members",
                    "&7" + members.size() + "/" + max + " players", "", CLICK + "Click to see everyone"),
                    p -> openMembers(p, 0));
        }

        // Buttons row 1
        h.set(20, item(Material.PINK_BED, NAME + "⌂ Team Home",
                team.hasHome() ? "&7Teleport to your team home." : "&7No home set yet.",
                "&7You have to stand still for a few seconds.", "", CLICK + "Click to teleport"),
                p -> {
                    p.closeInventory();
                    actions().home(p);
                });
        h.set(21, item(Material.COMPASS, NAME + "⚑ Set Team Home",
                "&7Set the team home to where", "&7you're standing now.", "",
                admin ? CLICK + "Click to set it here" : "&cOwner and admins only"),
                p -> {
                    actions().setHome(p);
                    refresh(p);
                });
        boolean chatOn = plugin.isTeamChat(me);
        String global = plugin.getConfig().getString("chat.global-prefix", "!");
        h.set(22, glow(item(Material.WRITABLE_BOOK, NAME + "✉ Team Chat: " + onOff(chatOn),
                "&7When ON, everything you type", "&7only goes to your team.",
                global.isEmpty() ? "" : "&7Start with &f" + global + " &7to talk to everyone.",
                "&7Quick message: &f/tc <message>", "", CLICK + "Click to switch"), chatOn),
                p -> {
                    actions().toggleChat(p);
                    refresh(p);
                });
        BankService bank = plugin.getBank();
        if (bank != null) {
            h.set(23, item(Material.GOLD_INGOT, NAME + "$ Team Bank",
                    "&7Money in the bank: &a" + Text.money(team.getBank()), "",
                    "&7Everyone can put money in.", "&7Owner and admins can take it out.", "",
                    CLICK + "Click to open"), p -> openBank(p));
        }
        boolean canInvite = admin || plugin.getConfig().getBoolean("teams.members-can-invite", false);
        h.set(24, item(Material.NAME_TAG, NAME + "+ Invite Players",
                "&7Ask online players to join.", "&7Spots left: &f" + Math.max(0, max - team.size()), "",
                canInvite ? CLICK + "Click to pick a player" : "&cOwner and admins only"),
                p -> openInvite(p, 0));

        // Buttons row 2
        h.set(29, glow(item(team.isFriendlyFire() ? Material.IRON_SWORD : Material.WOODEN_SWORD,
                NAME + "⚔ Friendly Fire: " + onOff(team.isFriendlyFire()),
                team.isFriendlyFire() ? "&7Teammates CAN hurt each other." : "&7Teammates can't hurt each other.", "",
                admin ? CLICK + "Click to switch" : "&cOwner and admins only"), team.isFriendlyFire()),
                p -> {
                    actions().toggleFriendlyFire(p);
                    refresh(p);
                });
        h.set(30, item(colorItem(team), "&" + team.getColor() + "&l🎨 Team Colour",
                "&7Your team name shows in this colour.", "",
                owner ? CLICK + "Click to pick a colour" : "&cOwner only"),
                p -> {
                    if (owner) {
                        openColors(p);
                    } else {
                        actions().setColor(p, null, "");
                    }
                });
        h.set(31, item(Material.NETHER_STAR, NAME + "★ Top Teams",
                "&7See the best teams on the server.", "", CLICK + "Click to look"), p -> openBrowse(p, 0));
        h.set(32, item(team.isOpen() ? Material.OAK_DOOR : Material.IRON_DOOR,
                NAME + "✦ Joining: " + (team.isOpen() ? "&a&lOPEN" : "&d&lINVITE ONLY"),
                team.isOpen() ? "&7Anyone can join your team." : "&7Players need an invite to join.", "",
                owner ? CLICK + "Click to switch" : "&cOwner only"),
                p -> {
                    actions().toggleOpen(p);
                    refresh(p);
                });
        if (owner) {
            h.set(33, item(Material.TNT, "&c&l✖ Disband Team",
                    "&7Deletes the team for everyone.", "&7The bank money goes to you.", "",
                    "&cClick to disband"),
                    p -> confirm(p, "Disband " + team.getName() + "?",
                            item(Material.TNT, "&c&lDisband " + team.getName(), "&7This can't be undone!"),
                            yes -> {
                                yes.closeInventory();
                                actions().disband(yes);
                            },
                            this::open));
        } else {
            h.set(33, item(Material.IRON_DOOR, "&c&l✖ Leave Team",
                    "&7Leave " + team.getName() + ".", "", "&cClick to leave"),
                    p -> confirm(p, "Leave " + team.getName() + "?",
                            item(Material.IRON_DOOR, "&c&lLeave " + team.getName()),
                            yes -> {
                                yes.closeInventory();
                                actions().leave(yes);
                            },
                            this::open));
        }

        h.set(40, closeItem(), Player::closeInventory);
        open(player, h);
    }

    /** Opens the main menu again, so the buttons show the new settings. */
    private void refresh(Player player) {
        open(player);
    }

    private Material colorItem(Team team) {
        for (ColorOption option : colors()) {
            if (option.hex().equalsIgnoreCase(team.getColor())) {
                return option.item();
            }
        }
        return Material.PINK_DYE;
    }

    // ------------------------------------------------------------------
    // Members
    // ------------------------------------------------------------------

    public void openMember(Player player, UUID target) {
        Team team = teams().getTeam(player.getUniqueId());
        if (team == null || !team.isMember(target)) {
            open(player);
            return;
        }
        Team.Role myRole = team.getRole(player.getUniqueId());
        Team.Role role = team.getRole(target);
        boolean owner = myRole == Team.Role.OWNER;
        String name = team.nameOf(target);
        MenuHolder h = create(3, TITLE + name);

        h.set(4, head(target, "&f&l" + role.icon() + name, List.of(
                "&7Role: &#FF69B4" + role.label(),
                Bukkit.getPlayer(target) != null ? "&a● Online" : "&7● Offline")), null);

        if (owner && role == Team.Role.MEMBER) {
            h.set(10, item(Material.LIME_DYE, "&a&l▲ Make Admin",
                    "&7Admins can invite, kick members,", "&7set the home and take bank money.", "",
                    CLICK + "Click to promote"),
                    p -> {
                        actions().promote(p, target);
                        openMember(p, target);
                    });
        }
        if (owner && role == Team.Role.ADMIN) {
            h.set(10, item(Material.YELLOW_DYE, "&e&l▼ Remove Admin",
                    "&7Make them a normal member again.", "", CLICK + "Click to demote"),
                    p -> {
                        actions().demote(p, target);
                        openMember(p, target);
                    });
        }
        if (owner) {
            h.set(12, item(Material.GOLDEN_HELMET, "&6&l♛ Make Owner",
                    "&7Give the team to " + name + ".", "&7You become an admin.", "", CLICK + "Click to give"),
                    p -> confirm(p, "Give the team to " + name + "?",
                            head(target, "&6&l♛ " + name + " becomes owner", List.of("&7You become an admin.")),
                            yes -> {
                                actions().transfer(yes, target);
                                open(yes);
                            },
                            no -> openMember(no, target)));
        }
        h.set(14, item(Material.REDSTONE_BLOCK, "&c&l✖ Kick",
                "&7Remove " + name + " from the team.", "", "&cClick to kick"),
                p -> confirm(p, "Kick " + name + "?",
                        head(target, "&c&lKick " + name, List.of()),
                        yes -> {
                            actions().kick(yes, target);
                            open(yes);
                        },
                        no -> openMember(no, target)));
        h.set(22, backItem(), this::open);
        open(player, h);
    }

    public void openMembers(Player player, int page) {
        Team team = teams().getTeam(player.getUniqueId());
        if (team == null) {
            open(player);
            return;
        }
        MenuHolder h = create(6, TITLE + "Members");
        List<UUID> members = team.sortedMembers();
        List<Integer> slots = inner(6);
        int start = page * slots.size();
        Team.Role myRole = team.getRole(player.getUniqueId());
        for (int i = 0; i < slots.size() && start + i < members.size(); i++) {
            UUID uuid = members.get(start + i);
            Team.Role role = team.getRole(uuid);
            boolean canManage = !uuid.equals(player.getUniqueId()) && role != null
                    && (myRole == Team.Role.OWNER || (myRole == Team.Role.ADMIN && role == Team.Role.MEMBER));
            List<String> lore = new ArrayList<>();
            lore.add("&7Role: &#FF69B4" + (role == null ? "" : role.label()));
            lore.add(Bukkit.getPlayer(uuid) != null ? "&a● Online" : "&7● Offline");
            if (canManage) {
                lore.add("");
                lore.add(CLICK + "Click to manage");
            }
            h.set(slots.get(i), head(uuid, "&f" + (role == null ? "" : role.icon()) + team.nameOf(uuid), lore),
                    canManage ? p -> openMember(p, uuid) : null);
        }
        pages(h, 6, page, members.size(), slots.size(), p -> openMembers(p, page - 1), p -> openMembers(p, page + 1));
        h.set(49, backItem(), this::open);
        open(player, h);
    }

    // ------------------------------------------------------------------
    // Invites
    // ------------------------------------------------------------------

    public void openInvite(Player player, int page) {
        Team team = teams().getTeam(player.getUniqueId());
        if (team == null) {
            open(player);
            return;
        }
        Team.Role role = team.getRole(player.getUniqueId());
        boolean canInvite = (role != null && role.atLeast(Team.Role.ADMIN))
                || plugin.getConfig().getBoolean("teams.members-can-invite", false);
        if (!canInvite) {
            player.closeInventory();
            actions().invite(player, null); // shows the "owner and admins only" message
            return;
        }
        if (team.size() >= teams().maxMembers()) {
            player.sendMessage(plugin.msg("&cYour team is full (" + teams().maxMembers() + " players max)."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            return;
        }
        List<Player> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player) && player.canSee(online) && teams().getTeam(online.getUniqueId()) == null) {
                candidates.add(online);
            }
        }
        candidates.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        MenuHolder h = create(6, TITLE + "Invite a Player");
        List<Integer> slots = inner(6);
        int start = page * slots.size();
        if (candidates.isEmpty()) {
            h.set(22, item(Material.GRAY_DYE, "&7Nobody to invite",
                    "&7Everyone online is already", "&7in a team (or it's just you)."), null);
        }
        for (int i = 0; i < slots.size() && start + i < candidates.size(); i++) {
            Player target = candidates.get(start + i);
            boolean invited = teams().hasInvite(target.getUniqueId(), team);
            h.set(slots.get(i), glow(head(target.getUniqueId(), "&f" + target.getName(), List.of(
                    invited ? "&aAlready invited" : "&7Not in a team", "",
                    invited ? "&7Waiting for them to accept..." : CLICK + "Click to invite")), invited),
                    invited ? null : p -> {
                        actions().invite(p, Bukkit.getPlayer(target.getUniqueId()));
                        openInvite(p, page);
                    });
        }
        pages(h, 6, page, candidates.size(), slots.size(), p -> openInvite(p, page - 1), p -> openInvite(p, page + 1));
        h.set(49, backItem(), this::open);
        open(player, h);
    }

    public void openInvites(Player player) {
        List<Team> invites = teams().invitesFor(player.getUniqueId());
        MenuHolder h = create(4, TITLE + "Your Invites");
        List<Integer> slots = inner(4);
        if (invites.isEmpty()) {
            h.set(13, item(Material.GRAY_DYE, "&7No invites right now",
                    "&7Ask a team owner to invite you,", "&7or join an open team!"), null);
        }
        for (int i = 0; i < slots.size() && i < invites.size(); i++) {
            Team team = invites.get(i);
            List<String> lore = teamLore(team, null);
            lore.add("");
            lore.add("&aLeft-click to join");
            lore.add("&cRight-click to say no");
            h.set(slots.get(i), item(bannerFor(team), "&" + team.getColor() + "&l" + team.getName(), lore),
                    p -> {
                        if (actions().accept(p, team.getName())) {
                            open(p);
                        } else {
                            openInvites(p);
                        }
                    });
            h.setRightClick(slots.get(i), p -> {
                actions().deny(p, team.getName());
                openInvites(p);
            });
        }
        h.set(31, backItem(), this::open);
        open(player, h);
    }

    // ------------------------------------------------------------------
    // All teams / top teams
    // ------------------------------------------------------------------

    public void openBrowse(Player player, int page) {
        List<Team> list = teams().sorted();
        boolean hasTeam = teams().getTeam(player.getUniqueId()) != null;
        MenuHolder h = create(6, TITLE + "All Teams");
        List<Integer> slots = inner(6);
        int start = page * slots.size();
        if (list.isEmpty()) {
            h.set(22, item(Material.GRAY_DYE, "&7No teams yet", "&7Be the first to make one!"), null);
        }
        for (int i = 0; i < slots.size() && start + i < list.size(); i++) {
            Team team = list.get(start + i);
            int rank = start + i + 1;
            boolean canJoin = !hasTeam && team.size() < teams().maxMembers()
                    && (team.isOpen() || teams().hasInvite(player.getUniqueId(), team));
            List<String> lore = teamLore(team, player);
            lore.add("");
            if (canJoin) {
                lore.add("&aClick to join!");
            } else if (!hasTeam && team.size() >= teams().maxMembers()) {
                lore.add("&cThis team is full.");
            }
            String medal = rank == 1 ? "&#FFD700" : rank == 2 ? "&#C0C0C0" : rank == 3 ? "&#CD7F32" : "&#FFB6C1";
            h.set(slots.get(i), glow(item(bannerFor(team), medal + "&l#" + rank + " &" + team.getColor() + "&l"
                    + team.getName(), lore), rank <= 3),
                    canJoin ? p -> {
                        if (actions().accept(p, team.getName())) {
                            open(p);
                        }
                    } : null);
        }
        pages(h, 6, page, list.size(), slots.size(), p -> openBrowse(p, page - 1), p -> openBrowse(p, page + 1));
        h.set(49, backItem(), this::open);
        open(player, h);
    }

    // ------------------------------------------------------------------
    // Colours
    // ------------------------------------------------------------------

    public void openColors(Player player) {
        Team team = teams().getTeam(player.getUniqueId());
        if (team == null) {
            open(player);
            return;
        }
        MenuHolder h = create(4, TITLE + "Team Colour");
        List<Integer> slots = inner(4);
        List<ColorOption> options = colors();
        for (int i = 0; i < slots.size() && i < options.size(); i++) {
            ColorOption option = options.get(i);
            boolean current = option.hex().equalsIgnoreCase(team.getColor());
            h.set(slots.get(i), glow(item(option.item(), "&" + option.hex() + "&l" + option.name(),
                    "&7Preview: &" + option.hex() + "[" + team.getName() + "]", "",
                    current ? "&aThis is your colour now" : CLICK + "Click to pick"), current),
                    p -> {
                        actions().setColor(p, option.hex(), option.name());
                        openColors(p);
                    });
        }
        h.set(31, backItem(), this::open);
        open(player, h);
    }

    // ------------------------------------------------------------------
    // Bank
    // ------------------------------------------------------------------

    public void openBank(Player player) {
        Team team = teams().getTeam(player.getUniqueId());
        BankService bank = plugin.getBank();
        if (team == null || bank == null) {
            open(player);
            return;
        }
        Team.Role role = team.getRole(player.getUniqueId());
        boolean admin = role != null && role.atLeast(Team.Role.ADMIN);
        MenuHolder h = create(3, TITLE + "Team Bank");

        h.set(4, item(Material.GOLD_BLOCK, "&6&l$ " + Text.money(team.getBank()),
                "&7Money in the team bank.", "&7Your money: &a" + Text.money(bank.balance(player))), null);

        double[] quick = {100, 1_000, 10_000};
        Material[] icons = {Material.GOLD_NUGGET, Material.GOLD_INGOT, Material.RAW_GOLD};
        for (int i = 0; i < quick.length; i++) {
            double amount = quick[i];
            h.set(10 + i, item(icons[i], "&a&l+ Put in " + Text.money(amount), "", CLICK + "Click to put it in"),
                    p -> {
                        actions().deposit(p, amount);
                        openBank(p);
                    });
        }
        h.set(13, item(Material.OAK_SIGN, "&a&l+ Put in other amount", "&7Type how much in chat.", "",
                CLICK + "Click"),
                p -> plugin.getPrompts().ask(p, "&#FF69B4✿ &fHow much do you want to put in? &7(like 500, 2k, 1m)",
                        text -> {
                            Double amount = Text.parseAmount(text);
                            if (amount == null) {
                                p.sendMessage(plugin.msg("&cThat isn't a valid amount."));
                            } else {
                                actions().deposit(p, amount);
                            }
                            openBank(p);
                        }));
        h.set(15, item(Material.HOPPER, "&e&l- Take out", "&7Type how much in chat.", "",
                admin ? CLICK + "Click" : "&cOwner and admins only"),
                p -> {
                    if (!admin) {
                        actions().withdraw(p, 0);
                        return;
                    }
                    plugin.getPrompts().ask(p, "&#FF69B4✿ &fHow much do you want to take out? &7(like 500, 2k, all)",
                            text -> {
                                Team current = teams().getTeam(p.getUniqueId());
                                Double amount = text.equalsIgnoreCase("all") && current != null
                                        ? Double.valueOf(current.getBank()) : Text.parseAmount(text);
                                if (amount == null) {
                                    p.sendMessage(plugin.msg("&cThat isn't a valid amount."));
                                } else {
                                    actions().withdraw(p, amount);
                                }
                                openBank(p);
                            });
                });
        h.set(16, item(Material.CHEST, "&e&l- Take out everything",
                "&7Take all " + Text.money(team.getBank()) + ".", "",
                admin ? CLICK + "Click" : "&cOwner and admins only"),
                p -> {
                    Team current = teams().getTeam(p.getUniqueId());
                    actions().withdraw(p, current == null ? 0 : current.getBank());
                    openBank(p);
                });

        h.set(22, backItem(), this::open);
        open(player, h);
    }

    // ------------------------------------------------------------------
    // Confirm and pages
    // ------------------------------------------------------------------

    public void confirm(Player player, String question, ItemStack info, Consumer<Player> yes, Consumer<Player> no) {
        MenuHolder h = create(3, TITLE + question);
        h.set(11, item(Material.LIME_CONCRETE, "&a&l✔ Yes", "&7Do it."), yes);
        h.set(13, info, null);
        h.set(15, item(Material.RED_CONCRETE, "&c&l✖ No", "&7Go back."), no);
        open(player, h);
    }

    private void pages(MenuHolder h, int rows, int page, int total, int perPage,
                       Consumer<Player> previous, Consumer<Player> next) {
        int bottom = (rows - 1) * 9;
        if (page > 0) {
            h.set(bottom, item(Material.ARROW, "&#FFB6C1&l« Page " + page), previous);
        }
        if ((page + 1) * perPage < total) {
            h.set(bottom + 8, item(Material.ARROW, "&#FFB6C1&lPage " + (page + 2) + " »"), next);
        }
    }
}
