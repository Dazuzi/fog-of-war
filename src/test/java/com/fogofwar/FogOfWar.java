package com.fogofwar;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FogOfWar
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FogOfWarPlugin.class);
		RuneLite.main(args);
	}
}