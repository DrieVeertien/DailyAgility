package com.mvpief;

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
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	@Inject private ExampleConfig config;
	@Inject private OverlayManager overlayManager;
	@Inject private DailyGoalOverlay overlay;
	@Inject private ClientThread clientThread;

	// endregion

	// region State

	@Getter private String currentCourse = null;
	@Getter private int lastMarkCount = 0;
	private boolean inventoryInitialized = false;
	private LapTimer lapTimer;

	// endregion

	// region Lifecycle

	@Override
	protected void startUp() throws Exception
	{
		lapTimer = new LapTimer(configManager);
		init();
		overlayManager.add(overlay);

		clientThread.invokeLater(() ->
		{
			ItemContainer inventory = client.getItemContainer(InventoryID.INV);
			if (inventory != null)
			{
				lastMarkCount = Arrays.stream(inventory.getItems())
						.filter(item -> item.getId() == MARK_OF_GRACE_ID)
						.mapToInt(Item::getQuantity)
						.sum();
				inventoryInitialized = true;
			}
		});
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
		return config.LapsRemaining();
	}

	public int getMarksLeft()
	{
		return config.MarksRemaining();
	}

	public long getEstimatedTimeRemaining()
	{
		if (currentCourse == null) return -1;
		double avg = getAverageLapTime(currentCourse);
		if (avg < 0) return -1;
		int laps = config.LapsRemaining();
		if (laps <= 0) return 0;
		return (long) (avg * laps);
	}

	// endregion

	// region Internals

	private void init()
	{
		String today = java.time.LocalDate.now().toString();
		String lastDate = config.LastSavedDate();

		if (!today.equals(lastDate))
		{
			configManager.setConfiguration("dailyAgility", "lapsRemaining", config.DailyGoal());
			configManager.setConfiguration("dailyAgility", "marksRemaining", config.MarksGoal());
			configManager.setConfiguration("dailyAgility", "lastSavedDate", today);
		}
	}

	private void subtractLap()
	{
		int newLapCount = config.LapsRemaining() - 1;
		configManager.setConfiguration("dailyAgility", "lapsRemaining", newLapCount);
	}

	private void markPickUp(int count)
	{
		int newCount = Math.max(0, config.MarksRemaining() - count);
		configManager.setConfiguration("dailyAgility", "marksRemaining", newCount);
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
		}
		else
		{
			log.debug("MVPief: This is not the correct message!");
			log.debug(chatMessage.getMessage());
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("dailyAgility")) return;

		if (event.getKey().equals("dailyGoal"))
		{
			configManager.setConfiguration("dailyAgility", "lapsRemaining", config.DailyGoal());
		}
		if (event.getKey().equals("marksGoal"))
		{
			configManager.setConfiguration("dailyAgility", "marksRemaining", config.MarksGoal());
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
	ExampleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExampleConfig.class);
	}

	// endregion
}