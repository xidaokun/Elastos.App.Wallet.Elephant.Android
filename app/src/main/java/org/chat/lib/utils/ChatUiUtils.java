package org.chat.lib.utils;

import android.content.Context;
import android.content.Intent;

import org.moment.lib.activity.MomentActivity;

public class ChatUiUtils {

    public static void startMomentActivity(Context context) {
        Intent intent = new Intent(context, MomentActivity.class);
        context.startActivity(intent);
    }
}
