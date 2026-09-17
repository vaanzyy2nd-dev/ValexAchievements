package me.valexsmp.achievements;

import me.valexsmp.achievements.achievement.AchievementManager;
import me.valexsmp.achievements.command.AchievementCommand;
import me.valexsmp.achievements.data.DatabaseManager;
import me.valexsmp.achievements.data.PlayerDataManager;
import me.valexsmp.achievements.listener.AchievementListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ValexAchievements extends JavaPlugin {
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private AchievementManager achievementManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("achievements.yml", false);

        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Database initialization failed. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.playerDataManager = new PlayerDataManager(this, databaseManager);
        this.achievementManager = new AchievementManager(this, playerDataManager);
        achievementManager.load();

        getServer().getPluginManager().registerEvents(
                new AchievementListener(achievementManager, playerDataManager), this
        );

        AchievementCommand achievementCommand = new AchievementCommand(this, achievementManager, playerDataManager);
        PluginCommand command = getCommand("achievement");
        if (command != null) {
            command.setExecutor(achievementCommand);
            command.setTabCompleter(achievementCommand);
        }

        playerDataManager.loadOnlinePlayers();
        playerDataManager.startAutosave();

        getLogger().info("ValexAchievements enabled. Loaded " + achievementManager.size() + " achievements.");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllOnline();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
