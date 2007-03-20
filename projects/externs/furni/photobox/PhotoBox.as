//
// $Id$

package {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Sprite;

import flash.text.TextField;
import flash.text.TextFieldType;

import flash.events.Event;
import flash.events.FocusEvent;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.events.TimerEvent;

import flash.net.URLRequest;

import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import flash.ui.Keyboard;

import flash.utils.Timer;

import com.adobe.webapis.flickr.FlickrService;
import com.adobe.webapis.flickr.PagedPhotoList;
import com.adobe.webapis.flickr.Photo;
import com.adobe.webapis.flickr.PhotoSize;
import com.adobe.webapis.flickr.PhotoUrl;
import com.adobe.webapis.flickr.events.FlickrResultEvent;

import com.whirled.ControlEvent;
import com.whirled.FurniControl;

[SWF(width="500", height="550")]
public class PhotoBox extends Sprite
{
    public function PhotoBox ()
    {
        // be prepared to clean up after ourselves...
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload, false, 0, true);

        // configure our conrol
        _furni = new FurniControl(this);
        _furni.addEventListener(ControlEvent.MESSAGE_RECEIVED, handleMessageReceived);
        _furni.addEventListener(ControlEvent.ACTION_TRIGGERED, handleActionTriggered);
        _furni.addEventListener(ControlEvent.GOT_CONTROL, handleGotControl);

        // Set up the flickr service
        // This is my (Ray Greenwell)'s personal Flickr key!!
        _flickr = new FlickrService("7aa4cc43b7fd51f0f118b0022b7ab13e")
        _flickr.addEventListener(FlickrResultEvent.PHOTOS_SEARCH,
            handlePhotoSearchResult);
        _flickr.addEventListener(FlickrResultEvent.PHOTOS_GET_SIZES,
            handlePhotoUrlKnown);

        // try to set up our UI
        var width :int = -1;
        try {
            width = root.loaderInfo.width;
        } catch (err :Error) {
            // we couldn't access the width yet, wait until we can
            root.loaderInfo.addEventListener(Event.COMPLETE, handleLoaded, false, 0, true);
        }
        if (width != -1) {
            configureUI(width);
        }
    }

    /**
     * Waits until we're fully loaded to configure our UI.
     */
    protected function handleLoaded (event :Event) :void
    {
        configureUI(root.loaderInfo.width);
    }

    /**
     * Configure the UI. Called from the constructor.
     */
    protected function configureUI (totalWidth :int) :void
    {
        var logo :DisplayObject = DisplayObject(new LOGO());
        addChild(logo);

//        var prompt :TextField = new TextField();
//        prompt.autoSize = TextFieldAutoSize.LEFT;
//        prompt.background = true;
//        prompt.backgroundColor = 0xFFFFFF;
//        var format :TextFormat = new TextFormat();
//        format.size = 16;
//        format.bold = true;
//        prompt.defaultTextFormat = format;
//        prompt.text = "Enter tags:";
//        prompt.y = logo.height;
//        prompt.autoSize = TextFieldAutoSize.NONE;
//        prompt.width = Math.max(prompt.width, logo.width);
//        addChild(prompt);

        _tagDisplay = new TextField();
        _tagDisplay.background = true;
        _tagDisplay.backgroundColor = 0xFFFFFF;
        _tagDisplay.height = logo.height;
        _tagDisplay.x = logo.width;
        _tagDisplay.width = (totalWidth - logo.width) / 2;

        _tagEntry = new TextField();
        _tagEntry.type = TextFieldType.INPUT;
        _tagEntry.background = true;
        _tagEntry.backgroundColor = 0xCCFFFF;
        _tagEntry.height = logo.height;
        _tagEntry.x = _tagDisplay.x + _tagDisplay.width;
        _tagEntry.width = totalWidth - _tagEntry.x;

        var format :TextFormat = new TextFormat();
        format.size = 18;
        _tagEntry.defaultTextFormat = format;
        _tagDisplay.defaultTextFormat = format;

        addChild(_tagDisplay);
        addChild(_tagEntry);

        _tagEntry.text = "<Click to enter tags>";
        _tagEntry.addEventListener(FocusEvent.FOCUS_IN, handleTagEntryFocus);
        _tagEntry.addEventListener(KeyboardEvent.KEY_DOWN, handleTagEntryKey);

        _loader = new Loader();
        _loader.mouseEnabled = true;
        _loader.mouseChildren = true;
        _loader.addEventListener(MouseEvent.CLICK, handleClick);
        _loader.addEventListener(MouseEvent.ROLL_OVER, handleMouseRoll);
        _loader.addEventListener(MouseEvent.ROLL_OUT, handleMouseRoll);
        _loader.y = logo.height;
        addChild(_loader);

        _overlay = new Sprite();
        _overlay.y = _loader.y;
        addChild(_overlay);

        // request control, or pretend we're it
        if (_furni.isConnected()) {
            _furni.requestControl();

        } else {
            // fake that we got control
            handleGotControl(null);
        }
    }

    /**
     * Handle focus received to our tag entry area.
     */
    protected function handleTagEntryFocus (event :FocusEvent) :void
    {
        // prepare for user input, stop listening
        _tagEntry.text = "";
//        _tagEntry.removeEventListener(FocusEvent.FOCUS_IN, handleTagEntryFocus);
    }

    /**
     * Handle a user-generated keypress.
     */
    protected function handleTagEntryKey (event :KeyboardEvent) :void
    {
        if (event.keyCode == Keyboard.ENTER) {
            var tags :String = _tagEntry.text;
            tags = tags.replace(/\s+/g, ","); // replace spaces with commas
            tags = tags.replace(/,+/g, ","); // prune consecutive commas
            tags = tags.replace(/^,/, ""); // remove spurious comma at start
            tags = tags.replace(/,$/, ""); // remove spurious comma at end

            // unfocus the tag entry area
            // (This seems to work even when we're in a security boundary)
            stage.focus = null; // will trigger unfocus event

            _ourTags = tags;
            _ourPhotos = null;
            _flickr.photos.search("", tags, "all");

        } else {
            // the user is entering stuff, clear everything out
            _ourPhotos = null;
            _ourTags = null;
        }
    }

    /**
     * Handle the results of a tag search.
     */
    protected function handlePhotoSearchResult (evt :FlickrResultEvent) :void
    {
        if (!evt.success) {
            trace("Failure searching for photos " +
                "[" + evt.data.error.errorMessage + "]");
            return;
        }

        // if the tags have since been cleared, throw away these results
        if (_ourTags == null) {
            return;
        }

        // save the metadata about photos
        _ourPhotos = (evt.data.photos as PagedPhotoList).photos;

        if (!_furni.isConnected()) {
            // if we're not connected, just get the next URL immediatly
            getNextPhotoUrl();
        }
    }

    /**
     * Get the next URL for photos that we ourselves have found via tags.
     */
    protected function getNextPhotoUrl () :void
    {
        if (_ourPhotos == null || _ourPhotos.length == 0) {
            _ourPhotos = null;
            return;
        }

        var photo :Photo = (_ourPhotos.shift() as Photo);
        _ourPageURL = "http://www.flickr.com/photos/" + photo.ownerId + "/" + 
            photo.id;
        _flickr.photos.getSizes(photo.id);
    }

    /**
     * Handle data arriving as a result of a getSizes() request.
     */
    protected function handlePhotoUrlKnown (evt :FlickrResultEvent) :void
    {
        if (!evt.success) {
            trace("Failure getting photo sizes " +
                "[" + evt.data.error.errorMessage + "]");
            return;
        }

        // if either of these are null, the user has started searching
        // on new tags...
        if (_ourTags == null || _ourPhotos == null) {
            return;
        }

        var sizes :Array = (evt.data.photoSizes as Array);
        var url :String = getMediumPhotoSource(sizes);
        if (url != null) {
            // yay! We've looked-up our next photo item
            _ourReadyPhoto = [ url, _ourPageURL, _ourTags ];

            if (_furni.isConnected()) {
                // send a message to the instance in control..
                _furni.sendMessage("queue", _ourReadyPhoto);

            } else {
                // just freaking show it
                showPhoto(_ourReadyPhoto);
            }
        }
    }

    /**
     * Handle a command to show a photo from the entity that's in control.
     */
    protected function handleActionTriggered (event :ControlEvent) :void
    {
        switch (event.name) {
        case "show":
            showPhoto(event.value as Array);
            break;

        case "send_photos":
            // hey, the instance in control wants us to send our goodies!
            if (_ourReadyPhoto != null) {
                _furni.sendMessage("queue", _ourReadyPhoto);

            } else {
                getNextPhotoUrl();
            }
        }
    }

    /**
     * Show the photo specified.
     */
    protected function showPhoto (photo :Array) :void
    {
        clearLoader();
        var url :String = String(photo[0]);
        _displayPageURL = String(photo[1]);
        _tagDisplay.text = String(photo[2]);
        _loader.load(new URLRequest(url));

        // if it's our personal photo, clear it 
        if ((_ourReadyPhoto != null) && (url == _ourReadyPhoto[0])) {
            _ourReadyPhoto = null;
        }
    }

    /**
     * Given an array of PhotoSize objects, return the source url
     * for the medium size photo.
     */
    protected function getMediumPhotoSource (sizes :Array) :String
    {
        for each (var p :PhotoSize in sizes) {
            if (p.label == "Medium") {
                return p.source;
            }
        }

        return null;
    }

    /**
     * Clear any resources from the loader and prepare it to load
     * another photo, or be unloaded.
     */
    protected function clearLoader () :void
    {
        try {
            _loader.close();
        } catch (e :Error) {
            // nada
        }
        _loader.unload();
        _displayPageURL = null;
        handleMouseRoll(null);
    }

    /**
     * Handle a click.
     */
    protected function handleClick (event :MouseEvent) :void
    {
        if (_displayPageURL == null) {
            return;
        }
        try {
            flash.net.navigateToURL(new URLRequest(_displayPageURL));
        } catch (err :Error) {
            trace("Oh my gosh: " + err);
        }
    }

    protected function handleMouseRoll (event :MouseEvent) :void
    {
        var draw :Boolean = (event == null || event.type == MouseEvent.ROLL_OVER) &&
            (_displayPageURL != null);

        with (_overlay.graphics) {
            clear();
            if (draw) {
                lineStyle(1, 0xFF4040);
                drawRect(0, 0, _loader.width - 1, _loader.height - 1);
            }
        }
    }

    /**
     * Take care of releasing resources when we unload.
     */
    protected function handleUnload (event :Event) :void
    {
        if (_ctrlTimer != null) {
            _ctrlTimer.stop();
            _ctrlTimer = null;
        }
        clearLoader();
    }

    // ============ Methods only used on the instance in "control"

    protected function handleGotControl (event :ControlEvent) :void
    {
        // set up the control timer, only used by the one in control...
        _ctrlTimer = new Timer(7000); // 7 seconds
        _ctrlTimer.addEventListener(TimerEvent.TIMER, handleCtrlTimer);
        _ctrlTimer.start();

        handleCtrlTimer(); // kick things off
    }

    protected function handleCtrlTimer (event :TimerEvent = null) :void
    {
        // if we're not even connected
        // call this by hand..
        if (!_furni.isConnected()) {
            getNextPhotoUrl();
            return;
        }

        if (_displayPhotos == null || _displayPhotos.length == 0) {
            // send out a message to all other boxes that we're
            // ready for their next photo
            _displayPhotos = null;
            _furni.triggerAction("send_photos");
            return;
        }

        // otherwise, trigger an action to show the next photo
        var nextPhoto :Array = (_displayPhotos.shift() as Array);
        _furni.triggerAction("show", nextPhoto)
    }

    /**
     * Handle a message event from other instances of this photobox
     * running on other clients.
     */
    protected function handleMessageReceived (event :ControlEvent) :void
    {
        if (!_furni.hasControl()) {
            // ignore messages from the ones not in control.
            return;
        }

        if (event.name == "queue") {
            var photoInfo :Array  = (event.value as Array);

            if (_displayPhotos == null) {
                // show it immediately, create the array as a marker
                // to not show the next one immediately.
                _displayPhotos = [];
                _furni.triggerAction("show", photoInfo);

            } else {
                // we'll save that for later
                _displayPhotos.push(photoInfo);
            }
        }
    }

    /** The interface through which we communicate with metasoy. */
    protected var _furni :FurniControl;

    /** The interface through which we make flickr API requests. */
    protected var _flickr :FlickrService;

    /** The text area to display tags. */
    protected var _tagDisplay :TextField;

    /** The text entry area for tags. */
    protected var _tagEntry :TextField;

    /** Loads up photos for display. */
    protected var _loader :Loader;

    /** A sprite drawn on top of everything, for use in drawing UI. */
    protected var _overlay :Sprite;

    /** The tags we've entered, associated with ourPhotos. */
    protected var _ourTags :String;

    /** The high-level metadata for the result set of photos from our
     * tag search. */
    protected var _ourPhotos :Array;

    /** The url of the photo page for the photo we're currently doing
     * a size lookup upon. */
    protected var _ourPageURL :String;

    /** The full data for the next photo we'd like to show. */
    protected var _ourReadyPhoto :Array;

    /** The page url for the photo we're currently showing. */
    protected var _displayPageURL :String;

    //=========================

    /** Timer used by the instance in control to coordinate the others. */
    protected var _ctrlTimer :Timer;

    /** The photos to display. */
    protected var _displayPhotos :Array;

    [Embed(source="flickr_logo.gif")]
    protected const LOGO :Class;
}
}
