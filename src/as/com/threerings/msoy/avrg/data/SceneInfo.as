//
// $Id$

package com.threerings.msoy.avrg.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.util.StringUtil;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Helps an AVRG keep track of which world server is currently hosting a given scene.
 */
public class SceneInfo
    implements DSet_Entry
{
    /** The id of the scene occupied by one or more players in our AVRG. */
    public var sceneId :int;

    /** The hostname of the world server hosting this scene. */
    public var hostname :String;

    /** The port on which to connect to said world server. */
    public var port :int;

    // from interface DSet_Entry
    public function getKey () :Object
    {
        return sceneId;
    }

    // from Object
    public function toString () :String
    {
        return StringUtil.simpleToString(this);
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        sceneId = ins.readInt();
        hostname = (ins.readField(String) as String);
        port = ins.readInt();
    }

    // from interface Streamable
    public function writeObject (os :ObjectOutputStream) :void
    {
        os.writeInt(sceneId);
        os.writeField(hostname);
        os.writeInt(port);
    }
}

}
