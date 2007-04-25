//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.whirled.spot.data.SpotSceneObject;

/**
 * Room stuff.
 */
public class RoomObject extends SpotSceneObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>roomService</code> field. */
    public static final String ROOM_SERVICE = "roomService";

    /** The field name of the <code>memories</code> field. */
    public static final String MEMORIES = "memories";

    /** The field name of the <code>controllers</code> field. */
    public static final String CONTROLLERS = "controllers";

    /** The field name of the <code>effects</code> field. */
    public static final String EFFECTS = "effects";
    // AUTO-GENERATED: FIELDS END

    /** A message sent by the server to have occupants load, but not play,
     * the specified music.
     * Format: [ url ].  */
    public static final String LOAD_MUSIC = "loadMusic";

    /** A corresponding message sent by each client when they've got the music
     * completely loaded. No other status is needed.
     * Format: [ url ]. */
    public static final String MUSIC_LOADED = "musicLoaded";

    /** The message sent by the server to kick-off music playing. The music
     * should be played once and then disposed-of. No action
     * should be taken if the music was not loaded. 
     * Format: [ url ]. */
    public static final String PLAY_MUSIC = "playMusic";

    /** Our room service marshaller. */
    public RoomMarshaller roomService;

    /** Contains the memories for all entities in this room. */
    public DSet<MemoryEntry> memories = new DSet<MemoryEntry>();

    /** Contains mappings for all controlled entities in this room. */
    public DSet<EntityControl> controllers = new DSet<EntityControl>();

    /** Contains the currently displayed "effects" (temporary furniture..). */
    public DSet<FurniData> effects = new DSet<FurniData>();

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>roomService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setRoomService (RoomMarshaller value)
    {
        RoomMarshaller ovalue = this.roomService;
        requestAttributeChange(
            ROOM_SERVICE, value, ovalue);
        this.roomService = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>memories</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToMemories (MemoryEntry elem)
    {
        requestEntryAdd(MEMORIES, memories, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>memories</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromMemories (Comparable key)
    {
        requestEntryRemove(MEMORIES, memories, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>memories</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateMemories (MemoryEntry elem)
    {
        requestEntryUpdate(MEMORIES, memories, elem);
    }

    /**
     * Requests that the <code>memories</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setMemories (DSet<com.threerings.msoy.world.data.MemoryEntry> value)
    {
        requestAttributeChange(MEMORIES, value, this.memories);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.world.data.MemoryEntry> clone =
            (value == null) ? null : value.typedClone();
        this.memories = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>controllers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToControllers (EntityControl elem)
    {
        requestEntryAdd(CONTROLLERS, controllers, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>controllers</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromControllers (Comparable key)
    {
        requestEntryRemove(CONTROLLERS, controllers, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>controllers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateControllers (EntityControl elem)
    {
        requestEntryUpdate(CONTROLLERS, controllers, elem);
    }

    /**
     * Requests that the <code>controllers</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setControllers (DSet<com.threerings.msoy.world.data.EntityControl> value)
    {
        requestAttributeChange(CONTROLLERS, value, this.controllers);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.world.data.EntityControl> clone =
            (value == null) ? null : value.typedClone();
        this.controllers = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>effects</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToEffects (FurniData elem)
    {
        requestEntryAdd(EFFECTS, effects, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>effects</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromEffects (Comparable key)
    {
        requestEntryRemove(EFFECTS, effects, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>effects</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateEffects (FurniData elem)
    {
        requestEntryUpdate(EFFECTS, effects, elem);
    }

    /**
     * Requests that the <code>effects</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setEffects (DSet<com.threerings.msoy.world.data.FurniData> value)
    {
        requestAttributeChange(EFFECTS, value, this.effects);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.world.data.FurniData> clone =
            (value == null) ? null : value.typedClone();
        this.effects = clone;
    }
    // AUTO-GENERATED: METHODS END
}
