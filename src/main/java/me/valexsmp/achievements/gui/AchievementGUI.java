package me.valexsmp.achievements.gui;

import me.valexsmp.achievements.ValexAchievements;
import me.valexsmp.achievements.achievement.Achievement;
import me.valexsmp.achievements.player.PlayerAchievementData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public final class AchievementGUI {
    private final ValexAchievements plugin;
    private YamlConfiguration cfg;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    public AchievementGUI(ValexAchievements plugin) { this.plugin = plugin; reload(); }

    public void reload() { cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "gui.yml")); }

    public void openMain(Player player) {
        int size = validSize(cfg.getInt("main.size", 54));
        AchievementHolder holder = new AchievementHolder(AchievementHolder.View.MAIN, "");
        Inventory inv = Bukkit.createInventory(holder, size, text(cfg.getString("main.title", "&5Achievements")));
        holder.inventory(inv);
        fill(inv, "items.background");
        addConfigured(inv, "items.mining");
        addConfigured(inv, "items.hunter");
        addConfigured(inv, "items.fishing");
        addConfigured(inv, "items.titles");
        addConfigured(inv, "items.close");
        player.openInventory(inv);
    }

    public void openCategory(Player player, String category) {
        int size = validSize(cfg.getInt("category.size", 54));
        AchievementHolder holder = new AchievementHolder(AchievementHolder.View.CATEGORY, category);
        Inventory inv = Bukkit.createInventory(holder, size, text(replace(cfg.getString("category.title", "&5%category%"), "%category%", category)));
        holder.inventory(inv);
        fill(inv, "category.background");
        int slot = 0;
        PlayerAchievementData data = plugin.getPlayerData(player.getUniqueId());
        for (Achievement a : plugin.achievements().byCategory(category)) {
            while (slot < size && (slot == cfg.getInt("category.back.slot", 49))) slot++;
            if (slot >= size) break;
            inv.setItem(slot++, achievementItem(a, data));
        }
        addConfigured(inv, "category.back");
        player.openInventory(inv);
    }

    public void openTitles(Player player) {
        int size = validSize(cfg.getInt("titles.size", 54));
        AchievementHolder holder = new AchievementHolder(AchievementHolder.View.TITLES, "");
        Inventory inv = Bukkit.createInventory(holder, size, text(cfg.getString("titles.title", "&5Titles")));
        holder.inventory(inv);
        fill(inv, "titles.background");
        PlayerAchievementData data = plugin.getPlayerData(player.getUniqueId());
        List<Achievement> all = plugin.achievements().all().stream().filter(a -> !a.title().isBlank()).toList();
        int slot = 0;
        for (Achievement a : all) {
            while (slot < size && slot == cfg.getInt("titles.back.slot", 49)) slot++;
            if (slot >= size) break;
            boolean unlocked = data.completed(a.id());
            ConfigurationSection section = cfg.getConfigurationSection(unlocked ? "titles.unlocked" : "titles.empty");
            if (section == null) continue;
            String materialName = section.getString("material", unlocked ? "NAME_TAG" : "GRAY_DYE");
            ItemStack item = item(materialName, section.getString("name", "%title%").replace("%title%", a.title()),
                    section.getStringList("lore").stream().map(s -> replace(s, "%selected%", unlocked && a.title().equals(data.activeTitle()) ? "&a✓ ᴇǫᴜɪᴘᴘᴇᴅ" : "&7ᴜɴᴇǫᴜɪᴘᴘᴇᴅ")).toList());
            inv.setItem(slot++, item);
        }
        addConfigured(inv, "titles.back");
        player.openInventory(inv);
    }

    private ItemStack achievementItem(Achievement a, PlayerAchievementData data) {
        boolean done = data.completed(a.id());
        String base = done ? "category.achievement.unlocked-material" : "category.achievement.locked-material";
        String material = cfg.getString(base, done ? "LIME_DYE" : "GRAY_DYE");
        long progress = Math.min(data.progress(a.id()), a.target());
        String bar = progressBar(progress, a.target());
        List<String> lore = new ArrayList<>();
        for (String line : cfg.getStringList("category.achievement.lore")) {
            if (line.equals("%description%")) lore.addAll(a.description());
            else if (line.equals("%reward%")) lore.add(a.rewardCommands().isEmpty() ? "&7ʀᴇᴡᴀʀᴅ: &fɴᴏɴᴇ" : "&7ʀᴇᴡᴀʀᴅ: &fᴜɴʟᴏᴄᴋᴇᴅ ᴄᴏᴍᴍᴀɴᴅs");
            else lore.add(replace(line, "%progress%", String.valueOf(progress)).replace("%target%", String.valueOf(a.target())).replace("%bar%", bar));
        }
        String name = replace(cfg.getString("category.achievement.name", "%achievement_name%"), "%achievement_name%", a.name()).replace("%status%", done ? "&a✓" : "&7○");
        return item(material, name, lore);
    }

    private String progressBar(long progress, long target) {
        int length = Math.max(1, cfg.getInt("progress.length", 10));
        int done = (int) Math.round((double) progress / Math.max(1, target) * length);
        String yes = cfg.getString("progress.completed", "&a■");
        String no = cfg.getString("progress.incomplete", "&7■");
        return yes.repeat(Math.max(0, done)) + no.repeat(Math.max(0, length - done));
    }

    private void fill(Inventory inv, String path) { for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, configured(path)); }
    private void addConfigured(Inventory inv, String path) { if (cfg.contains(path)) inv.setItem(cfg.getInt(path + ".slot", 0), configured(path)); }
    private ItemStack configured(String path) { return item(cfg.getString(path + ".material", "STONE"), cfg.getString(path + ".name", " "), cfg.getStringList(path + ".lore")); }
    private ItemStack item(String materialName, String name, List<String> lore) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.STONE;
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(text(name));
        meta.lore(lore.stream().map(this::text).toList());
        stack.setItemMeta(meta);
        return stack;
    }
    private net.kyori.adventure.text.Component text(String s) { return serializer.deserialize(s == null ? "" : s); }
    private String replace(String value, String key, String replacement) { return value == null ? "" : value.replace(key, replacement); }
    private int validSize(int size) { return Math.max(9, Math.min(54, (size / 9) * 9)); }
}
