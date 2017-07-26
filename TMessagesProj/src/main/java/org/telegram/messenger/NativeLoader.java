/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.messenger;

import android.content.Context;

public class NativeLoader {

    private final static int LIB_VERSION = 27;
    private final static String LIB_NAME = "tmessages." + LIB_VERSION;

    private static volatile boolean nativeLoaded = false;

    public static synchronized void initNativeLibs(Context context) {
        if (nativeLoaded) {
            return;
        }

        try {
            System.loadLibrary(LIB_NAME);
            init(null, BuildVars.DEBUG_VERSION);
            nativeLoaded = true;
        } catch (Error e) {
            FileLog.e(e);
        }
    }

    private static native void init(String path, boolean enable);
    //public static native void crash();
}
