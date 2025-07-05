package com.fogofwar.util;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

public final class MinimapUtil {
    private static final int SIDE_PANELS_ID = 4607;
    private static final int FIXED_PARENT_ID = 548;
    private static final int FIXED_CHILD_ID = 21;
    private static final int STRETCH_PARENT_ID = 161;
    private static final int STRETCH_CHILD_ID = 30;
    private static final int PRE_EOC_PARENT_ID = 164;
    private static final int PRE_EOC_CHILD_ID = 30;
    private MinimapUtil() {}
    public static Widget getMinimapWidget(Client client) {
        if (client.isResized()) {
            if (client.getVarbitValue(SIDE_PANELS_ID) == 1) {
                return client.getWidget(PRE_EOC_PARENT_ID, PRE_EOC_CHILD_ID);
            }
            return client.getWidget(STRETCH_PARENT_ID, STRETCH_CHILD_ID);
        }
        return client.getWidget(FIXED_PARENT_ID, FIXED_CHILD_ID);
    }
}