package me.valexsmp.achievements.player;

import java.util.*;

public final class PlayerAchievementData {
    private final UUID uuid;
    private final Map<String, Long> progress = new HashMap<>();
    private final Set<String> completed = new HashSet<>();
    private String activeTitle = "";

    public PlayerAchievementData(UUID uuid) { this.uuid = uuid; }
    public UUID uuid() { return uuid; }
    public Map<String, Long> progress() { return progress; }
    public Set<String> completed() { return completed; }
    public String activeTitle() { return activeTitle; }
    public void activeTitle(String title) { activeTitle = title == null ? "" : title; }
    public long progress(String id) { return progress.getOrDefault(id, 0L); }
    public boolean completed(String id) { return completed.contains(id); }
    public void setProgress(String id, long value) { progress.put(id, Math.max(0, value)); }
    public void complete(String id) { completed.add(id); }
}
