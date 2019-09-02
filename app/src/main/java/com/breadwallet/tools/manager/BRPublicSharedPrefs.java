package com.breadwallet.tools.manager;

import android.content.Context;
import android.content.SharedPreferences;

public class BRPublicSharedPrefs {
    public static final String TAG = BRPublicSharedPrefs.class.getName();

    public static String PREFS_NAME = "MyPublicPrefsFile";

    public static boolean getUseFingerprint(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("useFingerprint", false);
    }

    public static void putUseFingerprint(Context activity, boolean use) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("useFingerprint", use);
        editor.apply();
    }
}
