package com.blossomsmp.teams.hooks;

import com.blossomsmp.teams.BlossomTeams;
import com.blossomsmp.teams.Team;
import com.blossomsmp.teams.util.Text;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.List;

/**
 * PlaceholderAPI placeholders, for TAB, holograms and chat:
 *
 * %blossomteams_name%          team name (or nothing)
 * %blossomteams_tag%           coloured [Name] with a space after (or nothing)
 * %blossomteams_colored%       coloured name (or nothing)
 * %blossomteams_color%         #FF69B4
 * %blossomteams_role%          Owner / Admin / Member
 * %blossomteams_role_icon%     ♛ / ★ / nothing
 * %blossomteams_members%       how many members
 * %blossomteams_max_members%   team size limit
 * %blossomteams_online%        members online
 * %blossomteams_kills%         team kills
 * %blossomteams_bank%          $1,234
 * %blossomteams_bank_short%    $1.2K
 * %blossomteams_has_team%      yes / no
 * %blossomteams_top_1_name%    best team's name (1 to 10)
 * %blossomteams_top_1_colored% best team's coloured name
 * %blossomteams_top_1_kills%   best team's kills
 * %blossomteams_top_1_members% best team's member count
 */
public class TeamPlaceholders extends PlaceholderExpansion {

    private final BlossomTeams plugin;

    public TeamPlaceholders(BlossomTeams plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "blossomteams";
    }

    @Override
    public String getAuthor() {
        return "BlossomSMP";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        try {
            return value(player, params.toLowerCase());
        } catch (Exception e) {
            return "";
        }
    }

    private String value(OfflinePlayer player, String params) {
        if (params.startsWith("top_")) {
            String[] parts = params.split("_", 3);
            if (parts.length < 3 || !parts[1].matches("\\d+")) {
                return "";
            }
            int index = Integer.parseInt(parts[1]) - 1;
            List<Team> sorted = plugin.getTeams().sorted();
            if (index < 0 || index >= sorted.size()) {
                return parts[2].equals("kills") || parts[2].equals("members") ? "0" : "---";
            }
            Team t = sorted.get(index);
            return switch (parts[2]) {
                case "name" -> t.getName();
                case "colored" -> t.colored();
                case "kills" -> String.valueOf(t.getKills());
                case "members" -> String.valueOf(t.size());
                default -> "";
            };
        }

        Team team = player == null ? null : plugin.getTeams().getTeam(player.getUniqueId());
        if (params.equals("has_team")) {
            return team == null ? "no" : "yes";
        }
        if (team == null) {
            return switch (params) {
                case "members", "online", "kills" -> "0";
                case "max_members" -> String.valueOf(plugin.getTeams().maxMembers());
                default -> "";
            };
        }
        Team.Role role = team.getRole(player.getUniqueId());
        return switch (params) {
            case "name" -> team.getName();
            case "tag" -> team.tag() + " ";
            case "colored" -> team.colored();
            case "color" -> team.getColor();
            case "role" -> role == null ? "" : role.label();
            case "role_icon" -> role == null ? "" : role.icon().trim();
            case "members" -> String.valueOf(team.size());
            case "max_members" -> String.valueOf(plugin.getTeams().maxMembers());
            case "online" -> String.valueOf(team.online().size());
            case "kills" -> String.valueOf(team.getKills());
            case "bank" -> Text.money(team.getBank());
            case "bank_short" -> Text.shortMoney(team.getBank());
            default -> null;
        };
    }
}
