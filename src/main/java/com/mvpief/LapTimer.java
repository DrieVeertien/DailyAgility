package com.mvpief;

import net.runelite.client.config.ConfigManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LapTimer
{
    private static final int MAX_LAP_HISTORY = 100;

    private static final long MAX_LAP_DURATION_MS = 5 * 60 * 1000; // 10 minutes

    private final ConfigManager configManager;

    private long lastLapTime = -1;
    @Getter private long lastLapDuration = -1;

    @Inject
    public LapTimer(ConfigManager configManager)
    {
        this.configManager = configManager;
    }

    public void recordLap(String course)
    {
        long now = System.currentTimeMillis();
        if (lastLapTime != -1)
        {
            long duration = now - lastLapTime;
            if (duration <= MAX_LAP_DURATION_MS)
            {
                lastLapDuration = duration;
                saveLapTime(course, lastLapDuration);
            }
            // else: gap too large (AFK/logged out) — skip timing, just re-baseline below.
        }
        lastLapTime = now;
    }

    public double getAverageLapTime(String course)
    {
        List<Long> history = getLapHistory(course);
        if (history.isEmpty()) return -1;
        return history.stream().mapToLong(Long::longValue).average().orElse(-1);
    }

    public void reset()
    {
        lastLapTime = -1;
        lastLapDuration = -1;
    }

    private List<Long> getLapHistory(String course)
    {
        String key = "lapHistory_" + course.replace(" ", "_");
        String stored = configManager.getConfiguration(DailyAgilityConfig.GROUP, key);
        if (stored == null || stored.isEmpty()) return new ArrayList<>();
        return Arrays.stream(stored.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    private void saveLapTime(String course, long duration)
    {
        List<Long> history = getLapHistory(course);
        history.add(duration);
        if (history.size() > MAX_LAP_HISTORY)
        {
            history = history.subList(history.size() - MAX_LAP_HISTORY, history.size());
        }
        String key = "lapHistory_" + course.replace(" ", "_");
        String value = history.stream().map(String::valueOf).collect(Collectors.joining(","));
        configManager.setConfiguration(DailyAgilityConfig.GROUP, key, value);
    }
}