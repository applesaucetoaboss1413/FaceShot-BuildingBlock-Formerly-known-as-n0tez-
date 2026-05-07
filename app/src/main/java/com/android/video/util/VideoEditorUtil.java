package com.android.video.util;

import android.content.Context;
import android.graphics.Bitmap;

public final class VideoEditorUtil {
    private static final boolean isLibLoaded;

    static {
        boolean loaded;
        try {
            System.loadLibrary("isffmpeg");
            System.loadLibrary("isvideoengine");
            System.loadLibrary("isvideoutil");
            System.loadLibrary("isplayer");
            System.loadLibrary("isTurboJpeg");
            loaded = true;
        } catch (Throwable t) {
            loaded = false;
        }
        isLibLoaded = loaded;
    }

    private VideoEditorUtil() {
    }

    public static boolean isAvailable() {
        return isLibLoaded;
    }

    public static native boolean nativeCancel();
    public static native int nativeConcatFiles(String[] files, String output);
    public static native int nativeCopyAudio(String src, String dst);
    public static native String nativeGetVideoInfo(Context context, String path);
    public static native String nativeGetFullVideoInfo(Context context, String path);
    public static native long nativeGetNearKeyFrame(String path, long position);
    public static native int nativeOpenVideoFile(String path, int format);
    public static native int nativeGetNextFrame(Bitmap bitmap);
    public static native long nativeSeekTo(long position);
    public static native void nativeRelease();
    public static native int nativeSaveBitmapAsPng(Bitmap bitmap, String path, int width, int height);
    public static native boolean nativeIsSaving();
}
