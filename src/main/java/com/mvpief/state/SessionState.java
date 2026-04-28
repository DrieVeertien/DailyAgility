package com.mvpief.state;

import com.mvpief.DailyAgilityConfig;
import lombok.Getter;
import lombok.Setter;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SessionState {
    @Inject private DailyAgilityConfig config;

    @Getter @Setter private String currentCourse = null;
    @Getter @Setter private int lastMarkCount = 0;
    @Getter @Setter private boolean inventoryInitialized = false;

    public boolean isGoalAchieved()
    {
        return config.lapsRemaining() + config.marksRemaining() <= 0;
    }

    public boolean isRunning()
    {
        return currentCourse != null;
    }

    public void reset()
    {
        currentCourse = null;
        lastMarkCount = 0;
        inventoryInitialized = false;
    }
}
