package com.blossomsmp.teams;

import com.blossomsmp.teams.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** One team: its members, settings, home, bank and kills. */
public class Team {

    public enum Role {
        MEMBER("Member", ""),
        ADMIN("Admin", "★ "),
        OWNER("Owner", "♛ ");

        private final String label;
        private final String icon;

        Role(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }

        public String label() {
            return label;
        }

        public String icon() {
            return icon;
        }

        public boolean atLeast(Role other) {
            return ordinal() >= other.ordinal();
        }
    }

    private String name;
    private String color;
    private final long created;
    private final Map<UUID, Role> members = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    private String homeWorld;
    private double homeX;
    private double homeY;
    private double homeZ;
    private float homeYaw;
    private float homePitch;

    private volatile boolean friendlyFire;
    private volatile boolean open;
    private volatile double bank;
    private volatile int kills;

    public Team(String name, String color, long created) {
        this.name = name;
        this.color = color;
        this.created = created;
    }

    // ------------------------------------------------------------------
    // Name and colour
    // ------------------------------------------------------------------

    /** Lowercase name, used as the key. */
    public String getId() {
        return name.toLowerCase(Locale.ROOT);
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    /** "#FF69B4" */
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    /** Coloured name, e.g. "&#FF69B4Sakura" */
    public String colored() {
        return "&" + color + name;
    }

    /** Coloured tag with brackets, e.g. "&#FF69B4[Sakura]" */
    public String tag() {
        return "&" + color + "[" + name + "]";
    }

    public long getCreated() {
        return created;
    }

    // ------------------------------------------------------------------
    // Members
    // ------------------------------------------------------------------

    public void putMember(UUID uuid, Role role, String playerName) {
        members.put(uuid, role);
        if (playerName != null) {
            names.put(uuid, playerName);
        }
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        names.remove(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public Role getRole(UUID uuid) {
        return members.get(uuid);
    }

    public void setRole(UUID uuid, Role role) {
        if (members.containsKey(uuid)) {
            members.put(uuid, role);
        }
    }

    public Map<UUID, Role> getMembers() {
        return members;
    }

    public int size() {
        return members.size();
    }

    public String nameOf(UUID uuid) {
        String stored = names.get(uuid);
        return stored != null ? stored : "Unknown";
    }

    public void updateName(UUID uuid, String playerName) {
        if (members.containsKey(uuid) && playerName != null) {
            names.put(uuid, playerName);
        }
    }

    public UUID getOwner() {
        for (Map.Entry<UUID, Role> entry : members.entrySet()) {
            if (entry.getValue() == Role.OWNER) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Finds a member by name (any case). */
    public UUID findMember(String playerName) {
        for (Map.Entry<UUID, String> entry : names.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(playerName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Owner first, then admins, then members, each by name. */
    public List<UUID> sortedMembers() {
        List<UUID> list = new ArrayList<>(members.keySet());
        list.sort(Comparator.<UUID>comparingInt(u -> -members.getOrDefault(u, Role.MEMBER).ordinal())
                .thenComparing(u -> nameOf(u).toLowerCase(Locale.ROOT)));
        return list;
    }

    public List<Player> online() {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : members.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                list.add(player);
            }
        }
        return list;
    }

    /** Sends a message to every online member. */
    public void broadcast(Component message) {
        for (Player player : online()) {
            player.sendMessage(message);
        }
    }

    public void broadcast(String prefix, String text) {
        broadcast(Text.component(prefix + text));
    }

    // ------------------------------------------------------------------
    // Home
    // ------------------------------------------------------------------

    public Location getHome() {
        if (homeWorld == null) {
            return null;
        }
        World world = Bukkit.getWorld(homeWorld);
        if (world == null) {
            return null;
        }
        return new Location(world, homeX, homeY, homeZ, homeYaw, homePitch);
    }

    public boolean hasHome() {
        return homeWorld != null;
    }

    public void setHome(Location location) {
        if (location == null || location.getWorld() == null) {
            homeWorld = null;
            return;
        }
        setHomeRaw(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    public void setHomeRaw(String world, double x, double y, double z, float yaw, float pitch) {
        this.homeWorld = world;
        this.homeX = x;
        this.homeY = y;
        this.homeZ = z;
        this.homeYaw = yaw;
        this.homePitch = pitch;
    }

    public void clearHome() {
        homeWorld = null;
    }

    public String getHomeWorld() {
        return homeWorld;
    }

    public double getHomeX() {
        return homeX;
    }

    public double getHomeY() {
        return homeY;
    }

    public double getHomeZ() {
        return homeZ;
    }

    public float getHomeYaw() {
        return homeYaw;
    }

    public float getHomePitch() {
        return homePitch;
    }

    // ------------------------------------------------------------------
    // Settings and stats
    // ------------------------------------------------------------------

    public boolean isFriendlyFire() {
        return friendlyFire;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public double getBank() {
        return bank;
    }

    public void setBank(double bank) {
        this.bank = Math.max(0, Math.round(bank * 100.0) / 100.0);
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public void addKill() {
        kills++;
    }
}
