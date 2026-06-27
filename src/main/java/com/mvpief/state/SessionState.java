package com.mvpief.state;

import com.mvpief.DailyAgilityConfig;
import lombok.Getter;
import lombok.Setter;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SessionState {
    // Consider the player "running" only if a lap was completed within this window.
    private static final long RUNNING_TIMEOUT_MS = 10 * 60 * 1000; // 10 minutes

    @Inject private DailyAgilityConfig config;

    @Getter @Setter private String currentCourse = null;
    @Getter @Setter private int lastMarkCount = 0;
    @Getter @Setter private boolean inventoryInitialized = false;

    // Timestamp (ms) of the last lap completion; 0 means no activity this session.
    private long lastActivityMs = 0;

    public boolean isGoalAchieved()
    {
        return config.lapsRemaining() + config.marksRemaining() <= 0;
    }

    /** Marks the player as actively running right now. Call on each completed lap. */
    public void markActivity()
    {
        lastActivityMs = System.currentTimeMillis();
    }

    public boolean isRunning()
    {
        return currentCourse != null
                && lastActivityMs != 0
                && (System.currentTimeMillis() - lastActivityMs) < RUNNING_TIMEOUT_MS;
    }

    public void reset()
    {
        currentCourse = null;
        lastMarkCount = 0;
        inventoryInitialized = false;
        lastActivityMs = 0;
    }
}
