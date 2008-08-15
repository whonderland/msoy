//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.badge.data.all.Badge;

import com.threerings.msoy.web.data.ServiceException;

/**
 * Provides information for the Me page.
 */
public interface MeService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/mesvc";

    /**
     * Loads the data for the MyWhirled view for the calling user.
     */
    MyWhirledData getMyWhirled ()
        throws ServiceException;

    /**
     * Updates the Whirled news HTML. Caller must be an admin.
     */
    void updateWhirledNews (String newsHtml)
        throws ServiceException;

    /**
     * Loads all items in a player's inventory of the specified type and optionally restricted to
     * the specified suite.
     */
    List<FeedMessage> loadFeed (int cutoffDays)
        throws ServiceException;

    /**
     * Loads the badges relevant to this player.
     */
    PassportData loadBadges ()
        throws ServiceException;

    /**
     * Loads all available badges. For testing only.
     */
    List<Badge> loadAllBadges()
        throws ServiceException;
}
