package me.valexsmp.achievements;

import me.valexsmp.achievements.achievement.AchievementManager;
import me.valexsmp.achievements.command.AchievementCommand;
import me.valexsmp.achievements.data.DatabaseManager;
import me.valexsmp.achievements.gui.AchievementGUI;
import me.valexsmp.achievements.listener.AchievementListener;
import me.valexsmp.achievements.player.PlayerAchievementData;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ValexAchievements extends JavaPlugin {
    private DatabaseManager database;
    private AchievementManager achievementManager;
    private AchievementGUI gui;
    private final Map<UUID, PlayerAchievementData> playerData = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("achievements.yml", false);
        saveResource("gui.yml", false);

        database = new DatabaseManager(this);
        database.initialize();
        achievementManager = new AchievementManager(this);
        achievementManager.load();
        gui = new AchievementGUI(this);

        AchievementCommand command = new AchievementCommand(this);
        getCommand("achievement").setExecutor(command);
        getCommand("achievement").setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new AchievementListener(this), this);
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::saveAll, 20L * getConfig().getLong("settings.autosave-seconds", 60), 20L * getConfig().getLong("settings.autosave-seconds", 60));
    }

    @Override
    public void onDisable() {
        saveAll();
        if (database != null) database.close();
    }

    public PlayerAchievementData getPlayerData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, id -> database.load(id));
    }

    public void savePlayer(UUID uuid) {
        PlayerAchievementData data = playerData.get(uuid);
        if (data != null) database.save(data);
    }

    public void unloadPlayer(UUID uuid) {
        PlayerAchievementData data = playerData.remove(uuid);
        if (data != null) database.save(data);
    }

    public void saveAll() {
        playerData.values().forEach(database::save);
    }

    public AchievementManager achievements() { return achievementManager; }
    public AchievementGUI gui() { return gui; }
}
