//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.Graphics;
import flash.external.ExternalInterface;
import flash.utils.ByteArray;

import mx.containers.Canvas;
import mx.controls.HSlider;
import mx.controls.VSlider;
import mx.controls.Label;
import mx.core.UIComponent;

import mx.events.SliderEvent;

import com.threerings.msoy.world.data.DecorData;


public class DecorViewerComp extends Canvas
{
    public static const log :Log = Log.getLog(DecorViewerComp);

    public function DecorViewerComp ()
    {
        if (ExternalInterface.available) {
            try {
                // hook up our ffi
                ExternalInterface.addCallback("updateParameters", updateParameters);
                ExternalInterface.addCallback("updateMedia", updateMedia);
            } catch (err :Error) {
                log.warning("External interface initialization failed: " + err);
            }
        } else {
            log.warning("External interface not available!");
        }
    }

    // inherited from Canvas
    override protected function createChildren () :void
    {
        super.createChildren();

        _results = new Label();
        addChild(_results);

        _horizonSlider = new VSlider();
        _horizonSlider.minimum = 0;
        _horizonSlider.maximum = 1;
        _horizonSlider.liveDragging = true;
        _horizonSlider.addEventListener(SliderEvent.CHANGE, processChange);
        _horizonSlider.x = 20;
        _horizonSlider.y = 50;
        _horizonSlider.height = PREVIEW_BOX_SIZE;
        addChild(_horizonSlider);
        
        _depthSlider = new VSlider();
        _depthSlider.minimum = 1;
        _depthSlider.maximum = 2000;
        _depthSlider.liveDragging = true;
        _depthSlider.addEventListener(SliderEvent.CHANGE, processChange);
        _depthSlider.x = PREVIEW_BOX_SIZE + 60;
        _depthSlider.y = 50;
        _depthSlider.height = PREVIEW_BOX_SIZE;
        addChild(_depthSlider);

        _widthSlider = new HSlider();
        _widthSlider.minimum = 1;
        _widthSlider.maximum = 2000;
        _widthSlider.liveDragging = true;
        _widthSlider.addEventListener(SliderEvent.CHANGE, processChange);
        _widthSlider.x = 50;
        _widthSlider.y = PREVIEW_BOX_SIZE + 60;
        _widthSlider.width = PREVIEW_BOX_SIZE;
        addChild(_widthSlider);

        // container for room display
        var preview :Canvas = new Canvas();
        preview.x = preview.y = 50;
        preview.width = preview.height = PREVIEW_BOX_SIZE;
        addChild(preview);
        
        _backdropCanvas = new Canvas();
        _backdropCanvas.x = _backdropCanvas.y = 0;
        _backdropCanvas.width = _backdropCanvas.height = PREVIEW_BOX_SIZE;
        preview.addChild(_backdropCanvas);

        var mask :Canvas = new Canvas();
        mask.x = mask.y = 0;
        mask.width = mask.height = PREVIEW_BOX_SIZE;
        var g :Graphics = mask.graphics;
        g.beginFill(0xffffff); 
        g.drawRect(0, 0, mask.width, mask.height);
        g.endFill();
        preview.addChild(mask);
        preview.mask = mask;

        // UIComponent wrapper is needed around a plain old sprite object
        _wrapper = new UIComponent();
        _media = new DecorMediaContainer(this);
        _wrapper.x = _backdropCanvas.x;
        _wrapper.y = _backdropCanvas.y;
        _wrapper.addChild(_media);
        preview.addChild(_wrapper);
        
        // send an initialization request to GWT
        if (ExternalInterface.available) {
            try {
                ExternalInterface.call("updateDecorInit");
            } catch (e :Error) {
                log.warning("Unable to initialize updates with Javascript: " + e);
            }
        } 
    }

    /**
     * Called whenever any of the UI elements changes.
     */
    protected function processChange (event :SliderEvent) :void
    {
        _data.width = _widthSlider.value;
        _data.horizon = _horizonSlider.value;
        _data.depth = _depthSlider.value;
        refreshPreview();
        
        sendUpdateToJS();
    }

    /**
     * Called from JavaScript, updates this viewer's internal parameters (width, height, etc.)
     */
    public function updateParameters (
        width :int, height :int, depth :int, horizon :Number, type :int) :void
    {
        // update storage
        _data.width = width;
        _data.height = height;
        _data.depth = depth;
        _data.type = type;
        _data.horizon = horizon;

        // update UI
        _widthSlider.value = width;
        _horizonSlider.value = horizon;
        _depthSlider.value = depth;
        refreshPreview();
    }

    /**
     * Called from JavaScript, updates this viewer's media.
     */
    public function updateMedia (mediaPath :String) :void
    {
        _mediaPath = mediaPath;
        _media.setMedia(mediaPath);
        _media.alpha = 0.7;
        refreshPreview();
    }

    /**
     * Refreshes the preview screen.
     */
    public function refreshPreview () :void
    {
        // redraw the room backdrop
        _backdrop.setRoom(_data);
        _backdrop.drawRoom(_backdropCanvas, _backdropCanvas.width, _backdropCanvas.height, true);

        // scale the canvas so that the entire backdrop fits on the canvas
        var scaleX :Number = PREVIEW_BOX_SIZE / _data.width;
        var scaleY :Number = PREVIEW_BOX_SIZE / _data.height;
        var scale :Number = Math.min (scaleX, scaleY);
        _backdropCanvas.scaleX = _backdropCanvas.scaleY = scale;

        // scale the bitmap container with the same scaling parameter
        _wrapper.scaleX = _wrapper.scaleY = scale;

        // center the bitmap horizontally, and align vertically with the bottom of the room
        _media.x = (_data.width - _media.width) / 2;
        _media.y = _data.height - _media.height;
    }
    
    /**
     * Sends the current viewer parameters to JavaScript. Only sends parameters that can change
     * in the viewer (width, height, etc.) - not the media, which can't be modified here.
     */
    public function sendUpdateToJS () :void
    {
        if (ExternalInterface.available) {
            try {
                ExternalInterface.call(
                    "updateDecor", _data.width, _data.height,
                    _data.depth, _data.horizon, _data.type);
            } catch (e :Error) {
                log.warning("Unable to send update to Javascript: " + e);
            }
        } else {
            log.warning("External interface not available, " +
                        "while trying to send an update to Javascript");
        }
    }

    // TEMP: helper function
    public function dlog (message :String) :void
    {
        if (_testing) {
            _results.text = message;
        }
    }

    protected static const PREVIEW_BOX_SIZE :Number = 200;
    
    protected var _testing :Boolean = true;
    
    protected var _results :Label;
    protected var _horizonSlider :VSlider;
    protected var _depthSlider :VSlider;
    protected var _widthSlider :HSlider;

    protected var _mediaPath :String;
    protected var _media :DecorMediaContainer;
    protected var _wrapper :UIComponent;
    protected var _backdropCanvas :Canvas;

    protected var _data :DecorData = new DecorData();
    protected var _backdrop :RoomBackdrop = new RoomBackdrop();

    [Embed(source="../../../../../../../pages/images/item/detail_preview_bg.png")]
    protected static const BACKGROUND :Class;
}
}


import com.threerings.msoy.world.client.DecorViewerComp;
import com.threerings.flash.MediaContainer;

/**
 * Helper class, extends MediaContainer by notifying listeners once the media was loaded.
 */
internal class DecorMediaContainer extends MediaContainer
{
    public function DecorMediaContainer (viewer :DecorViewerComp)
    {
        _viewer = viewer;
    }

    override protected function stoppedLoading () :void
    {
        _viewer.refreshPreview();
    }

    public var _viewer :DecorViewerComp;
}

