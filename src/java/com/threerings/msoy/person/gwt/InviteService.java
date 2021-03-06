//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.web.gwt.EmailContact;
import com.threerings.msoy.web.gwt.MemberCard;

/**
 * Handles invitation-related functionality.
 */
@RemoteServiceRelativePath(InviteService.REL_PATH)
public interface InviteService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/invitesvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + InviteService.ENTRY_POINT;

    /**
     * Loads up e-mail addresses from a user's webmail account.
     */
    List<EmailContact> getWebMailAddresses (String email, String password)
        throws ServiceException;

    /**
     * Send out some of this person's available invites.
     *
     * @param anonymous if true, the invitations will not be from the caller but will be
     * anonymous. This is only allowed for admin callers.
     */
    InvitationResults sendInvites (
        List<EmailContact> addresses, String fromName, String subject, String customMessage,
        boolean anonymous)
        throws ServiceException;

    /**
     * Send out invitations to a game via email.
     */
    InvitationResults sendGameInvites (
        List<EmailContact> addresses, int gameId, String from, String subject, String body)
        throws ServiceException;

    /**
     * Send out invitations to a game via whirled mail.
     */
    void sendWhirledMailGameInvites (
        Set<Integer> recipientIds, int gameId, String subject, String body, String args)
        throws ServiceException;

    /**
     * Gets the ID of the current user's home room.  Necessary for the new share page (this may
     * in fact become more specialized to retrieve any data necessary for that page).
     *
     * @return ID of the current user's home room.
     */
    int getHomeSceneId ()
        throws ServiceException;

    /**
     * Gets the user's friends, up to the given maximum, sorted by most recently online. If gameId
     * is non-zero, the returned members are futher limited to those that have not yet played the
     * game with that id.
     */
    List<MemberCard> getFriends (int gameId, int count)
        throws ServiceException;
}
