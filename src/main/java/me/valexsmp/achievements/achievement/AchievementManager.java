package me.valexsmp.achievements.achievement;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import me.valexsmp.achievements.ValexAchievements;

import java.io.File;
import java.util.*;

public final class AchievementManager {
    private final ValexAchievements plugin;
    private final Map<String, Achievement> achievements = new LinkedHashMap<>();

    public AchievementManager(ValexAchievements plugin) { this.plugin = plugin; }

    public void load() {
        achievements.clear();
        File file = new File(plugin.getDataFolder(), "achievements.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("achievements");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            String path = "achievements." + id;
            try {
                Achievement achievement = new Achievement(
                        id,
                        cfg.getString(path + ".category", "ᴏᴛʜᴇʀ"),
                        cfg.getString(path + ".name", id),
                        cfg.getStringList(path + ".description"),
                        Achievement.AchievementType.valueOf(cfg.getString(path + ".type", "MINE_ORE").toUpperCase(Locale.ROOT)),
                        Math.max(1, cfg.getLong(path + ".target", 1)),
                        cfg.getString(path + ".title", ""),
                        cfg.getStringList(path + ".reward.commands")
                );
                achievements.put(id, achievement);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid achievement type for " + id);
            }
        }
    }

    public Achievement get(String id) { return achievements.get(id); }
    public Collection<Achievement> all() { return Collections.unmodifiableCollection(achievements.values()); }
    public List<Achievement> byCategory(String category) {
        return achievements.values().stream().filter(a -> a.category().equalsIgnoreCase(category)).toList();
    }
    public List<String> categories() {
        return achievements.values().stream().map(Achievement::category).distinct().toList();
    }
}
