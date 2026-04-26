package com.mvpief;

import lombok.Getter;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.ChatMessageType;
import net.runelite.client.events.ConfigChanged;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.runelite.api.ItemContainer;
import net.runelite.api.Item;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;

@Slf4j
@PluginDescriptor(
		name = "Daily Agility"
)
public class DailyAgility extends Plugin
{
	private static final Pattern LAP_PATTERN = Pattern.compile("Your (.+) Rooftop lap count is: (\\d+)");
	private static final int MAX_LAP_HISTORY = 100;
	@Inject
	private ConfigManager configManager;

	@Inject
	private Client client;

	@Inject
	private ExampleConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private DailyGoalOverlay overlay;

	@Inject
	private ClientThread clientThread;


	private long lastLapTime = -1;
	@Getter
	private long lastLapDuration = -1;
	@Getter
	private String currentCourse = null;
	@Getter
	private int lastMarkCount = 0;
	private boolean inventoryInitialized = false;

	public int getMarksLeft()
	{
		return config.MarksRemaining();
	}
	public int getLapsLeft()
	{
		return config.LapsRemaining();
	}

	private void addMark(int count)
	{
		int newCount = Math.max(0, config.MarksRemaining() - count);
		configManager.setConfiguration("dailyAgility", "marksRemaining", newCount);
		log.debug("I added a Mark");
	}

	public double getAverageLapTime(String course)
	{
		List<Long> history = getLapHistory(course);
		if (history.isEmpty()) return -1;
		return history.stream().mapToLong(Long::longValue).average().orElse(-1);
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

	private List<Long> getLapHistory(String course)
	{
		String key = "lapHistory_" + course.replace(" ", "_");
		String stored = configManager.getConfiguration("dailyAgility", key);
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
		configManager.setConfiguration("dailyAgility", key, value);
	}

    private void init()
	{
		String today = java.time.LocalDate.now().toString();
		String lastDate = config.LastSavedDate();

		if (!today.equals(lastDate))
		{
			// New day — reset from goal setting
			configManager.setConfiguration("dailyAgility", "lapsRemaining", config.DailyGoal());
			configManager.setConfiguration("dailyAgility", "marksRemaining", config.MarksGoal());
			configManager.setConfiguration("dailyAgility", "lastSavedDate", today);
		}
		// If same day, lapsRemaining, marksRemaining already holds the correct value
	}

	private void subtractLap()
	{
		int newLapCount = config.LapsRemaining() - 1;
		configManager.setConfiguration("dailyAgility", "lapsRemaining", newLapCount);
	}

	private void timeLap(String course)
	{
		long now = System.currentTimeMillis();
		if (lastLapTime != -1)
		{
			lastLapDuration = now - lastLapTime;
			saveLapTime(course, lastLapDuration);
		}

		lastLapTime = now;
	}

	@Override
	protected void startUp() throws Exception
	{
		init();
		overlayManager.add(overlay);

		clientThread.invokeLater(() ->
		{
			ItemContainer inventory = client.getItemContainer(InventoryID.INV);
			if (inventory != null)
			{
				lastMarkCount = Arrays.stream(inventory.getItems())
						.filter(item -> item.getId() == 11849)
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
		overlayManager.remove(overlay);
	}

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
			timeLap(course);
		} else {log.debug("MVPief: This is not the correct message!");
		log.debug(chatMessage.getMessage());}

	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("dailyAgility") && event.getKey().equals("dailyGoal"))
		{
			configManager.setConfiguration("dailyAgility", "lapsRemaining", config.DailyGoal());
		}
		if (event.getGroup().equals("dailyAgility") && event.getKey().equals("marksGoal"))
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
				.filter(item -> item.getId() == 11849)
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
			addMark(picked);
		}

		lastMarkCount = currentCount;
	}

	@Provides
	ExampleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExampleConfig.class);
	}
}