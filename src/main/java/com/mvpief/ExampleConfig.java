package com.mvpief;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("dailyAgility")
public interface ExampleConfig extends Config
{
	@ConfigSection(name = "Laps", description = "Lap goal and display settings", position = 0)
	String lapsSection = "laps";

	@ConfigSection(name = "Marks of Grace", description = "Marks of Grace goal settings", position = 1)
	String marksSection = "marks";

	@ConfigSection(name = "Display", description = "Overlay display options", position = 2)
	String displaySection = "display";

	// --- Laps ---
	@ConfigItem(keyName = "dailyGoal", name = "Daily Lap Goal", description = "How many rooftop laps to complete today.", position = 0, section = lapsSection)
	default int DailyGoal() { return 100; }

	@ConfigItem(keyName = "lapsRemaining", name = "", description = "", hidden = true, section = lapsSection)
	default int LapsRemaining() { return -1; }

	// --- Marks ---
	@ConfigItem(keyName = "marksGoal", name = "Daily Marks Goal", description = "Marks of Grace to collect today.", position = 0, section = marksSection)
	default int MarksGoal() { return 10; }

	@ConfigItem(keyName = "marksRemaining", name = "", description = "", hidden = true, section = marksSection)
	default int MarksRemaining() { return 0; }

	// --- Display ---
	@ConfigItem(keyName = "showLapTime", name = "Show Last Lap Time", description = "Show the last lap time on the overlay.", position = 0, section = displaySection)
	default boolean showLapTime() { return true; }

	@ConfigItem(keyName = "showETA", name = "Show ETA", description = "Show estimated time remaining on the overlay.", position = 1, section = displaySection)
	default boolean showETA() { return true; }

	@ConfigItem(keyName = "showAverageTime", name = "Show Average Time", description = "Show average lap time on the overlay.", position = 2, section = displaySection)
	default boolean showAverageTime() { return true; }

	// --- Internal ---
	@ConfigItem(keyName = "lastSavedDate", name = "", description = "", hidden = true)
	default String LastSavedDate() { return ""; }
}