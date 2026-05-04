package com.mvpief;
import com.mvpief.state.SessionState;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

@Slf4j
public class DailyGoalOverlay extends OverlayPanel
{
    // region Setup

    private final DailyAgility plugin;

    @Inject
    private DailyAgilityConfig config;
    @Inject private SessionState sessionState;

    @Inject
    public DailyGoalOverlay(DailyAgility plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    // endregion

    // region Render

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().clear();
        if (!shouldRender()) return super.render(graphics);

        try
        {
            renderLapsLeft();
            renderMarksLeft();
            renderLastLapTime();
            String course = plugin.getCurrentCourse();
            renderAvgLapTime(course);
            renderETA(course);
        }
        catch (Exception e)
        {
            log.error("DailyGoalOverlay render error", e);
        }

        return super.render(graphics);
    }

    // endregion

    // region Render Helpers

    private void renderLapsLeft()
    {
        if (!config.showLapsLeft()) return;
        int laps = plugin.getLapsLeft();
        String value;
        Color color;

        if (laps < 0)
        {
            value = "+" + Math.abs(laps);
            color = Color.GREEN;
        }
        else
        {
            value = String.valueOf(laps);
            color = Color.WHITE;
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Laps left:")
                .right(value)
                .rightColor(color)
                .build());
    }

    private void renderMarksLeft()
    {
        if (!config.showMarksLeft()) return;
        int marks = plugin.getMarksLeft();
        String value;
        Color color;

        if (marks < 0)
        {
            value = "+" + Math.abs(marks);
            color = Color.GREEN;
        }
        else
        {
            value = String.valueOf(marks);
            color = Color.WHITE;
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Marks left:")
                .right(value)
                .rightColor(color)
                .build());
    }

    private void renderLastLapTime()
    {
        if (!config.showLapTime()) return;
        long duration = plugin.getLastLapDuration();
        if (duration == -1) return;

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Last lap:")
                .right(formatSeconds(duration))
                .build());
    }

    private void renderAvgLapTime(String course)
    {
        if (course == null || !config.showAverageTime()) return;
        double avg = plugin.getAverageLapTime(course);
        if (avg <= 0) return;

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Avg lap:")
                .right(formatSeconds((long) avg))
                .build());
    }

    private void renderETA(String course)
    {
        if (course == null || !config.showETA()) return;
        long eta = plugin.getEstimatedTimeRemainingMs();
        if (eta <= 0) return;

        panelComponent.getChildren().add(LineComponent.builder()
                .left("ETA:")
                .right(formatSeconds(eta))
                .build());
    }

    private boolean shouldRender()
    {
        OverlayDisplayMode mode = config.overlayDisplayMode();

        switch (mode) {
            case ALWAYS:
                return true;
            case ON_GOAL:
                return !sessionState.isGoalAchieved();
            case ON_RUNNING:
                return sessionState.isRunning();
            case NEVER:
                return false;
        }
        return false;

    }

    // endregion

    // region Utilities

    private String formatSeconds(long ms)
    {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) return hours + "h " + minutes + "m " + seconds + "s";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";

    }

    // endregion
}