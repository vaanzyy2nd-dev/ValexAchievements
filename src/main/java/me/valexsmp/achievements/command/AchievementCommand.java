package me.valexsmp.achievements.command;

import me.valexsmp.achievements.ValexAchievements;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public final class AchievementCommand implements CommandExecutor, TabCompleter {
    private final ValexAchievements plugin;
    public AchievementCommand(ValexAchievements plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("valexachievements.admin")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
            plugin.reloadConfig(); plugin.achievements().load(); plugin.gui().reload(); sender.sendMessage(ChatColor.GREEN + "ValexAchievements reloaded."); return true;
        }
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (!player.hasPermission("valexachievements.use")) return true;
        plugin.gui().openMain(player);
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? List.of("reload") : List.of();
    }
}
