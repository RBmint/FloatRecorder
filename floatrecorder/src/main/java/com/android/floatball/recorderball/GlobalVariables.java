package com.android.floatball.recorderball;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;

class GlobalVariables {
    static String name;
    static boolean isRSon;
    static String mVideoPath;
    static int number = 1;
    static String time;

    private static Intent mResultIntent = null;
    static Intent getResultIntent() {
        return mResultIntent;
    }
    static void setResultIntent(Intent mResultIntent) {
        GlobalVariables.mResultIntent = mResultIntent;
    }

    private static int mResultCode = 0;
    static int getResultCode() {
        return mResultCode;
    }
    static void setResultCode(int mResultCode) {
        GlobalVariables.mResultCode = mResultCode;
    }

    private static MediaProjectionManager mMpmngr;
    static MediaProjectionManager getMpmngr() {
        return mMpmngr;
    }
    static void setMpmngr(MediaProjectionManager mMpmngr) {
        GlobalVariables.mMpmngr = mMpmngr;
    }
}
