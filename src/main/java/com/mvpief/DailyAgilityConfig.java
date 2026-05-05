package com.mvpief;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(DailyAgilityConfig.GROUP)
public interface DailyAgilityConfig extends Config
{
	String GROUP = "dailyAgility";

	@ConfigSection(name = "Laps", description = "Lap goal and display settings", position = 0)
	String lapsSection = "laps";

	@ConfigSection(name = "Marks of Grace", description = "Marks of Grace goal settings", position = 1)
	String marksSection = "marks";

	@ConfigSection(name = "Display", description = "Overlay display options", position = 2)
	String displaySection = "display";

	@ConfigSection(name = "Logging", description = "Logging options", position = 3)
	String loggingSection = "logging";

	// --- Laps ---
	@ConfigItem(keyName = "dailyGoal", name = "Daily Lap Goal", description = "How many rooftop laps to complete today.", position = 0, section = lapsSection)
	default int dailyGoal() { return 100; }

	@ConfigItem(keyName = "lapsRemaining", name = "", description = "", hidden = true, section = lapsSection)
	default int lapsRemaining() { return -1; }

	// --- Marks ---
	@ConfigItem(keyName = "marksGoal", name = "Daily Marks Goal", description = "Marks of Grace to collect today.", position = 0, section = marksSection)
	default int marksGoal() { return 10; }

	@ConfigItem(keyName = "marksRemaining", name = "", description = "", hidden = true, section = marksSection)
	default int marksRemaining() { return 0; }

	// --- Display ---
	@ConfigItem(
			keyName = "overlayDisplayMode",
			name = "Overlay Mode",
			description = "Controls which overlay panels are rendered.",
			position = 0,
			section = displaySection
	)
	default OverlayDisplayMode overlayDisplayMode() { return OverlayDisplayMode.ALWAYS; }

	@ConfigItem(keyName = "showLapsLeft", name = "Show Laps Left", description = "Show the laps left on the overlay.", position = 0, section = displaySection)
	default boolean showLapsLeft() { return true; }

	@ConfigItem(keyName = "showMarksLeft", name = "Show Marks Left", description = "Show the marks left on the overlay.", position = 1, section = displaySection)
	default boolean showMarksLeft() { return true; }

	@ConfigItem(keyName = "showLapTime", name = "Show Last Lap Time", description = "Show the last lap time on the overlay.", position = 2, section = displaySection)
	default boolean showLapTime() { return true; }

	@ConfigItem(keyName = "showAverageTime", name = "Show Average Time", description = "Show average lap time on the overlay.", position = 3, section = displaySection)
	default boolean showAverageTime() { return true; }

	@ConfigItem(keyName = "showETA", name = "Show ETA", description = "Show estimated time remaining on the overlay.", position = 4, section = displaySection)
	default boolean showETA() { return true; }

	@ConfigItem(keyName = "logEntries", name = "Log daily laps", description = "Logs the daily laps and mark of grace.", position = 0, section = loggingSection)
	default boolean logEntries() { return true; }

	// --- Internal ---
	@ConfigItem(keyName = "lastSavedDate", name = "", description = "", hidden = true)
	default String lastSavedDate() { return ""; }

	@ConfigItem(keyName = "lastKnownCourse", name = "", description = "", hidden = true)
	default String lastKnownCourse() { return ""; }
}