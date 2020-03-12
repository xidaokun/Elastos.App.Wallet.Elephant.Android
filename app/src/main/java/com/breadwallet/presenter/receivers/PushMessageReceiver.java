package com.breadwallet.presenter.receivers;

import android.content.Context;
import android.util.Log;

import com.alibaba.sdk.android.push.MessageReceiver;
import com.alibaba.sdk.android.push.notification.CPushMessage;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.util.BRConstants;

import org.chat.lib.entity.NewFriendBean;
import org.chat.lib.source.ChatDataSource;
import org.greenrobot.eventbus.EventBus;

import java.util.Map;

public class PushMessageReceiver extends MessageReceiver {
    // 消息接收部分的LOG_TAG
    public static final String REC_TAG = "receiver";

    @Override
    public void onNotification(Context context, String title, String summary, Map<String, String> extraMap) {
        // TODO 处理推送通知
        Log.d("xidaokun_push", "Receive notification, title: " + title + ", summary: " + summary + ", extraMap: " + extraMap);

        NewFriendBean waitAcceptBean = new NewFriendBean();
        waitAcceptBean.nickName = extraMap.get("nickname");
        waitAcceptBean.did = extraMap.get("did");
        waitAcceptBean.carrierAddr = extraMap.get("carrierAddr");
        waitAcceptBean.acceptStatus = BRConstants.RECEIVE_ACCEPT;
        waitAcceptBean.timeStamp = System.currentTimeMillis();
        ChatDataSource.getInstance(context).cacheWaitAcceptFriend(waitAcceptBean);

        EventBus.getDefault().post(summary);
    }
    @Override
    public void onMessage(Context context, CPushMessage cPushMessage) {
        Log.d("xidaokun_push", "onMessage, messageId: " + cPushMessage.getMessageId() + ", title: " + cPushMessage.getTitle() + ", content:" + cPushMessage.getContent());
    }
    @Override
    public void onNotificationOpened(Context context, String title, String summary, String extraMap) {
        Log.d("xidaokun_push", "onNotificationOpened, title: " + title + ", summary: " + summary + ", extraMap:" + extraMap);
        UiUtils.startWaitAcceptActivity(context);
    }
    @Override
    protected void onNotificationClickedWithNoAction(Context context, String title, String summary, String extraMap) {
        Log.d("xidaokun_push", "onNotificationClickedWithNoAction, title: " + title + ", summary: " + summary + ", extraMap:" + extraMap);
    }
    @Override
    protected void onNotificationReceivedInApp(Context context, String title, String summary, Map<String, String> extraMap, int openType, String openActivity, String openUrl) {
        Log.d("xidaokun_push", "onNotificationReceivedInApp, title: " + title + ", summary: " + summary + ", extraMap:" + extraMap + ", openType:" + openType + ", openActivity:" + openActivity + ", openUrl:" + openUrl);
    }
    @Override
    protected void onNotificationRemoved(Context context, String messageId) {
        Log.d("xidaokun_push", "onNotificationRemoved");
    }
}
