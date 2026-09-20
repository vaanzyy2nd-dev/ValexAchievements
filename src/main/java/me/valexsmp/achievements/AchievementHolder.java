package me.valexsmp.achievements.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class AchievementHolder implements InventoryHolder {
    private final View view;
    private final String category;
    private Inventory inventory;

    public enum View { MAIN, CATEGORY, TITLES }
    public AchievementHolder(View view, String category) { this.view = view; this.category = category; }
    public View view() { return view; }
    public String category() { return category; }
    public void inventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
