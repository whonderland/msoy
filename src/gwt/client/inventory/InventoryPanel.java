//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.TabPanel;

import com.threerings.msoy.web.client.WebContext;

/**
 * Displays a tabbed panel containing a player's inventory.
 */
public class InventoryPanel extends TabPanel
{
    public InventoryPanel (WebContext ctx)
    {
        setStyleName("inventory");
        // create item panels for our known item types (alas we can't use
        // ItemEnum here)
        add(new ItemPanel(ctx, "PHOTO"), "Photos");
        add(new ItemPanel(ctx, "DOCUMENT"), "Documents");
        add(new ItemPanel(ctx, "FURNITURE"), "Furniture");
        add(new ItemPanel(ctx, "GAME"), "Games");
        selectTab(0);
    }
}
