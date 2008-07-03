package com.threerings.msoy.game.server;

import com.threerings.msoy.game.data.MsoyBureauLauncherCredentials;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.server.ClientFactory;
import com.threerings.presents.server.ClientResolver;
import com.threerings.presents.server.PresentsClient;
import com.threerings.util.Name;

import static com.threerings.msoy.Log.log;

/**
 * Creates very basic clients for bureau launcher connections, otherwise delegates.
 */
public class MsoyBureauLauncherClientFactory implements ClientFactory
{
    /** 
     * Creates a new factory. 
     * @param delegate factory to use when a non-bureau launcher connection is encountered
     */
    public MsoyBureauLauncherClientFactory (ClientFactory delegate)
    {
        _delegate = delegate;
    }

    // from interface ClientFactory
    public Class<? extends PresentsClient> getClientClass (AuthRequest areq)
    {
        // Just give bureau launchers a vanilla PresentsClient client.
        if (areq.getCredentials() instanceof MsoyBureauLauncherCredentials) {
            return PresentsClient.class;
        } else {
            return _delegate.getClientClass(areq);
        }
    }

    // from interface ClientFactory
    public Class<? extends ClientResolver> getClientResolverClass (Name username)
    {
        String prefix = 
            MsoyBureauLauncherCredentials.PEER_PREFIX +
            MsoyBureauLauncherCredentials.PREFIX;

        // Just give bureau launchers a vanilla ClientResolver.
        if (username.toString().startsWith(prefix)) {
            return ClientResolver.class;
        } else {
            return _delegate.getClientResolverClass(username);
        }
    }

    protected ClientFactory _delegate;
}
