package com.mvpief;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PluginLauncher
{
	public static void main(String[] args) throws Exception
	{
        //noinspection unchecked
        ExternalPluginManager.loadBuiltin(DailyAgility.class);
		RuneLite.main(args);
	}
}