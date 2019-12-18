package org.common.lib;

import android.app.Application;
import android.content.Context;

public class BaseApplication extends Application {

    private static Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        //LeakCanary.install(this);
    }

    public static Context getContext(){
        return mContext;
    }
}
