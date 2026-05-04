package com.mvpief;

import com.mvpief.state.SessionState;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDate;

@Slf4j
@PluginDescriptor(name = "Daily Agility")
public class DailyAgility extends Plugin
{
	// region Constants

	private static final Pattern LAP_PATTERN = Pattern.compile("Your (.+) Rooftop lap count is: (\\d+)");
	private static final int MARK_OF_GRACE_ID = 11849;

	// endregion

	// region Injected

	@Inject private ConfigManager configManager;
	@Inject private Client client;
	@Inject private DailyAgilityConfig config;
	@Inject private OverlayManager overlayManager;
	@Inject private DailyGoalOverlay overlay;
	@Inject private ClientThread clientThread;
	@Inject private LapTimer lapTimer;
	@Inject private SessionState sessionState;
	@Inject private ClientToolbar clientToolbar;
	@Inject private DailyAgilityPanel panel;
	@Inject private LogStore logStore;

	// endregion

	// region State

	@Getter private String currentCourse = null;
	@Getter private int lastMarkCount = 0;
	private boolean inventoryInitialized = false;
	private NavigationButton navButton;

	// endregion

	// region Lifecycle

	@Override
	protected void startUp() throws Exception
	{
		resetDailyProgressIfNewDay();
		initInventory();
		initPanel();
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		inventoryInitialized = false;
		lapTimer.reset();
		overlayManager.remove(overlay);
	}

	// endregion

	// region Getters

	public long getLastLapDuration()
	{
		return lapTimer.getLastLapDuration();
	}

	public double getAverageLapTime(String course)
	{
		return lapTimer.getAverageLapTime(course);
	}

	public int getLapsLeft()
	{
		return config.lapsRemaining();
	}

	public int getMarksLeft()
	{
		return config.marksRemaining();
	}


	public long getEstimatedTimeRemainingMs()
	{
		if (currentCourse == null) return -1;
		double avg = getAverageLapTime(currentCourse);
		if (avg < 0) return -1;
		int laps = config.lapsRemaining();
		if (laps <= 0) return 0;
		return (long) (avg * laps);
	}

	// endregion

	// region Internals

	private void resetDailyProgressIfNewDay()
	{
		String today = LocalDate.now().toString();
		String lastDate = config.lastSavedDate();

		if (!today.equals(lastDate))
		{
			configManager.setConfiguration(DailyAgilityConfig.GROUP, "lapsRemaining", config.dailyGoal());
			configManager.setConfiguration(DailyAgilityConfig.GROUP, "marksRemaining", config.marksGoal());
			configManager.setConfiguration(DailyAgilityConfig.GROUP, "lastSavedDate", today);
		}
	}

	private void initInventory()
	{
		clientThread.invokeLater(() ->
		{
			ItemContainer inventory = client.getItemContainer(InventoryID.INV);
			if (inventory == null)
			{
				return;
			}

			lastMarkCount = Arrays.stream(inventory.getItems())
					.filter(item -> item.getId() == MARK_OF_GRACE_ID)
					.mapToInt(Item::getQuantity)
					.sum();
			inventoryInitialized = true;
		});
	}

	private void initPanel()
	{
		final BufferedImage rawIcon = ImageUtil.loadImageResource(getClass(), "markofgrace.png");
		final BufferedImage icon = ImageUtil.resizeImage(rawIcon, 16, 16);

		navButton = NavigationButton.builder()
				.tooltip("Daily Agility")
				.icon(icon)
				.priority(6)           // lower = further left in the sidebar
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
		panel.setOnDateSelected(date -> panel.setLogEntries(logStore.getEntries(null, date)));

		refreshPanel();
		refreshCalendarHighlights();
	}

	private void refreshPanel()
	{
		panel.setLogEntries(logStore.getEntries(null));
	}

	private void refreshCalendarHighlights()
	{
		Set<LocalDate> dates = logStore.getKnownDates().stream()
				.map(LocalDate::parse)
				.collect(java.util.stream.Collectors.toSet());
		panel.setDatesWithData(dates);
	}

	private void subtractLap()
	{
		int newLapCount = config.lapsRemaining() - 1;
		configManager.setConfiguration(DailyAgilityConfig.GROUP, "lapsRemaining", newLapCount);
		if (currentCourse != null) logStore.recordLap(currentCourse);
		refreshPanel();
		refreshCalendarHighlights();
	}

	private void markPickUp(int count)
	{
		int newCount = config.marksRemaining() - count;
		configManager.setConfiguration(DailyAgilityConfig.GROUP, "marksRemaining", newCount);
		if (currentCourse != null) logStore.recordMarks(currentCourse, count);
		refreshPanel();
	}

	// endregion

	// region Events

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		if (chatMessage.getType() != ChatMessageType.GAMEMESSAGE) return;

		String clean = chatMessage.getMessage().replaceAll("<[^>]+>", "");
		Matcher matcher = LAP_PATTERN.matcher(clean);
		if (matcher.find())
		{
			String course = matcher.group(1);
			currentCourse = course;
			subtractLap();
			lapTimer.recordLap(course);
			sessionState.setCurrentCourse(matcher.group(1));
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(DailyAgilityConfig.GROUP)) return;

		// TODO: make a button to set the current lapsRemaining to the current goal & make it increase by delta.
		// For now the lap counter just resets to the new dailGoal when changed.
		if (event.getKey().equals("dailyGoal"))
		{
			configManager.setConfiguration(DailyAgilityConfig.GROUP, "lapsRemaining", config.dailyGoal());
		}
		if (event.getKey().equals("marksGoal"))
		{
			configManager.setConfiguration(DailyAgilityConfig.GROUP, "marksRemaining", config.marksGoal());
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INV) return;

		ItemContainer inventory = event.getItemContainer();
		int currentCount = Arrays.stream(inventory.getItems())
				.filter(item -> item.getId() == MARK_OF_GRACE_ID)
				.mapToInt(Item::getQuantity)
				.sum();

		if (!inventoryInitialized)
		{
			lastMarkCount = currentCount;
			inventoryInitialized = true;
			return;
		}

		if (currentCount > lastMarkCount)
		{
			int picked = currentCount - lastMarkCount;
			markPickUp(picked);
		}

		lastMarkCount = currentCount;
	}

	// endregion

	// region Config

	@Provides
	DailyAgilityConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DailyAgilityConfig.class);
	}

	// endregion
}