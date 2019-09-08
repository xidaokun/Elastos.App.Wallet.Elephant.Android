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

    public static boolean getRecoverNeedRestart(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("recoverRestart", false);
    }

    public static void putRecoverNeedRestart(Context activity, boolean restart) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("recoverRestart", restart);
        editor.apply();
    }

    public static boolean getIsRecover(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("isRecover", false);
    }

    public static void putIsRecover(Context activity, boolean recover) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isRecover", recover);
        editor.apply();
    }

    public static String getRecoverWalletName(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("recoverWalletName", "");
    }

    public static void putRecoverWalletName(Context activity, String walletName) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("recoverWalletName", walletName);
        editor.apply();
    }
}
