package com.entityrenderdistance;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class EntityRenderDistance
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(EntityRenderDistancePlugin.class);
		RuneLite.main(args);
	}
}