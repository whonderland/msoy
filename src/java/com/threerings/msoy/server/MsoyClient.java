//
// $Id$

package com.threerings.msoy.server;

import java.util.logging.Level;

import com.samskivert.util.Invoker;

import com.threerings.presents.net.BootstrapData;
import com.threerings.presents.server.InvocationException;
import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatSet;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.CrowdObjectAccess;

import com.threerings.whirled.server.WhirledClient;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBootstrapData;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.web.data.MemberName;

import static com.threerings.msoy.Log.log;

/**
 * Represents an attached Msoy client on the server-side.
 */
public class MsoyClient extends WhirledClient
{
    @Override // from PresentsClient
    protected BootstrapData createBootstrapData ()
    {
        return new MsoyBootstrapData();
    }

    @Override // from PresentsClient
    protected void populateBootstrapData (BootstrapData data)
    {
        super.populateBootstrapData(data);

        //((MsoyBootstrapData) data).chatOid = MsoyServer.chatOid;
    }

    @Override // from PresentsClient
    protected void sessionWillStart ()
    {
        super.sessionWillStart();

        _memobj = (MemberObject) _clobj;

        MsoyAuthenticator.Account acct = (MsoyAuthenticator.Account) _authdata;
        if (acct != null) {
            _memobj.setTokens(acct.tokens);
        } else {
            _memobj.setTokens(new MsoyTokenRing());
        }

        MsoyServer.registerMember(_memobj);
    }

    @Override // from PresentsClient
    protected void sessionConnectionClosed ()
    {
        super.sessionConnectionClosed();

        // if we're a guest, end our session now, there's no way to reconnect
        if (_memobj != null && _memobj.isGuest()) {
            safeEndSession();
        }
    }

    @Override // from PresentsClient
    protected void sessionDidEnd ()
    {
        super.sessionDidEnd();

        if (_memobj == null) {
            return;
        }

        // clean up logged-on data for this member
        MsoyServer.clearMember(_memobj);

        // nothing more needs doing for guests
        if (_memobj.isGuest()) {
            _memobj = null;
            return;
        }

        final MemberName name = _memobj.memberName;
        final StatSet stats = _memobj.stats;
        _memobj = null;

        // update the member record in the database
        MsoyServer.invoker.postUnit(new Invoker.Unit("sessionDidEnd:" + name) {
            public boolean invoke () {
                try {
                    // write out any modified stats
                    Stat[] statArr = new Stat[stats.size()];
                    stats.toArray(statArr);
                    MsoyServer.statrepo.writeModified(name.getMemberId(), statArr);

                    // use a naive session length for now, ignoring web activity
                    MsoyServer.memberRepo.noteSessionEnded(
                        name.getMemberId(), Math.round(_connectTime / 60f));
                } catch (Exception e) {
                    log.log(Level.WARNING,
                            "Failed to note ended session [member=" + name + "].", e);
                }
                return false;
            }
        });
    }

    @Override // from CrowdClient
    protected void clearLocation (BodyObject bobj)
    {
        super.clearLocation(bobj);
        try {
            MsoyServer.worldGameReg.leaveWorldGame((MemberObject)bobj);
        } catch (InvocationException e) {
            // a warning will have already been logged
        }
    }
    
    /** A casted reference to the userobject. */
    protected MemberObject _memobj;
}
