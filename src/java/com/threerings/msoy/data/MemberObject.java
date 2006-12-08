//
// $Id$

package com.threerings.msoy.data;

import java.util.Iterator;

import com.samskivert.util.ListUtil;
import com.samskivert.util.Predicate;

import com.threerings.presents.dobj.DSet;
import com.threerings.util.Name;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.data.TokenRing;

import com.threerings.parlor.game.data.GameObject;

import com.threerings.whirled.spot.data.ClusteredBodyObject;

import com.threerings.msoy.web.data.MemberName;

import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.StaticMediaDesc;

import com.threerings.msoy.game.data.GameMemberInfo;

import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.WorldMemberInfo;

import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.GroupName;

/**
 * Represents a connected msoy user.
 */
public class MemberObject extends BodyObject
    implements ClusteredBodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memberName</code> field. */
    public static final String MEMBER_NAME = "memberName";

    /** The field name of the <code>sceneId</code> field. */
    public static final String SCENE_ID = "sceneId";

    /** The field name of the <code>clusterOid</code> field. */
    public static final String CLUSTER_OID = "clusterOid";

    /** The field name of the <code>recentScenes</code> field. */
    public static final String RECENT_SCENES = "recentScenes";

    /** The field name of the <code>ownedScenes</code> field. */
    public static final String OWNED_SCENES = "ownedScenes";

    /** The field name of the <code>inventory</code> field. */
    public static final String INVENTORY = "inventory";

    /** The field name of the <code>loadedInventory</code> field. */
    public static final String LOADED_INVENTORY = "loadedInventory";

    /** The field name of the <code>tokens</code> field. */
    public static final String TOKENS = "tokens";

    /** The field name of the <code>homeSceneId</code> field. */
    public static final String HOME_SCENE_ID = "homeSceneId";

    /** The field name of the <code>avatar</code> field. */
    public static final String AVATAR = "avatar";

    /** The field name of the <code>chatStyle</code> field. */
    public static final String CHAT_STYLE = "chatStyle";

    /** The field name of the <code>chatPopStyle</code> field. */
    public static final String CHAT_POP_STYLE = "chatPopStyle";

    /** The field name of the <code>friends</code> field. */
    public static final String FRIENDS = "friends";

    /** The field name of the <code>groups</code> field. */
    public static final String GROUPS = "groups";
    // AUTO-GENERATED: FIELDS END

    /** The name and id information for this user. */
    public MemberName memberName;

    /** The scene id that the user is currently occupying. */
    public int sceneId;

    /** The object ID of the user's cluster. */
    public int clusterOid;

    /** The recent scenes we've been through. */
    public DSet<SceneBookmarkEntry> recentScenes =
        new DSet<SceneBookmarkEntry>();

    /** The scenes we own. */
    public DSet<SceneBookmarkEntry> ownedScenes =
        new DSet<SceneBookmarkEntry>();

    /** The user's inventory, lazy-initialized. */
    public DSet<Item> inventory = new DSet<Item>();

    /** A bitmask of the item types that have been loaded into inventory.
     * Use TODO and TODO to access.
     */
    public int loadedInventory;

    /** The tokens defining the access controls for this user. */
    public MsoyTokenRing tokens;

    /** The id of the user's home scene. */
    public int homeSceneId;

    /** The avatar that the user has chosen, or null for guests. */
    public Avatar avatar;

    /** The style of our chat. */
    public short chatStyle;

    /** The pop style of our chat. */
    public short chatPopStyle;

    /** The friends of this player. */
    public DSet<FriendEntry> friends = new DSet<FriendEntry>();

    /** The groups of this player. */
    public DSet<GroupMembership> groups;

    /**
     * Returns this member's unique id.
     */
    public int getMemberId ()
    {
        return (memberName == null) ? MemberName.GUEST_ID
                                    : memberName.getMemberId();
    }

    /**
     * Return true if this user is merely a guest.
     */
    public boolean isGuest ()
    {
        return (getMemberId() == MemberName.GUEST_ID);
    }

    /**
     * Get the media to use as our headshot.
     */
    public MediaDesc getHeadShotMedia ()
    {
        if (avatar != null) {
            return avatar.getHeadShotMedia();
        }
        return new StaticMediaDesc(StaticMediaDesc.HEADSHOT, Item.AVATAR);
    }

    // documentation inherited from superinterface ScenedBodyObject
    public int getSceneId ()
    {
        return sceneId;
    }

    // documentation inherited from interface ClusteredBodyObject
    public int getClusterOid ()
    {
        return clusterOid;
    }

    // documentation inherited from interface ClusteredBodyObject
    public String getClusterField ()
    {
        return CLUSTER_OID;
    }

    // documentation inherited
    public OccupantInfo createOccupantInfo (PlaceObject plobj)
    {
        if (plobj instanceof RoomObject) {
            return new WorldMemberInfo(this);

//        } else if (plobj instanceof GameObject) {
//            return new GameMemberInfo(this);
//
        } else {
            return new MemberInfo(this);
        }
    }

    @Override // from BodyObject
    public TokenRing getTokens ()
    {
        return tokens;
    }

    @Override // from BodyObject
    public Name getVisibleName ()
    {
        return memberName;
    }

    /**
     * Is this user a member of the specified group?
     */
    public boolean isGroupMember (int groupId)
    {
        return isGroupRank(groupId, GroupMembership.RANK_MEMBER);
    }

    /**
     * Is this user a manager in the specified group?
     */
    public boolean isGroupManager (int groupId)
    {
        return isGroupRank(groupId, GroupMembership.RANK_MANAGER);
    }

    /**
     * @return true if the user has at least the specified rank in the
     * specified group.
     */
    public boolean isGroupRank (int groupId, byte requiredRank)
    {
        return getGroupRank(groupId) >= requiredRank;
    }

    /**
     * Get the user's rank in the specified group.
     */
    public byte getGroupRank (int groupId)
    {
        if (groups != null) {
            GroupName group = new GroupName();
            group.groupId = groupId;
            GroupMembership membInfo = groups.get(group);
            if (membInfo != null) {
                return membInfo.rank;
            }
        }
        return GroupMembership.RANK_NON_MEMBER;
    }

    /**
     * Return true if the specified item type has been loaded.
     */
    public boolean isInventoryLoaded (byte itemType)
    {
        return (0 != ((1 << itemType) & loadedInventory));
    }

    /**
     * Get an iterator of the items of the specified type.
     *
     * @throws IllegalStateException if the specified type is not yet
     * loaded.
     */
    public Iterator<Item> getItems (final byte itemType)
    {
        if (!isInventoryLoaded(itemType)) {
            throw new IllegalStateException(
                "Items not yet loaded: " + itemType);
        }

        // set up a predicate for that type of item
        Predicate<Item> pred = new Predicate<Item>() {
            public boolean isMatch (Item item) {
                return (item.getType() == itemType);
            }
        };

        // use the predicate to filter
        return pred.filter(inventory.iterator());
    }

    // TEMP: hackery
    @Override
    public void setOid (int oid)
    {
        super.setOid(oid);

        chatStyle = (short) (oid % 2);
        chatPopStyle = (short) (oid % 2);
    }
    // END

    public void alter (String field)
    {
        if (CHAT_STYLE.equals(field)) {
            setChatStyle((short) ((chatStyle + 1) % 2));

        } else if (CHAT_POP_STYLE.equals(field)) {
            setChatPopStyle((short) ((chatPopStyle + 1) % 2));
        }
    }

    /**
     * Add the specified scene to the recent scene list for this user.
     */
    public void addToRecentScenes (int sceneId, String name)
    {
        SceneBookmarkEntry newEntry = new SceneBookmarkEntry(
            sceneId, name, System.currentTimeMillis());

        SceneBookmarkEntry oldest = null;
        for (SceneBookmarkEntry sbe : recentScenes) {
            if (sbe.sceneId == sceneId) {
                newEntry.orderingId = sbe.orderingId;
                updateRecentScenes(newEntry);
                return;
            }
            if (oldest == null || oldest.lastVisit > sbe.lastVisit) {
                oldest = sbe;
            }
        }

        int size = recentScenes.size();
        if (size < MAX_RECENT_SCENES) {
            newEntry.orderingId = (short) size;
            addToRecentScenes(newEntry);

        } else {
            newEntry.orderingId = oldest.orderingId;
            updateRecentScenes(newEntry);
        }
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>memberName</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setMemberName (MemberName value)
    {
        MemberName ovalue = this.memberName;
        requestAttributeChange(
            MEMBER_NAME, value, ovalue);
        this.memberName = value;
    }

    /**
     * Requests that the <code>sceneId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setSceneId (int value)
    {
        int ovalue = this.sceneId;
        requestAttributeChange(
            SCENE_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.sceneId = value;
    }

    /**
     * Requests that the <code>clusterOid</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setClusterOid (int value)
    {
        int ovalue = this.clusterOid;
        requestAttributeChange(
            CLUSTER_OID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.clusterOid = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>recentScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToRecentScenes (SceneBookmarkEntry elem)
    {
        requestEntryAdd(RECENT_SCENES, recentScenes, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>recentScenes</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromRecentScenes (Comparable key)
    {
        requestEntryRemove(RECENT_SCENES, recentScenes, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>recentScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateRecentScenes (SceneBookmarkEntry elem)
    {
        requestEntryUpdate(RECENT_SCENES, recentScenes, elem);
    }

    /**
     * Requests that the <code>recentScenes</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setRecentScenes (DSet<com.threerings.msoy.data.SceneBookmarkEntry> value)
    {
        requestAttributeChange(RECENT_SCENES, value, this.recentScenes);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.data.SceneBookmarkEntry> clone =
            (value == null) ? null : value.typedClone();
        this.recentScenes = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>ownedScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToOwnedScenes (SceneBookmarkEntry elem)
    {
        requestEntryAdd(OWNED_SCENES, ownedScenes, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>ownedScenes</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromOwnedScenes (Comparable key)
    {
        requestEntryRemove(OWNED_SCENES, ownedScenes, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>ownedScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateOwnedScenes (SceneBookmarkEntry elem)
    {
        requestEntryUpdate(OWNED_SCENES, ownedScenes, elem);
    }

    /**
     * Requests that the <code>ownedScenes</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setOwnedScenes (DSet<com.threerings.msoy.data.SceneBookmarkEntry> value)
    {
        requestAttributeChange(OWNED_SCENES, value, this.ownedScenes);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.data.SceneBookmarkEntry> clone =
            (value == null) ? null : value.typedClone();
        this.ownedScenes = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>inventory</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToInventory (Item elem)
    {
        requestEntryAdd(INVENTORY, inventory, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>inventory</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromInventory (Comparable key)
    {
        requestEntryRemove(INVENTORY, inventory, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>inventory</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateInventory (Item elem)
    {
        requestEntryUpdate(INVENTORY, inventory, elem);
    }

    /**
     * Requests that the <code>inventory</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setInventory (DSet<com.threerings.msoy.item.web.Item> value)
    {
        requestAttributeChange(INVENTORY, value, this.inventory);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.item.web.Item> clone =
            (value == null) ? null : value.typedClone();
        this.inventory = clone;
    }

    /**
     * Requests that the <code>loadedInventory</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setLoadedInventory (int value)
    {
        int ovalue = this.loadedInventory;
        requestAttributeChange(
            LOADED_INVENTORY, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.loadedInventory = value;
    }

    /**
     * Requests that the <code>tokens</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTokens (MsoyTokenRing value)
    {
        MsoyTokenRing ovalue = this.tokens;
        requestAttributeChange(
            TOKENS, value, ovalue);
        this.tokens = value;
    }

    /**
     * Requests that the <code>homeSceneId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setHomeSceneId (int value)
    {
        int ovalue = this.homeSceneId;
        requestAttributeChange(
            HOME_SCENE_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.homeSceneId = value;
    }

    /**
     * Requests that the <code>avatar</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setAvatar (Avatar value)
    {
        Avatar ovalue = this.avatar;
        requestAttributeChange(
            AVATAR, value, ovalue);
        this.avatar = value;
    }

    /**
     * Requests that the <code>chatStyle</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setChatStyle (short value)
    {
        short ovalue = this.chatStyle;
        requestAttributeChange(
            CHAT_STYLE, Short.valueOf(value), Short.valueOf(ovalue));
        this.chatStyle = value;
    }

    /**
     * Requests that the <code>chatPopStyle</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setChatPopStyle (short value)
    {
        short ovalue = this.chatPopStyle;
        requestAttributeChange(
            CHAT_POP_STYLE, Short.valueOf(value), Short.valueOf(ovalue));
        this.chatPopStyle = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>friends</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToFriends (FriendEntry elem)
    {
        requestEntryAdd(FRIENDS, friends, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>friends</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromFriends (Comparable key)
    {
        requestEntryRemove(FRIENDS, friends, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>friends</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateFriends (FriendEntry elem)
    {
        requestEntryUpdate(FRIENDS, friends, elem);
    }

    /**
     * Requests that the <code>friends</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setFriends (DSet<com.threerings.msoy.data.FriendEntry> value)
    {
        requestAttributeChange(FRIENDS, value, this.friends);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.data.FriendEntry> clone =
            (value == null) ? null : value.typedClone();
        this.friends = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>groups</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToGroups (GroupMembership elem)
    {
        requestEntryAdd(GROUPS, groups, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>groups</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromGroups (Comparable key)
    {
        requestEntryRemove(GROUPS, groups, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>groups</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateGroups (GroupMembership elem)
    {
        requestEntryUpdate(GROUPS, groups, elem);
    }

    /**
     * Requests that the <code>groups</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setGroups (DSet<com.threerings.msoy.web.data.GroupMembership> value)
    {
        requestAttributeChange(GROUPS, value, this.groups);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.web.data.GroupMembership> clone =
            (value == null) ? null : value.typedClone();
        this.groups = clone;
    }
    // AUTO-GENERATED: METHODS END

    public static final int MAX_RECENT_SCENES = 10;
}
