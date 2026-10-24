package com.blossomsmp.teams;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Keeps every team in memory and saves them to plugins/BlossomTeams/teams.yml. */
public class TeamManager {

    public static final String DEFAULT_COLOR = "#FF69B4";

    private final BlossomTeams plugin;
    private final File file;
    private final Map<String, Team> teams = new ConcurrentHashMap<>();
    private final Map<UUID, Team> byPlayer = new ConcurrentHashMap<>();
    /** invited player -> (team id -> expiry time) */
    private final Map<UUID, Map<String, Long>> invites = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;

    public TeamManager(BlossomTeams plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "teams.yml");
    }

    // ------------------------------------------------------------------
    // Loading and saving
    // ------------------------------------------------------------------

    public void load() {
        teams.clear();
        byPlayer.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("teams");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            Team team = new Team(s.getString("name", key), s.getString("color", DEFAULT_COLOR),
                    s.getLong("created", System.currentTimeMillis()));
            team.setFriendlyFire(s.getBoolean("friendly-fire", false));
            team.setOpen(s.getBoolean("open", false));
            team.setBank(s.getDouble("bank", 0));
            team.setKills(s.getInt("kills", 0));

            ConfigurationSection m = s.getConfigurationSection("members");
            if (m != null) {
                for (String id : m.getKeys(false)) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(id);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    Team.Role role;
                    try {
                        role = Team.Role.valueOf(m.getString(id + ".role", "MEMBER"));
                    } catch (IllegalArgumentException e) {
                        role = Team.Role.MEMBER;
                    }
                    team.putMember(uuid, role, m.getString(id + ".name", "Unknown"));
                }
            }
            if (team.size() == 0) {
                continue;
            }
            if (team.getOwner() == null) {
                // No owner saved: the first member becomes owner
                team.setRole(team.sortedMembers().get(0), Team.Role.OWNER);
            }
            ConfigurationSection home = s.getConfigurationSection("home");
            if (home != null && home.getString("world") != null) {
                team.setHomeRaw(home.getString("world"), home.getDouble("x"), home.getDouble("y"),
                        home.getDouble("z"), (float) home.getDouble("yaw"), (float) home.getDouble("pitch"));
            }
            teams.put(team.getId(), team);
            for (UUID uuid : team.getMembers().keySet()) {
                byPlayer.put(uuid, team);
            }
        }
        plugin.getLogger().info("Loaded " + teams.size() + " team(s).");
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Team t : teams.values()) {
            String p = "teams." + t.getId() + ".";
            yaml.set(p + "name", t.getName());
            yaml.set(p + "color", t.getColor());
            yaml.set(p + "created", t.getCreated());
            yaml.set(p + "friendly-fire", t.isFriendlyFire());
            yaml.set(p + "open", t.isOpen());
            yaml.set(p + "bank", t.getBank());
            yaml.set(p + "kills", t.getKills());
            for (Map.Entry<UUID, Team.Role> entry : t.getMembers().entrySet()) {
                String mp = p + "members." + entry.getKey() + ".";
                yaml.set(mp + "role", entry.getValue().name());
                yaml.set(mp + "name", t.nameOf(entry.getKey()));
            }
            if (t.hasHome()) {
                yaml.set(p + "home.world", t.getHomeWorld());
                yaml.set(p + "home.x", t.getHomeX());
                yaml.set(p + "home.y", t.getHomeY());
                yaml.set(p + "home.z", t.getHomeZ());
                yaml.set(p + "home.yaw", (double) t.getHomeYaw());
                yaml.set(p + "home.pitch", (double) t.getHomePitch());
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save teams.yml", e);
        }
    }

    public void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    public void markDirty() {
        dirty = true;
    }

    // ------------------------------------------------------------------
    // Lookups
    // ------------------------------------------------------------------

    public Team getTeam(UUID player) {
        return player == null ? null : byPlayer.get(player);
    }

    public Team getByName(String name) {
        return name == null ? null : teams.get(name.toLowerCase(Locale.ROOT));
    }

    public List<Team> all() {
        return new ArrayList<>(teams.values());
    }

    /** Best teams first: most kills, then most members, then name. */
    public List<Team> sorted() {
        List<Team> list = all();
        list.sort(Comparator.comparingInt(Team::getKills).reversed()
                .thenComparing(Comparator.comparingInt(Team::size).reversed())
                .thenComparing(Team::getId));
        return list;
    }

    public int maxMembers() {
        return Math.max(1, plugin.getConfig().getInt("teams.max-members", 3));
    }

    /** Returns why a team name isn't allowed, or null if it's fine. */
    public String checkName(String name, Team renaming) {
        int min = plugin.getConfig().getInt("teams.name-min-length", 3);
        int max = plugin.getConfig().getInt("teams.name-max-length", 12);
        if (name == null || name.length() < min || name.length() > max) {
            return "Team names must be " + min + " to " + max + " characters long.";
        }
        if (!name.matches("[A-Za-z0-9_]+")) {
            return "Team names can only use letters, numbers and _";
        }
        for (String blocked : plugin.getConfig().getStringList("teams.blocked-names")) {
            if (name.toLowerCase(Locale.ROOT).contains(blocked.toLowerCase(Locale.ROOT))) {
                return "That team name isn't allowed.";
            }
        }
        Team existing = getByName(name);
        if (existing != null && existing != renaming) {
            return "A team called " + existing.getName() + " already exists.";
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Changes
    // ------------------------------------------------------------------

    public Team create(UUID owner, String ownerName, String name) {
        Team team = new Team(name, DEFAULT_COLOR, System.currentTimeMillis());
        team.setFriendlyFire(plugin.getConfig().getBoolean("teams.default-friendly-fire", false));
        team.putMember(owner, Team.Role.OWNER, ownerName);
        teams.put(team.getId(), team);
        byPlayer.put(owner, team);
        invites.remove(owner);
        markDirty();
        return team;
    }

    public void disband(Team team) {
        teams.remove(team.getId());
        for (UUID uuid : new ArrayList<>(team.getMembers().keySet())) {
            byPlayer.remove(uuid);
        }
        for (Map<String, Long> map : invites.values()) {
            map.remove(team.getId());
        }
        markDirty();
    }

    public void addMember(Team team, UUID uuid, String playerName) {
        team.putMember(uuid, Team.Role.MEMBER, playerName);
        byPlayer.put(uuid, team);
        invites.remove(uuid);
        markDirty();
    }

    public void removeMember(Team team, UUID uuid) {
        team.removeMember(uuid);
        byPlayer.remove(uuid);
        markDirty();
    }

    public void rename(Team team, String newName) {
        String oldId = team.getId();
        teams.remove(oldId);
        team.setName(newName);
        teams.put(team.getId(), team);
        for (Map<String, Long> map : invites.values()) {
            Long expiry = map.remove(oldId);
            if (expiry != null) {
                map.put(team.getId(), expiry);
            }
        }
        markDirty();
    }

    // ------------------------------------------------------------------
    // Invites
    // ------------------------------------------------------------------

    public void invite(Team team, UUID player) {
        long seconds = Math.max(10, plugin.getConfig().getLong("teams.invite-expire-seconds", 120));
        invites.computeIfAbsent(player, k -> new ConcurrentHashMap<>())
                .put(team.getId(), System.currentTimeMillis() + seconds * 1000L);
    }

    public boolean hasInvite(UUID player, Team team) {
        Map<String, Long> map = invites.get(player);
        if (map == null) {
            return false;
        }
        Long expiry = map.get(team.getId());
        if (expiry == null) {
            return false;
        }
        if (expiry < System.currentTimeMillis()) {
            map.remove(team.getId());
            return false;
        }
        return true;
    }

    public void removeInvite(UUID player, Team team) {
        Map<String, Long> map = invites.get(player);
        if (map != null) {
            map.remove(team.getId());
        }
    }

    /** Teams that have invited this player (expired invites are removed). */
    public List<Team> invitesFor(UUID player) {
        List<Team> list = new ArrayList<>();
        Map<String, Long> map = invites.get(player);
        if (map == null) {
            return list;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            Team team = teams.get(entry.getKey());
            if (team == null || entry.getValue() < now) {
                it.remove();
            } else {
                list.add(team);
            }
        }
        return list;
    }
}
