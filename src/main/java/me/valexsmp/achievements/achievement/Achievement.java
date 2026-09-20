package me.valexsmp.achievements.achievement;

import java.util.List;
import java.util.Map;

public record Achievement(String id, String category, String name, List<String> description,
                          AchievementType type, long target, String title, List<String> rewardCommands) {
    public enum AchievementType { MINE_ORE, KILL_MOB, CATCH_FISH }
}
