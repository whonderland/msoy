//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.AccountInfo;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.RegisterInfo;
import com.threerings.msoy.web.data.SessionData;
import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link WebUserService}.
 */
public interface WebUserServiceAsync
{
    /**
     * The asynchronous version of {@link WebUserService#login}.
     */
    void login (String clientVersion, String username, String password, int expireDays,
                       AsyncCallback<SessionData> callback);

    /**
     * The asynchronous version of {@link WebUserService#register}.
     */
    void register (String clientVersion, RegisterInfo info, AsyncCallback<SessionData> callback);

    /**
     * The asynchronous version of {@link WebUserService#validateSession}.
     */
    void validateSession (String clientVersion, String authtok, int expireDays,
                          AsyncCallback<SessionData> callback);

    /**
     * The asynchronous version of {@link WebUserService#getConnectConfig}.
     */
    void getConnectConfig (AsyncCallback<ConnectConfig> callback);

    /**
     * The asynchronous version of {@link WebUserService#loadLaunchConfig}.
     */
    void loadLaunchConfig (WebIdent ident, int gameId, AsyncCallback<LaunchConfig> callback);

    /**
     * The asynchronous version of {@link WebUserService#sendForgotPasswordEmail}.
     */
    void sendForgotPasswordEmail (String email, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebUserService#updateEmail}.
     */
    void updateEmail (WebIdent ident, String newEmail, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebUserService#updateEmailPrefs}.
     */
    void updateEmailPrefs (WebIdent ident, boolean emailOnWhirledMail,
                           boolean emailAnnouncements, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebUserService#updatePassword}.
     */
    void updatePassword (WebIdent ident, String newPassword, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebUserService#resetPassword}.
     */
    void resetPassword (int memberId, String code, String newPassword,
                        AsyncCallback<Boolean> callback);

    /**
     * The asynchronous version of {@link WebUserService#configurePermaName}.
     */
    void configurePermaName (WebIdent ident, String permaName, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link WebUserService#getAccountInfo}.
     */
    void getAccountInfo (WebIdent ident, AsyncCallback<AccountInfo> callback);

    /**
     * The asynchronous version of {@link WebUserService#updateAccountInfo}.
     */
    void updateAccountInfo (WebIdent ident, AccountInfo info, AsyncCallback<Void> callback);
}
