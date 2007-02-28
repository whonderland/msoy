//
// $Id$

package com.threerings.msoy.item.web;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

import com.threerings.msoy.web.client.DeploymentConfig;

/**
 * Contains information about a piece of media.
 */
public class MediaDesc implements Streamable, IsSerializable
{
    /** The MIME type for plain UTF-8 text. */
    public static final byte TEXT_PLAIN = 0;

    /** The MIME type for PNG image data. */
    public static final byte IMAGE_PNG = 10;

    /** The MIME type for JPEG image data. */
    public static final byte IMAGE_JPEG = 11;

    /** The MIME type for GIF image data. */
    public static final byte IMAGE_GIF = 12;

    /** The MIME type for MPEG audio data. */
    public static final byte AUDIO_MPEG = 20;

//    /** The MIME type for WAV audio data. */
//    public static final byte AUDIO_WAV = 21;

    /** The MIME type for FLV video data. */
    public static final byte VIDEO_FLASH = 30;

    /** The MIME type for MPEG video data. */
    public static final byte VIDEO_MPEG = 31;

    /** The MIME type for Quicktime video data. */
    public static final byte VIDEO_QUICKTIME = 32;

    /** The MIME type for AVI video data. */
    public static final byte VIDEO_MSVIDEO = 33;

    /** The MIME type for Flash SWF files. */
    public static final byte APPLICATION_SHOCKWAVE_FLASH = 40;

    /** The MIME type for Java JAR files. */
    public static final byte APPLICATION_JAVA_ARCHIVE = 41;

    /** Identifies that a "half thumbnail" sized image is desired. */
    public static final int HALF_THUMBNAIL_SIZE = 0;

    /** Identifies that a thumbnail sized image is desired. */
    public static final int THUMBNAIL_SIZE = 1;

    /** Identifies that a preview sized image is desired. */
    public static final int PREVIEW_SIZE = 2;

    /** The thumbnail image width.  */
    public static final int THUMBNAIL_WIDTH = 160;

    /** The thumbnail image height.  */
    public static final int THUMBNAIL_HEIGHT = 120;

    /** Defines the dimensions of our various image sizes. */
    public static final int[] DIMENSIONS = {
        THUMBNAIL_WIDTH/2, THUMBNAIL_HEIGHT/2, // half thumbnail size
        THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, // thumbnail size
        THUMBNAIL_WIDTH*2, THUMBNAIL_HEIGHT*2, // preview size
    };

    /** A constant used to indicate that an image does not exceed half thumbnail size in either
     * dimension. */
    public static final byte NOT_CONSTRAINED = 0;

    /** A constant used to indicate that an image exceeds thumbnail size proportionally more in the
     * horizontal dimension. */
    public static final byte HORIZONTALLY_CONSTRAINED = 1;

    /** A constant used to indicate that an image exceeds thumbnail size proportionally more in the
     * vertical dimension. */
    public static final byte VERTICALLY_CONSTRAINED = 2;

    /** A constant used to indicate that an image exceeds half thumbnail size proportionally more
     * in the horizontal dimension but does not exceed thumbnail size in either dimension. */
    public static final byte HALF_HORIZONTALLY_CONSTRAINED = 3;

    /** A constant used to indicate that an image exceeds half thumbnail size proportionally more
     * in the vertical dimension but does not exceed thumbnail size in either dimension. */
    public static final byte HALF_VERTICALLY_CONSTRAINED = 4;

    /** The SHA-1 hash of this media's data. */
    public byte[] hash;

    /** The MIME type of the media associated with this item. */
    public byte mimeType;

    /** The size constraint on this media, if any. See {@link #computeConstraint}. */
    public byte constraint;

    /**
     * Get the path of the URL for the media specified.
     */
    public static String getMediaPath (byte[] mediaHash, byte mimeType)
    {
        return DeploymentConfig.mediaURL + hashToString(mediaHash) +
            mimeTypeToSuffix(mimeType);
    }

    /**
     * Convert the specified media hash into a String.
     */
    public static String hashToString (byte[] hash)
    {
        if (hash == null) {
            return "";
        }
        char[] chars= new char[hash.length * 2];
        for (int ii = 0; ii < hash.length; ii++) {
            int val = hash[ii];
            if (val < 0) {
                val += 256;
            }
            chars[2 * ii] = HEX.charAt(val/16);
            chars[2 * ii + 1] = HEX.charAt(val%16);
        }
        return new String(chars);
    }

    /**
     * Convert the specified String back into a media hash.
     */
    public static byte[] stringToHash (String hash)
    {
        if (hash == null || hash.length() % 2 != 0) {
            return null;
        }

        hash = hash.toLowerCase();
        byte[] data = new byte[hash.length() / 2];
        for (int ii = 0; ii < hash.length(); ii += 2) {
            int value = (byte) (HEX.indexOf(hash.charAt(ii)) << 4);
            value += HEX.indexOf(hash.charAt(ii + 1));

            // values over 127 are wrapped around, restoring negative bytes
            data[ii / 2] = (byte) value;
        }

        return data;
    }

    /**
     * Maps the supplied string representation of a mime type to our internal
     * integer code. Returns -1 if the mime type is unknown.
     */
    public static byte stringToMimeType (String mimeType)
    {
        mimeType = mimeType.toLowerCase();
        if (mimeType.equals("text/plain")) {
            return TEXT_PLAIN;
        } else if (mimeType.equals("image/png")) {
            return IMAGE_PNG;
        } else if (mimeType.equals("image/jpeg")) {
            return IMAGE_JPEG;
        } else if (mimeType.equals("image/gif")) {
            return IMAGE_GIF;
        } else if (mimeType.equals("audio/mpeg")) {
            return AUDIO_MPEG;
//        } else if (mimeType.equals("audio/wav")) {
//            return AUDIO_WAV;
        } else if (mimeType.equals("video/flash")) {
            return VIDEO_FLASH;
        } else if (mimeType.equals("video/mpeg")) {
            return VIDEO_MPEG;
        } else if (mimeType.equals("video/quicktime")) {
            return VIDEO_QUICKTIME;
        } else if (mimeType.equals("video/msvideo")) {
            return VIDEO_MSVIDEO;
        } else if (mimeType.equals("application/x-shockwave-flash")) {
            return APPLICATION_SHOCKWAVE_FLASH;
        } else if (mimeType.equals("application/java-archive")) {
            return APPLICATION_JAVA_ARCHIVE;
        } else {
            return -1;
        }
    }

    /**
     * Maps the supplied filename suffix to a mime type. Returns -1 if the
     * suffix is unknown.
     */
    public static byte suffixToMimeType (String filename)
    {
        filename = filename.toLowerCase();
        if (filename.endsWith(".txt")) {
            return TEXT_PLAIN;
        } else if (filename.endsWith(".png")) {
            return IMAGE_PNG;
        } else if (filename.endsWith(".jpg")) {
            return IMAGE_JPEG;
        } else if (filename.endsWith(".gif")) {
            return IMAGE_GIF;
        } else if (filename.endsWith(".mp3")) {
            return AUDIO_MPEG;
//        } else if (filename.endsWith(".wav")) {
//            return AUDIO_WAV;
        } else if (filename.endsWith(".flv")) {
            return VIDEO_FLASH;
        } else if (filename.endsWith(".mpg")) {
            return VIDEO_MPEG;
        } else if (filename.endsWith(".mov")) {
            return VIDEO_QUICKTIME;
        } else if (filename.endsWith(".avi")) {
            return VIDEO_MSVIDEO;
        } else if (filename.endsWith(".swf")) {
            return APPLICATION_SHOCKWAVE_FLASH;
        } else if (filename.endsWith(".jar")) {
            return APPLICATION_JAVA_ARCHIVE;
        } else {
            return -1;
        }
    }

    /**
     * Returns a file suffix for use with the specified mime tpye or .dat if
     * mime type is unknown.
     */
    public static String mimeTypeToSuffix (byte mimeType)
    {
        switch (mimeType) {
        case TEXT_PLAIN: return ".txt";
        case IMAGE_PNG: return ".png";
        case IMAGE_JPEG: return ".jpg";
        case IMAGE_GIF: return ".gif";
        case AUDIO_MPEG: return ".mp3";
//        case AUDIO_WAV: return ".wav";
        case VIDEO_FLASH: return ".flv";
        case VIDEO_MPEG: return ".mpg";
        case VIDEO_QUICKTIME: return ".mov";
        case VIDEO_MSVIDEO: return ".avi";
        case APPLICATION_SHOCKWAVE_FLASH: return ".swf";
        case APPLICATION_JAVA_ARCHIVE: return ".jar";
        default: return ".dat";
        }
    }

    /**
     * Maps the supplied integer representation of a mime type to the standard string
     * representation. Returns "application/octet-stream".
     */
    public static String mimeTypeToString (byte mimeType)
    {
        switch (mimeType) {
        case TEXT_PLAIN: return "text/plain";
        case IMAGE_PNG: return "image/png";
        case IMAGE_JPEG: return "image/jpeg";
        case IMAGE_GIF: return "image/gif";
        case AUDIO_MPEG: return "audio/mpeg";
//        case AUDIO_WAV: return "audo/wav";
        case VIDEO_FLASH: return "video/flash";
        case VIDEO_MPEG: return "video/mpeg";
        case VIDEO_QUICKTIME: return "video/quicktime";
        case VIDEO_MSVIDEO: return "video/msvideo";
        case APPLICATION_SHOCKWAVE_FLASH: return "application/x-shockwave-flash";
        case APPLICATION_JAVA_ARCHIVE: return "application/java-archive";
        default: return "application/octet-stream";
        }
    }

    /**
     * Returns true if the supplied mime type is a supported image type.
     */
    public static boolean isImage (byte mimeType)
    {
        switch (mimeType) {
        case IMAGE_PNG:
        case IMAGE_JPEG:
        case IMAGE_GIF:
            return true;

        default:
            return false;
        }
    }

    /**
     * Computes the constraining dimension for an image (if any) based on the supplied target and
     * actual dimensions.
     */
    public static byte computeConstraint (int size, int actualWidth, int actualHeight)
    {
        float wfactor = (float)DIMENSIONS[2*size] / actualWidth;
        float hfactor = (float)DIMENSIONS[2*size+1] / actualHeight;
        if (wfactor > 1 && hfactor > 1) {
            // if we're computing the size of a thumbnail image, see if it is constrained at half
            // size or still unconstrained
            if (size == THUMBNAIL_SIZE) {
                return computeConstraint(HALF_THUMBNAIL_SIZE, actualWidth, actualHeight);
            } else {
                return NOT_CONSTRAINED;
            }
        } else if (wfactor < hfactor) {
            return (size == HALF_THUMBNAIL_SIZE) ?
                HALF_HORIZONTALLY_CONSTRAINED : HORIZONTALLY_CONSTRAINED;
        } else {
            return (size == HALF_THUMBNAIL_SIZE) ?
                HALF_VERTICALLY_CONSTRAINED : VERTICALLY_CONSTRAINED;
        }
    }

    /** Used for unserialization. */
    public MediaDesc ()
    {
    }

    /**
     * Creates a media descriptor from the supplied configuration.
     */
    public MediaDesc (byte[] hash, byte mimeType)
    {
        this(hash, mimeType, NOT_CONSTRAINED);
    }

    /**
     * Creates a media descriptor from the supplied configuration.
     */
    public MediaDesc (byte[] hash, byte mimeType, byte constraint)
    {
        this.hash = hash;
        this.mimeType = mimeType;
        this.constraint = constraint;
    }

    /**
     * TEMPORARY CONSTRUCTOR, for making it easy for me to
     * pre-initialize some media...
     */
    public MediaDesc (String filename)
    {
        this(stringToHash(filename.substring(0, filename.indexOf('.'))),
             (byte) suffixToMimeType(filename));
    }

    /**
     * Returns the path of the URL that references this media.
     */
    public String getMediaPath ()
    {
        return getMediaPath(hash, mimeType);
    }

    /**
     * Return true if this media has a visual component that can be shown in
     * flash.
     */
    public boolean hasFlashVisual ()
    {
        switch (mimeType) {
        case IMAGE_PNG:
        case IMAGE_JPEG:
        case IMAGE_GIF:
        case VIDEO_FLASH:
        case APPLICATION_SHOCKWAVE_FLASH:
            return true;

        default:
            return false;
        }
    }

    /**
     * Is this media merely an image type?
     */
    public boolean isImage ()
    {
        return isImage(mimeType);
    }

    /**
     * Is this media purely audio?
     */
    public boolean isAudio ()
    {
        switch (mimeType) {
        case AUDIO_MPEG:
//        case AUDIO_WAV:
            return true;

        default:
            return false;
        }
    }

    // @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof MediaDesc) {
            MediaDesc that = (MediaDesc) other;
            return (this.mimeType == that.mimeType) && (this.constraint == that.constraint) &&
                arraysEqual(this.hash, that.hash);
        }
        return false;
    }

    // @Override // from Object
    public int hashCode ()
    {
        int code = 0;
        for (int ii = Math.min(3, hash.length - 1); ii >= 0; ii--) {
            code <<= 8;
            code |= hash[ii];
        }
        return code;
    }

    // @Override // from Object
    public String toString ()
    {
        return hashToString(hash) + mimeTypeToSuffix(mimeType);
    }

    /**
     * Helper function for {@link #equals} because we must work in JavaScript
     * land.
     */
    protected static boolean arraysEqual (byte[] left, byte[] right)
    {
        if (left == right) {
            return true;
        }
        if (left == null || right == null || left.length != right.length) {
            return false;
        }
        for (int ii = 0; ii < left.length; ii++) {
            if (left[ii] != right[ii]) {
                return false;
            }
        }
        return true;
    }

    /** Hexidecimal digits. */
    protected static final String HEX = "0123456789abcdef";
}
