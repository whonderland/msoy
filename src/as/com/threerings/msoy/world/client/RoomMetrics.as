package com.threerings.msoy.world.client {

import com.threerings.msoy.world.data.DecorData;

/**
 * Much of this will change soon when I add heights.
 */
public class RoomMetrics
{
    public var sceneWidth :int

    public var sceneHeight :int;

    public var sceneDepth :int;

    /**
     * The scale of media items at z=1.0.
     */
    public var minScale :Number;

    public const maxScale :Number = 1.0;

    public var scaleRange :Number;

    /**
     * Actual pixel height of the back wall.
     */
    public var backWallHeight :Number;

    /** Y pixel of the top of the back wall. */
    public var backWallTop :Number;

    /** Y pixel of the bottom of the back wall. */
    public var backWallBottom :Number;

    /** Y pixel of the horizon. */
    public var horizonY :Number;

    public var subHorizonHeight :Number;

    public function update (data :DecorData) :void
    {
        this.sceneDepth = data.depth;
        this.sceneWidth = data.width;
        this.sceneHeight = data.height;
        var horizon :Number = 1 - data.horizon;

        // I'm using 'this' to make clear which assignments are for public props
        this.minScale = (sceneDepth == 0) ? 0 : (FOCAL / (FOCAL + sceneDepth));
        this.scaleRange = maxScale - minScale;
        this.backWallHeight = sceneHeight * minScale;

        this.horizonY = sceneHeight * horizon;
        this.backWallTop = horizonY - (backWallHeight * horizon);
        this.backWallBottom = backWallTop + backWallHeight;

        this.subHorizonHeight = sceneHeight - horizonY;
    }

    /** The focal length of our perspective rendering. */
    // This value (488) was chosen so that the standard depth (400) causes layout nearly identical
    // to the original perspective math.  So, it's purely historical, but we could choose a new
    // focal length and even a new standard scene depth.
    // TODO
    public static const FOCAL :Number = 488;
}
}
