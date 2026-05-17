package com.fogofwar.util;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
public final class MinimapUtil {
	public static final int MINIMAP_GROUP_ID = 164;
	private static final int SIDE_PANELS_ID = 4607;
	private static final int FIXED_PARENT_ID = 548;
	private static final int FIXED_CHILD_ID = 22;
	private static final int STRETCH_PARENT_ID = 161;
	private static final int RESIZED_CHILD_ID = 30;
	private MinimapUtil() {}
	public static Widget getMinimapWidget(Client client) {
		if (client.isResized()) {
			if (client.getVarbitValue(SIDE_PANELS_ID) == 1) return client.getWidget(MINIMAP_GROUP_ID, RESIZED_CHILD_ID);
			return client.getWidget(STRETCH_PARENT_ID, RESIZED_CHILD_ID);
		}
		return client.getWidget(FIXED_PARENT_ID, FIXED_CHILD_ID);
	}
}
