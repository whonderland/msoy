package com.threerings.msoy.world.data {

import com.threerings.util.Hashable;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.data.MediaData;

public class FurniData
    implements Hashable, Streamable
{
    /** The id of this piece of furni. */
    public var id :int;

    /** Info about the media that represents this piece of furni. */
    public var media :MediaData;

    /** The location in the scene. */
    public var loc :MsoyLocation;

    // documentation inherited from superinterface Equalable
    public function equals (other :Object) :Boolean
    {
        return (other is FurniData) &&
            (other as FurniData).id == this.id;
    }

    // documentation inherited from interface Hashable
    public function hashCode () :int
    {
        return id;
    }

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(id);
        out.writeObject(media);
        out.writeObject(loc);
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        id = ins.readInt();
        media = (ins.readObject() as MediaData);
        loc = (ins.readObject() as MsoyLocation);
    }
}
}
