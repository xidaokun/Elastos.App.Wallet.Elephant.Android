package org.moment.lib;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import java.io.File;


public class MyApplication extends Application {
	// 默认存放图片的路径
	public final static String DEFAULT_SAVE_IMAGE_PATH = Environment.getExternalStorageDirectory() + File.separator + "CircleDemo" + File.separator + "Images"
				+ File.separator;

	private static Context mContext;
	@Override
	public void onCreate() {
		super.onCreate();
		mContext = getApplicationContext();
		//LeakCanary.install(this);
//        QPManager.getInstance(getApplicationContext()).initRecord();
	}

	public static Context getContext(){
		return mContext;
	}


}
