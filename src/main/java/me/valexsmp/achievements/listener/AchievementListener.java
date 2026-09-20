package me.valexsmp.achievements.listener;

import me.valexsmp.achievements.ValexAchievements;
import me.valexsmp.achievements.achievement.Achievement;
import me.valexsmp.achievements.gui.AchievementHolder;
import me.valexsmp.achievements.player.PlayerAchievementData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class AchievementListener implements Listener {
    private final ValexAchievements plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();

    private static final Set<Material> ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE, Material.ANCIENT_DEBRIS
    );

    public AchievementListener(ValexAchievements plugin) { this.plugin = plugin; }

    @EventHandler public void onJoin(PlayerJoinEvent event) { plugin.getPlayerData(event.getPlayer().getUniqueId()); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { plugin.unloadPlayer(event.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMine(BlockBreakEvent event) { if (ORES.contains(event.getBlock().getType())) progress(event.getPlayer(), Achievement.AchievementType.MINE_ORE, 1); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) progress(killer, Achievement.AchievementType.KILL_MOB, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && event.getCaught() instanceof Item) progress(event.getPlayer(), Achievement.AchievementType.CATCH_FISH, 1);
    }

    private void progress(Player player, Achievement.AchievementType type, long amount) {
        PlayerAchievementData data = plugin.getPlayerData(player.getUniqueId());
        for (Achievement a : plugin.achievements().all()) {
            if (a.type() != type || data.completed(a.id())) continue;
            long next = Math.min(a.target(), data.progress(a.id()) + amount);
            data.setProgress(a.id(), next);
            if (next >= a.target()) complete(player, data, a);
            else if (plugin.getConfig().getBoolean("settings.notify-progress", true)) notifyProgress(player, a, next);
        }
    }

    private void complete(Player player, PlayerAchievementData data, Achievement a) {
        data.complete(a.id());
        String msg = plugin.getConfig().getString("messages.completed", "&a%achievement_name% unlocked!").replace("%achievement_name%", a.name());
        player.sendMessage(serializer.deserialize(msg));
        if (plugin.getConfig().getBoolean("settings.give-title-on-completion", true) && !a.title().isBlank()) {
            String title = plugin.getConfig().getString("messages.title-unlocked", "&fTitle: %title%").replace("%title%", a.title());
            player.sendMessage(serializer.deserialize(title));
        }
        for (String command : a.rewardCommands()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()).replace("%uuid%", player.getUniqueId().toString()));
        plugin.savePlayer(player.getUniqueId());
    }

    private void notifyProgress(Player player, Achievement a, long progress) {
        String msg = plugin.getConfig().getString("messages.progress", "%achievement_name% %progress%/%target%")
                .replace("%achievement_name%", a.name()).replace("%progress%", String.valueOf(progress)).replace("%target%", String.valueOf(a.target()));
        player.sendMessage(serializer.deserialize(msg));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof AchievementHolder holder)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
        switch (holder.view()) {
            case MAIN -> handleMain(player, slot);
            case CATEGORY -> handleCategory(player, holder.category(), slot);
            case TITLES -> handleTitles(player, slot);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) { if (event.getView().getTopInventory().getHolder() instanceof AchievementHolder) event.setCancelled(true); }

    private void handleMain(Player player, int slot) {
        var cfg = plugin.getConfig();
        var gui = new org.bukkit.configuration.file.YamlConfiguration();
        gui = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.File(plugin.getDataFolder(), "gui.yml"));
        if (slot == gui.getInt("items.close.slot", 49)) { player.closeInventory(); return; }
        if (slot == gui.getInt("items.titles.slot", 40)) { plugin.gui().openTitles(player); return; }
        if (slot == gui.getInt("items.mining.slot", 20)) { plugin.gui().openCategory(player, findCategory("items.mining", "ᴍɪɴɪɴɢ")); return; }
        if (slot == gui.getInt("items.hunter.slot", 22)) { plugin.gui().openCategory(player, findCategory("items.hunter", "ʜᴜɴᴛᴇʀ")); return; }
        if (slot == gui.getInt("items.fishing.slot", 24)) { plugin.gui().openCategory(player, findCategory("items.fishing", "ғɪsʜɪɴɢ")); }
    }

    private String findCategory(String path, String fallback) {
        var gui = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.File(plugin.getDataFolder(), "gui.yml"));
        String display = gui.getString(path + ".name", fallback);
        for (Achievement a : plugin.achievements().all()) if (display.toLowerCase(Locale.ROOT).contains(a.category().toLowerCase(Locale.ROOT))) return a.category();
        return fallback;
    }

    private void handleCategory(Player player, String category, int slot) {
        var gui = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.File(plugin.getDataFolder(), "gui.yml"));
        if (slot == gui.getInt("category.back.slot", 49)) { plugin.gui().openMain(player); }
    }

    private void handleTitles(Player player, int slot) {
        var gui = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.File(plugin.getDataFolder(), "gui.yml"));
        if (slot == gui.getInt("titles.back.slot", 49)) { plugin.gui().openMain(player); return; }
        PlayerAchievementData data = plugin.getPlayerData(player.getUniqueId());
        List<Achievement> all = plugin.achievements().all().stream().filter(a -> !a.title().isBlank()).toList();
        int current = 0;
        for (Achievement a : all) {
            while (current < 54 && current == gui.getInt("titles.back.slot", 49)) current++;
            if (current++ == slot && data.completed(a.id())) {
                data.activeTitle(a.title());
                player.sendMessage(serializer.deserialize(plugin.getConfig().getString("messages.title-selected", "&fTitle selected: %title%").replace("%title%", a.title())));
                plugin.savePlayer(player.getUniqueId());
                plugin.gui().openTitles(player);
                return;
            }
        }
    }
}
