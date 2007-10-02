//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.TypedArray;
import com.threerings.presents.dobj.DSet;

import com.threerings.ezgame.data.EZGameObject;

import com.whirled.data.WhirledGame;
import com.whirled.data.WhirledGameMarshaller;

/**
 * Maintains additional state for MSOY games.
 */
public class MsoyGameObject extends EZGameObject
    implements WhirledGame
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>whirledGameService</code> field. */
    public static const WHIRLED_GAME_SERVICE :String = "whirledGameService";

    /** The field name of the <code>levelPacks</code> field. */
    public static const LEVEL_PACKS :String = "levelPacks";

    /** The field name of the <code>itemPacks</code> field. */
    public static const ITEM_PACKS :String = "itemPacks";
    // AUTO-GENERATED: FIELDS END

    /** The whirled game services. */
    public var whirledGameService :WhirledGameMarshaller;

    /** The set of game data available to this game. */
    public var gameData :TypedArray /*GameData*/;

    /** Information on which players own which game data. */
    public var ownershipData :DSet /*Ownership*/;

    // from interface WhirledGame
    public function getWhirledGameService () :WhirledGameMarshaller
    {
        return whirledGameService;
    }

    // from interface WhirledGame
    public function getGameData () :Array
    {
        return gameData;
    }

    // from interface WhirledGame
    public function getGameDataOwnership () :DSet
    {
        return ownershipData;
    }

    override protected function readDefaultFields (ins :ObjectInputStream) :void
    {
        super.readDefaultFields(ins);

        whirledGameService = (ins.readObject() as WhirledGameMarshaller);
        gameData = (ins.readObject() as TypedArray);
        ownershipData = (ins.readObject() as DSet);
    }
}
}
