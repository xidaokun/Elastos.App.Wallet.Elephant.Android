package org.chat.lib.push;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import com.alibaba.sdk.android.push.CloudPushService;
import com.alibaba.sdk.android.push.CommonCallback;
import com.alibaba.sdk.android.push.noonesdk.PushServiceFactory;
import com.breadwallet.BuildConfig;
import com.breadwallet.tools.threads.executor.BRExecutor;

public class PushClient {

    private static final String TAG = PushClient.class.getSimpleName();

    private static PushClient mInstance;

    private CloudPushService mPushService;

    private PushClient() {
    }

    public static PushClient getInstance() {
        if(null == mInstance) {
            mInstance = new PushClient();
        }

        return mInstance;
    }

    /**
     * 初始化云推送通道
     */
    public void initCloudChannel(Context applicationContext) {
        initNotify(applicationContext);

        PushServiceFactory.init(applicationContext);
        mPushService = PushServiceFactory.getCloudPushService();
        Log.d("aliConfig", "ALI_AR_APPKEY:"+BuildConfig.ALI_AR_APPKEY+"  ALI_AR_APPSECRET:"+BuildConfig.ALI_AR_APPSECRET);
//        mPushService.register(applicationContext, new CommonCallback() {
//            @Override
//            public void onSuccess(String s) {
//                Log.d("aliConfig", "onSuccess:"+s);
//            }
//
//            @Override
//            public void onFailed(String s, String s1) {
//                Log.d("aliConfig", "onFailed s:" + s + " s1:" + s1);
//            }
//        });
        mPushService.register(applicationContext, BuildConfig.ALI_AR_APPKEY, BuildConfig.ALI_AR_APPSECRET, new CommonCallback() {
            @Override
            public void onSuccess(String s) {
                Log.d("aliConfig", "onSuccess:"+s);
            }

            @Override
            public void onFailed(String s, String s1) {
                Log.d("aliConfig", "onFailed s:" + s + " s1:" + s1);
            }
        });

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                removeAlias();
            }
        });
    }

    private void initNotify(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            // 通知渠道的id
            String id = "1";
            // 用户可以看到的通知渠道的名字.
            CharSequence name = "notification channel";
            // 用户可以看到的通知渠道的描述
            String description = "notification description";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(id, name, importance);
            // 配置通知渠道的属性
            mChannel.setDescription(description);
            // 设置通知出现时的闪灯（如果 android 设备支持的话）
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            // 设置通知出现时的震动（如果 android 设备支持的话）
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            //最后在notificationmanager中创建该通知渠道
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    public void bindAccount(String account, final CommonCallback commonCallback) {
        if(null == mPushService) return;
        mPushService.bindAccount(account, new CommonCallback() {
            @Override
            public void onSuccess(String s) {
                Log.d("xidaokun_push", "bindAccount success s:"+s);
                if(null != commonCallback) commonCallback.onSuccess(s);
            }

            @Override
            public void onFailed(String s, String s1) {
                Log.d("xidaokun_push", "bindAccount failed s:"+s + " s1:"+s1);
                if(null != commonCallback) commonCallback.onFailed(s, s1);
            }
        });
    }

    public void removeAlias() {
        if(null == mPushService) return;
        mPushService.removeAlias(null, new CommonCallback() {
            @Override
            public void onSuccess(String s) {
                Log.d("xidaokun_push", "removeAlias onSuccess s:"+s);
            }

            @Override
            public void onFailed(String s, String s1) {
                Log.d("xidaokun_push", "removeAlias failed s:"+s+" s1:"+s1);
            }
        });
    }

    public void bindAlias(String alias, final CommonCallback commonCallback) {
        if(null == mPushService) return;
        mPushService.listAliases(new CommonCallback() {
            @Override
            public void onSuccess(String s) {
                Log.d("xidaokun_push", "bindAlias success s:"+s);
            }

            @Override
            public void onFailed(String s, String s1) {
                Log.d("xidaokun_push", "bindAlias failed s:"+s+" s1:"+s1);
            }
        });

        mPushService.addAlias(alias, new CommonCallback() {
            @Override
            public void onSuccess(String s) {
                Log.d("xidaokun_push", "bindAlias success s:"+s);
                if(null != commonCallback) commonCallback.onSuccess(s);
            }

            @Override
            public void onFailed(String s, String s1) {
                Log.d("xidaokun_push", "bindAlias failed s:"+s+" s1:"+s1);
                if(null != commonCallback) commonCallback.onFailed(s, s1);
            }
        });
    }

}
