package org.chat.lib.push;

import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Date;

public class PushServer {

    private static final String APPKEY = "28357838";
    private static final String ACCESSKEYID = "LTAI4FsqAH9aMgMLTRz47vDT";
    private static final String ACCESSSECRET = "LLjjzzieTgX42Iz7ehY3tMtbCiCzMK";

    private static class ExtParameters {
        public String did;
        public String carrierAddr;
        public String nickname;
    }

    public static void setIosNotice(String did, String targetValue, String nickName, String carrierAddr) {
        ExtParameters extParameters = new ExtParameters();
        extParameters.did = did;
        extParameters.nickname = nickName;
        extParameters.carrierAddr = carrierAddr;

        PushRequest pushRequest = new PushRequest("LTAI4FsqAH9aMgMLTRz47vDT", "LLjjzzieTgX42Iz7ehY3tMtbCiCzMK");
        pushRequest.setAppKey("28435481");
        pushRequest.setTarget("ALIAS");
        Log.d("xidaokun_push", "targetValue:"+ targetValue);
        pushRequest.setTargetValue(targetValue);
        pushRequest.setPushType("NOTICE");
        pushRequest.setDeviceType("ALL");

        pushRequest.setTitle(nickName);
        pushRequest.setBody(did);

        pushRequest.setIOSSilentNotification(Boolean.TRUE);
        pushRequest.setIOSMutableContent(Boolean.TRUE);
        pushRequest.setIOSSilentNotification(Boolean.TRUE);
        pushRequest.setIOSRemind(Boolean.TRUE);
        pushRequest.setStoreOffline(Boolean.TRUE);
        pushRequest.setIOSApnsEnv("DEV");

        pushRequest.setiOSExtParameters(new Gson().toJson(extParameters));

        String expireTime = Util.getISO8601Time(new Date(System.currentTimeMillis() + 12 * 3600 * 1000)); // 12小时后消息失效, 不会再发送
        pushRequest.setExpireTime(expireTime);
        pushRequest.setStoreOffline(true); // 离线消息是否保存,若保存, 在推送时候，用户即使不在线，下一次上线则会收到

        pushRequest.asyncExecute(new PushRequest.PushCallback() {
            @Override
            public void onFailure(PushRequest request, IOException e) {
                Log.d("xidaokun_push", "failed");
            }

            @Override
            public void onResponse(PushRequest request, PushResponse response) {
                Log.d("xidaokun_push", "response:");
            }
        });
    }

    public static void sendNotice(String did, String targetValue, String nickName, String carrierAddr) {

        ExtParameters extParameters = new ExtParameters();
        extParameters.did = did;
        extParameters.nickname = nickName;
        extParameters.carrierAddr = carrierAddr;

        PushRequest pushRequest = new PushRequest(ACCESSKEYID, ACCESSSECRET);
        pushRequest.setAppKey(APPKEY);
        pushRequest.setTarget("ALIAS");
        Log.d("xidaokun_push", "targetValue:"+ targetValue);
        pushRequest.setTargetValue(targetValue);
        pushRequest.setPushType("NOTICE");
        pushRequest.setDeviceType("ALL");

        pushRequest.setTitle(nickName);
        pushRequest.setBody(did);

        pushRequest.setAndroidNotifyType("BOTH");//通知的提醒方式 "VIBRATE" : 震动 "SOUND" : 声音 "BOTH" : 声音和震动 NONE : 静音
        pushRequest.setAndroidOpenType("APPLICATION"); //点击通知后动作 "APPLICATION" : 打开应用 "ACTIVITY" : 打开AndroidActivity "URL" : 打开URL "NONE" : 无跳转

        pushRequest.setAndroidExtParameters(new Gson().toJson(extParameters));

        pushRequest.setAndroidNotificationChannel("1");

        //辅助弹窗设置
        pushRequest.setAndroidPopupActivity("org.chat.lib.presenter.NewFriendListActivity");
        pushRequest.setAndroidPopupTitle("wrapper title");
        pushRequest.setAndroidPopupBody("wrapper body");

        String expireTime = Util.getISO8601Time(new Date(System.currentTimeMillis() + 12 * 3600 * 1000)); // 12小时后消息失效, 不会再发送
        pushRequest.setExpireTime(expireTime);
        pushRequest.setStoreOffline(true); // 离线消息是否保存,若保存, 在推送时候，用户即使不在线，下一次上线则会收到
        pushRequest.asyncExecute(new PushRequest.PushCallback() {
            @Override
            public void onFailure(PushRequest request, IOException e) {
                Log.d("xidaokun_push", "failed");
            }

            @Override
            public void onResponse(PushRequest request, PushResponse response) {
                Log.d("xidaokun_push", "response:");
            }
        });

    }

}
