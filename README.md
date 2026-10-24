# ❀ BlossomTeams

Pink teams for Blossom SMP (Paper 1.21.11, Java 21). Replaces BetterTeams.

## What it does
- **Pink menu**: type `/team` and do everything by clicking
- **Make a team** (type the name in chat), **invite** online players by clicking their head
- **Invites** with clickable [ACCEPT] / [DENY] in chat
- **Roles**: Owner ♛, Admin ★, Member. The owner can promote, demote, kick and give the team away
- **Team chat**: `/tc <message>`, or `/tc` to switch it on (start a message with `!` to talk to everyone)
- **Team home** with a 5 second stand-still warmup, cooldown, and no teleporting in combat (BlossomCombat)
- **Team bank** using BlossomEconomy money: everyone can put money in, owner and admins can take it out
- **Friendly fire** on/off per team (swords, arrows, tridents, TNT, lingering potions)
- **Team colour** picker (14 colours), **open or invite-only** teams
- **Team kills** and a **Top Teams** menu
- **Max team size** (3 by default, good for a ranked SMP)
- Placeholders for TAB, holograms and chat
- Staff: `/team admin` to disband teams, spy on team chat, reload

## Build the .jar (GitHub, no installing)
1. Make a new repository on GitHub and upload **everything inside this folder**, including the `.github` folder.
   `pom.xml` and `src` must be on the main page of the repo, not inside another folder.
2. Open the **Actions** tab and wait for the green tick.
3. Click the finished run, scroll down and download **BlossomTeams** (a zip with `BlossomTeams.jar` inside).

## Install
1. Stop the server.
2. Remove **BetterTeams** (and BetterTeamsAddon) from `plugins` if they're there, so `/team` isn't taken twice.
3. Upload `BlossomTeams.jar` to `plugins` and start the server.
4. The console should say `Hooked into Vault (team bank is on)` and `Hooked into PlaceholderAPI`.

Settings are in `plugins/BlossomTeams/config.yml`. After editing, run `/team admin reload`.
Teams are saved in `plugins/BlossomTeams/teams.yml`.

## Commands
| Command | What it does |
|---|---|
| `/team` | Open the team menu |
| `/team create <name>` | Make a team |
| `/team invite <player>` | Invite someone (owner/admins) |
| `/team accept <team>` / `/team deny <team>` | Answer an invite (also joins open teams) |
| `/team leave` | Leave your team |
| `/team kick / promote / demote / transfer <player>` | Manage members |
| `/team home`, `/team sethome`, `/team delhome` | Team home |
| `/tc <message>` or `/team chat` | Team chat |
| `/team ff` | Friendly fire on/off |
| `/team open` | Open / invite only |
| `/team color` | Pick a colour |
| `/team rename <name>` | Rename (owner) |
| `/team deposit <amount>`, `/team withdraw <amount>` | Team bank (`2k`, `1m` work) |
| `/team info [team]`, `/team top` | Team info and best teams |
| `/team disband` | Delete your team (asks first) |
| `/team admin reload / disband <team> / setkills <team> <n> / spy / save` | Staff (`blossomteams.admin`) |

## Placeholders
| Placeholder | Example |
|---|---|
| `%blossomteams_tag%` | `[Sakura] ` in the team colour (empty without a team) |
| `%blossomteams_name%` / `%blossomteams_colored%` | `Sakura` |
| `%blossomteams_role%` / `%blossomteams_role_icon%` | `Owner` / `♛` |
| `%blossomteams_members%` / `%blossomteams_max_members%` / `%blossomteams_online%` | `2` / `3` / `1` |
| `%blossomteams_kills%` | `14` |
| `%blossomteams_bank%` / `%blossomteams_bank_short%` | `$12,500` / `$12.5K` |
| `%blossomteams_has_team%` | `yes` / `no` |
| `%blossomteams_top_1_name%` (1-10), `_colored`, `_kills`, `_members` | best teams, for a hologram |

**Show the team tag in TAB:** in `plugins/TAB/groups.yml`, add `%blossomteams_tag%` to the start of `tabsuffix`
or the end of `tabprefix`, then `/tab reload`.

**Top teams hologram:**
```
/dh create topteams <#FF1493>&l✿ TOP TEAMS ✿</#FFB6C1>
/dh line add topteams 1 &#FFD700&l#1 %blossomteams_top_1_colored% &#FFB6C1- &f%blossomteams_top_1_kills% kills
/dh line add topteams 1 &#C0C0C0&l#2 %blossomteams_top_2_colored% &#FFB6C1- &f%blossomteams_top_2_kills% kills
/dh line add topteams 1 &#CD7F32&l#3 %blossomteams_top_3_colored% &#FFB6C1- &f%blossomteams_top_3_kills% kills
```

## Good to know
- Friendly fire is **off** by default, so teammates can't kill each other to swap RankedSMP ranks.
- Disbanding gives the team bank money to the owner.
- Team chat is logged in the console, and staff with `/team admin spy` can read it.
- Commands that need a team member's name use the name they had when they were last online.
