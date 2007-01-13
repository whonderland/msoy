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

import com.threerings.msoy.export.FurniControl;

[SWF(width="500", height="550")]
public class PhotoBox extends Sprite
{
    public function PhotoBox ()
    {
        // configure our conrols
        _furni = new FurniControl(this);
        _furni.eventTriggered = handleMsoyEvent;

        // this is my (Ray Greenwell)'s personal Flickr key!!
        _flickr = new FlickrService("7aa4cc43b7fd51f0f118b0022b7ab13e")
        _flickr.addEventListener(FlickrResultEvent.PHOTOS_SEARCH,
            handlePhotoSearch);
        _flickr.addEventListener(FlickrResultEvent.PHOTOS_GET_SIZES,
            handlePhotoSizes);

        _timer = new Timer(7000, 1); // 7 seconds, fire once
        _timer.addEventListener(TimerEvent.TIMER, handleTimer);

        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        configureUI();
    }

    /**
     * Configure the UI. Called from the constructor.
     */
    protected function configureUI () :void
    {
        var logo :DisplayObject = new FlickrLogo_Asset();
        addChild(logo);

        var prompt :TextField = new TextField();
        prompt.autoSize = TextFieldAutoSize.LEFT;
        prompt.background = true;
        prompt.backgroundColor = 0xFFFFFF;
        var format :TextFormat = new TextFormat();
        format.size = 16;
        format.bold = true;
        prompt.defaultTextFormat = format;
        prompt.text = "Enter tags:";
        prompt.y = logo.height;
        prompt.autoSize = TextFieldAutoSize.NONE;
        prompt.width = Math.max(prompt.width, logo.width);
        addChild(prompt);

        _tagEntry = new TextField();
        _tagEntry.type = TextFieldType.INPUT;
        _tagEntry.background = true;
        _tagEntry.backgroundColor = 0xCCFFFF;
        _tagEntry.x = Math.max(prompt.width, logo.width);
        _tagEntry.height = prompt.height + logo.height;
        _tagEntry.width = 500 - _tagEntry.x;
        addChild(_tagEntry);
        _tagEntry.addEventListener(KeyboardEvent.KEY_DOWN, handleKey);
        _tagEntry.addEventListener(FocusEvent.FOCUS_IN, handleTagFocus);
        _tagEntry.addEventListener(FocusEvent.FOCUS_OUT, handleTagFocus);
        format = new TextFormat();
        format.size = 36;
        _tagEntry.defaultTextFormat = format;

        _loader = new Loader();
        _loader.y = 50;
        addChild(_loader);
    }

    /**
     * Handle the results of a tag search.
     */
    protected function handlePhotoSearch (evt :FlickrResultEvent) :void
    {
        if (!evt.success) {
            trace("Failure searching for photos " +
                "[" + evt.data.error.errorMessage + "]");
            return;
        }

        _photos = (evt.data.photos as PagedPhotoList).photos;
        loadNextPhoto();
    }

    /**
     * Load the next photo in the photo list maintained by this
     * photobox.
     */
    protected function loadNextPhoto () :void
    {
        if (_photos == null || _photos.length == 0) {
            _photos = null;
            return;
        }

        var photo :Photo = (_photos.shift() as Photo);
        _flickr.photos.getSizes(photo.id);
    }

    /**
     * Handle data arriving as a result of a getSizes() request.
     */
    protected function handlePhotoSizes (evt :FlickrResultEvent) :void
    {
        if (!evt.success) {
            trace("Failure getting photo sizes " +
                "[" + evt.data.error.errorMessage + "]");
            return;
        }
        _controlId++;

        var sizes :Array = (evt.data.photoSizes as Array);
        var url :String = getMediumPhotoSource(sizes);
        if (url != null) {
            displayPhoto(url);
            // always broadcast if we can
            if (_furni.isConnected()) {
                _furni.triggerEvent("show", [ _controlId, url, _enteredTags ]);
            }
        }

        // if our photos aren't null, restart the timer
        if (_photos != null) {
            _timer.reset();
            _timer.start();
        }
    }

    /**
     * Handle focus changes to our tag entry area.
     */
    protected function handleTagFocus (event :FocusEvent) :void
    {
        if (event.type == FocusEvent.FOCUS_IN) {
            _tagEntry.backgroundColor = 0xFFFFFF;
            _tagEntry.text = "";
            _hasFocus = true;

        } else {
            _tagEntry.backgroundColor = 0xCCFFFF;
            _tagEntry.text = _displayedTags;
            _hasFocus = false;
        }
    }

    /**
     * Handle a user-generated keypress.
     */
    protected function handleKey (event :KeyboardEvent) :void
    {
        if (event.keyCode == Keyboard.ENTER) {
            var tags :String = _tagEntry.text;
            tags = tags.replace(/\s+/g, ","); // replace spaces with commas
            tags = tags.replace(/,+/g, ","); // prune consecutive commas
            tags = tags.replace(/^,/, ""); // remove spurious comma at start
            tags = tags.replace(/,$/, ""); // remove spurious comma at end
            _enteredTags = tags;
            _displayedTags = tags;
            _shownOwn = false;
            _flickr.photos.search("", tags, "all");

            // unfocus the tag entry area
            // (This seems to work even when we're in a security boundary)
            stage.focus = null; // will trigger unfocus event
        }
    }

    /**
     * Handle the timer expiring.
     */
    protected function handleTimer (event :TimerEvent) :void
    {
        loadNextPhoto();
    }

    /**
     * Handle a "trigger" event from other instances of this photobox
     * running on other clients.
     */
    protected function handleMsoyEvent (event :String, arg :Object) :void
    {
        // ok, the problem is that if someone enters a room their
        // photobox is controlId=0, all the other boxes have a higher id.
        // until an event arrives from a higher box, the low box
        // won't know that it's the new guy, so it may display up to 1
        // photo incorrectly, only noticing when the new photo arrives
        // from a higher box.

        // also: sometimes when you start two at the same time, they both
        // stop: WHY?

        if (event == "show") {
            var args :Array  = (arg as Array);
            var newId :int = int(args[0]);
            var url :String = String(args[1]);
            var tags :String = String(args[2]);
            if (newId > _controlId) {
                _controlId = newId;
                _photos = null; // kill our own display of photos

            } else if (newId == _controlId) {
                if (tags == _enteredTags) {
                    _shownOwn = true;

                } else if (!_shownOwn) {
                    // sorry charlie, we were second to try this controlId
                    _photos = null;
                }
            }

            if (_photos == null) {
                displayPhoto(url);
                _displayedTags = tags;
                if (!_hasFocus) {
                    _tagEntry.text = tags;
                }

            } else {
                // else, ignore our own events
                //trace("ignoring " + url);
            }
        }
    }

    /**
     * Take care of releasing resources when we unload.
     */
    protected function handleUnload (event :Event) :void
    {
        _timer.stop();
        clearLoader();
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
     * Display the photo at the specified url.
     */
    protected function displayPhoto (url :String) :void
    {
        clearLoader();
        _loader.load(new URLRequest(url));
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
    }

    /** The interface through which we make flickr API requests. */
    protected var _flickr :FlickrService;

    /** The interface through which we communicate with metasoy. */
    protected var _furni :FurniControl;

    /** Handles the countdown to showing the next photo. */
    protected var _timer :Timer;

    /** The text entry area for tags. */
    protected var _tagEntry :TextField;

    protected var _hasFocus :Boolean;

    protected var _displayedTags :String = "";

    protected var _enteredTags :String;

    /** Whether or not we've shown one of our own at the current controlId. */
    protected var _shownOwn :Boolean;

    /** Loads up photos for display. */
    protected var _loader :Loader;

    /** If this instance was used to do a tag search, this contains the
     * resultant photos which are queued up for display on other instances.  */
    protected var _photos :Array;

    /** The 'control id' which determines which piece of furni is in control. */
    protected var _controlId :int = 0;
}
}
