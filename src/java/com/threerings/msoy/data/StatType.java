//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.util.StringUtil;
import com.threerings.util.ActionScript;
import com.threerings.util.MessageBundle;

import com.threerings.stats.data.IntSetStat;
import com.threerings.stats.data.IntStat;
import com.threerings.stats.data.Stat;

/**
 * Enumerates the various stats used in Whirled.
 */
@ActionScript(omit=true)
public enum StatType implements Stat.Type
{
    // social stats
    FRIENDS_MADE(new IntStat(), true),
    INVITES_ACCEPTED(new IntStat(), true),
    WHIRLED_COMMENTS(new IntStat(), true),
    MINUTES_ACTIVE(new IntStat(), true),
    CONSEC_DAILY_LOGINS(new IntStat(), true),
    WHIRLEDS_VISITED(new IntSetStat(), true),

    // game stats
    TROPHIES_EARNED(new IntStat(), true),
    GAMES_PLAYED(new IntStat(), true),
    MP_GAMES_HOSTED(new IntStat(), true),
    MP_GAMES_WON(new IntStat(), true),
    MP_GAME_PARTNERS(new IntSetStat(), true),

    // creation stats
    ITEMS_LISTED(new IntStat(), true),
    ITEMS_SOLD(new IntStat(), true),
    ITEMS_PURCHASED(new IntStat(), true),

    UNUSED(new IntStat());

    /** Returns the translation key used by this stat. */
    public String key ()
    {
        return MessageBundle.qualify(
            MsoyCodes.STATS_MSGS, "m.stat_" + StringUtil.toUSLowerCase(name()));
    }

    // from interface Stat.Type
    public Stat newStat ()
    {
        return (Stat)_prototype.clone();
    }

    // from interface Stat.Type
    public int code ()
    {
        return _code;
    }

    // from interface Stat.Type
    public boolean isPersistent () {
        return _persist;
    }

    // most stats are persistent
    StatType (Stat prototype)
    {
        this(prototype, true);
    }

    StatType (Stat prototype, boolean persist)
    {
        _persist = persist;
        _prototype = prototype;

        // configure our prototype and map ourselves into the Stat system
        _code = Stat.initType(this, _prototype);
    }

    protected Stat _prototype;
    protected int _code;
    protected boolean _persist;
}
