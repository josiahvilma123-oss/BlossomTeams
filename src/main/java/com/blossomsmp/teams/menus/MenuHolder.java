package com.blossomsmp.teams.menus;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Marks an inventory as a BlossomTeams menu and remembers what each slot does. */
public class MenuHolder implements InventoryHolder {

    private final Map<Integer, Consumer<Player>> actions = new HashMap<>();
    private final Map<Integer, Consumer<Player>> rightActions = new HashMap<>();
    private Inventory inventory;

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /** Puts an item in a slot. The action runs when it's clicked (can be null). */
    public void set(int slot, ItemStack item, Consumer<Player> action) {
        inventory.setItem(slot, item);
        if (action == null) {
            actions.remove(slot);
        } else {
            actions.put(slot, action);
        }
    }

    /** A different action for right-clicking a slot. */
    public void setRightClick(int slot, Consumer<Player> action) {
        rightActions.put(slot, action);
    }

    public Consumer<Player> action(int slot, boolean rightClick) {
        if (rightClick && rightActions.containsKey(slot)) {
            return rightActions.get(slot);
        }
        return actions.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
