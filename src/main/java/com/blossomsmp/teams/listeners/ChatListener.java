package com.blossomsmp.teams.listeners;

import com.blossomsmp.teams.BlossomTeams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Answers to chat questions (team name, amounts) and team chat mode.
 *
 * Uses the older chat event on purpose: Paper fires it BEFORE the new one, so we get the
 * message before any chat plugin (BlossomChat, DiscordSRV...) can format or send it.
 */
@SuppressWarnings("deprecation")
public class ChatListener implements Listener {

    private final BlossomTeams plugin;

    public ChatListener(BlossomTeams plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String text = event.getMessage();

        // Typing an answer to a menu question
        if (plugin.getPrompts().handle(player, text)) {
            hide(event);
            return;
        }

        if (!plugin.isTeamChat(player.getUniqueId())) {
            return;
        }
        // "!hello" while team chat is on goes to normal chat
        String global = plugin.getConfig().getString("chat.global-prefix", "!");
        if (!global.isEmpty() && text.startsWith(global)) {
            String rest = text.substring(global.length()).trim();
            if (rest.isEmpty()) {
                hide(event);
            } else {
                event.setMessage(rest);
            }
            return;
        }
        hide(event);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                plugin.getActions().teamChat(player, text);
            }
        });
    }

    private void hide(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        try {
            event.getRecipients().clear();
        } catch (UnsupportedOperationException ignored) {
            // Some servers give a read-only list; cancelling is enough
        }
    }
}
