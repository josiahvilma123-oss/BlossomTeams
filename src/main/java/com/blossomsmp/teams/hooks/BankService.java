package com.blossomsmp.teams.hooks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Talks to the server's money plugin (BlossomEconomy) through Vault.
 * Only created when Vault is installed.
 */
public class BankService {

    private Economy economy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : rsp.getProvider();
    }

    public boolean isReady() {
        return economy() != null;
    }

    public double balance(OfflinePlayer player) {
        Economy economy = economy();
        return economy == null ? 0 : economy.getBalance(player);
    }

    public boolean has(OfflinePlayer player, double amount) {
        Economy economy = economy();
        return economy != null && economy.has(player, amount);
    }

    public boolean take(OfflinePlayer player, double amount) {
        Economy economy = economy();
        return economy != null && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean give(OfflinePlayer player, double amount) {
        Economy economy = economy();
        return economy != null && economy.depositPlayer(player, amount).transactionSuccess();
    }
}
