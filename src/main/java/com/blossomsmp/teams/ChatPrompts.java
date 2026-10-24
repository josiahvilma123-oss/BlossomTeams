package com.blossomsmp.teams;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Asks a player to type something in chat (a team name, an amount...). */
public class ChatPrompts {

    private record Prompt(Consumer<String> callback, long expires) {
    }

    private final BlossomTeams plugin;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public ChatPrompts(BlossomTeams plugin) {
        this.plugin = plugin;
    }

    public void ask(Player player, String question, Consumer<String> callback) {
        player.closeInventory();
        prompts.put(player.getUniqueId(), new Prompt(callback, System.currentTimeMillis() + 60_000L));
        player.sendMessage(plugin.msg(question));
        player.sendMessage(plugin.msg("&7Type it in chat, or type &fcancel&7."));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
    }

    /**
     * Called from the (async) chat event. Returns true if the message was an answer,
     * in which case the chat message must be cancelled.
     */
    public boolean handle(Player player, String message) {
        Prompt prompt = prompts.remove(player.getUniqueId());
        if (prompt == null || prompt.expires() < System.currentTimeMillis()) {
            return false;
        }
        String answer = message.trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (answer.equalsIgnoreCase("cancel")) {
                player.sendMessage(plugin.msg("&7Cancelled."));
                return;
            }
            prompt.callback().accept(answer);
        });
        return true;
    }

    public void clear(UUID player) {
        prompts.remove(player);
    }
}
