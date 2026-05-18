package com.fogofwar.render.minimap;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
public final class MinimapWidgetProvider {
	private MinimapWidgetProvider() {}
	public static Widget getMinimapWidget(Client client) {
		if (client.isResized()) {
			if (client.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 1) return client.getWidget(InterfaceID.ToplevelPreEoc.MINIMAP);
			return client.getWidget(InterfaceID.ToplevelOsrsStretch.MINIMAP);
		}
		return client.getWidget(InterfaceID.Toplevel.MINIMAP);
	}
}
