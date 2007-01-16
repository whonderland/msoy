package com.threerings.msoy.client {

import flash.display.Shape;

import flash.events.Event;

import flash.system.Capabilities;

import mx.core.Application;
import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;

import mx.events.ResizeEvent;

import com.threerings.util.ArrayUtil;
import com.threerings.util.Name;

import com.threerings.crowd.client.PlaceView;

public class TopPanel extends Canvas
{
    /** The control bar. */
    public var controlBar :ControlBar;

    /**
     * Construct the top panel.
     */
    public function TopPanel (ctx :MsoyContext)
    {
        _ctx = ctx;
        percentWidth = 100;
        percentHeight = 100;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        _placeBox = new Canvas();
        _placeBox.includeInLayout = false;
        addChild(_placeBox);

        // set up a mask on the placebox
        _placeMask = new Shape();
        _placeBox.mask = _placeMask;
        _placeBox.rawChildren.addChild(_placeMask);

        // set up the control bar
        controlBar = new ControlBar(ctx);
        controlBar.includeInLayout = false;
        controlBar.setStyle("top", 0);
        controlBar.setStyle("left", 0);
        addChild(controlBar);

        if (DeploymentConfig.devClient) {
            // set up the build stamp label
            _buildStamp = new Label();
            _buildStamp.includeInLayout = false;
            _buildStamp.mouseEnabled = false;
            _buildStamp.mouseChildren = false;
            _buildStamp.text = "Build: " + DeploymentConfig.buildTime + "  " +
                Capabilities.version;
            _buildStamp.setStyle("color", "#F7069A");
            _buildStamp.setStyle("fontSize", 12);
            _buildStamp.setStyle("fontWeight", "bold");
            _buildStamp.setStyle("top", 41);
            addChild(_buildStamp);
        }

        // clear out the application and install ourselves as the only child
        var app :Application = Application(Application.application);
        app.removeAllChildren();
        app.addChild(this);
        layoutPanels();

        app.stage.addEventListener(Event.RESIZE, stageResized);
    }

    protected function stageResized (event :Event) :void
    {
        layoutPanels();
    }

    public function getPlaceView () :PlaceView
    {
        return _placeView;
    }

    public function setPlaceView (view :PlaceView) :void
    {
        clearPlaceView(null);
        _placeView = view;

        var comp :UIComponent = (view as UIComponent);
        comp.setStyle("left", 0);
        comp.setStyle("top", 0);
        comp.setStyle("right", 0);
        comp.setStyle("bottom", 0);
        _placeBox.addChild(comp);
    }

    /**
     * Clear the specified place view, or null to clear any.
     */
    public function clearPlaceView (view :PlaceView) :void
    {
        if ((_placeView != null) && (view == null || view == _placeView)) {
            _placeBox.removeChild(_placeView as UIComponent);
            _placeView = null;
        }
    }

    public function setSidePanel (side :UIComponent) :void
    {
        clearSidePanel(null);
        _sidePanel = side;
        _sidePanel.includeInLayout = false;
//        _sidePanel.addEventListener(ResizeEvent.RESIZE, panelResized);

        addChild(_sidePanel); // add to end
        layoutPanels();
    }

    /**
     * Clear the specified side panel, or null to clear any.
     */
    public function clearSidePanel (side :UIComponent) :void
    {
        if ((_sidePanel != null) && (side == null || side == _sidePanel)) {
            removeChild(_sidePanel);
//            _sidePanel.removeEventListener(ResizeEvent.RESIZE, panelResized);
            _sidePanel = null;
            layoutPanels();
        }
    }

    public function setBottomPanel (bottom :UIComponent) :void
    {
        clearBottomPanel(null);
        _bottomPanel = bottom;
        _bottomPanel.includeInLayout = false;
//        _bottomPanel.addEventListener(ResizeEvent.RESIZE, panelResized);

        addChild(_bottomPanel); // add to end
        layoutPanels();
    }
    
    public function clearBottomPanel (bottom :UIComponent) :void
    {
        if ((_bottomPanel != null) && (bottom == null || bottom == _bottomPanel)) {
            removeChild(_bottomPanel);
//            _bottomPanel.removeEventListener(ResizeEvent.RESIZE, panelResized);
            _bottomPanel = null;
            layoutPanels();
        }
    }
    
    public function showFriends (show :Boolean) :void
    {
        if (show) {
            // lazy-init the friendslist
            if (_friendsList == null) {
                _friendsList = new FriendsList(_ctx);
            }
            // put the pals list atop everything else
            addChild(_friendsList);

        } else {
            if (_friendsList != null) {
                removeChild(_friendsList);
            }
        }
    }

// TODO: doesn't work, we're using hardcoded panel sizes now
//    protected function panelResized (event :ResizeEvent) :void
//    {
//        layoutPanels();
//    }

    protected function layoutPanels () :void
    {
        var sidePanelWidth :int = getSidePanelWidth(),
            bottomPanelHeight :int = getBottomPanelHeight();
            
        _placeBox.setStyle("top", ControlBar.HEIGHT);
        _placeBox.setStyle("bottom", bottomPanelHeight);
        _placeBox.setStyle("left", sidePanelWidth);
        _placeBox.setStyle("right", 0);
        
        if (_sidePanel != null) {
            _sidePanel.setStyle("top", ControlBar.HEIGHT);
            _sidePanel.setStyle("bottom", bottomPanelHeight);
            _sidePanel.setStyle("left", 0);
            _sidePanel.setStyle("right", unscaledWidth - sidePanelWidth);
        }
        
        if (_bottomPanel != null) {    
            _bottomPanel.setStyle("top", unscaledHeight - bottomPanelHeight);
            _bottomPanel.setStyle("bottom", 0);
            _bottomPanel.setStyle("left", 0);
            _bottomPanel.setStyle("right", unscaledWidth - ControlBar.WIDTH);
        }
            
        adjustPlaceMask();
    }

    protected function adjustPlaceMask () :void
    {
        _placeMask.graphics.clear();
        _placeMask.graphics.beginFill(0xFFFFFF);
        _placeMask.graphics.drawRect(0, 0,
            stage.stageWidth - getSidePanelWidth(),
            stage.stageHeight - ControlBar.HEIGHT - getBottomPanelHeight());
        _placeMask.graphics.endFill();
    }

    protected function getSidePanelWidth () :int
    {
        return (_sidePanel == null ? 0 : SIDE_PANEL_WIDTH);
    }
    
    protected function getBottomPanelHeight () :int
    {
        return (_bottomPanel == null ? 0 : BOTTOM_PANEL_HEIGHT);
    }
        
    /** The giver of life. */
    protected var _ctx :MsoyContext;

    /** The current place view. */
    protected var _placeView :PlaceView;

    /** The box that will hold the placeview. */
    protected var _placeBox :Canvas;

    /** The mask configured on the placeview so that it doesn't overlap
     * our other components. */
    protected var _placeMask :Shape;

    /** The current side panel component. */
    protected var _sidePanel :UIComponent;

    /** The current bottom panel component. */
    protected var _bottomPanel :UIComponent;
    
    /** The list of our friends. */
    protected var _friendsList :FriendsList;
    
    /** A label indicating the build stamp for the client. */
    protected var _buildStamp :Label;

    protected static const SIDE_PANEL_WIDTH :int = 350;
    
    protected static const BOTTOM_PANEL_HEIGHT :int = 50;
}
}
