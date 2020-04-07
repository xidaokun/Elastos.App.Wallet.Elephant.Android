package org.chat.lib.presenter;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.breadwallet.R;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.elastos.jni.utils.SchemeStringUtils;
import com.google.gson.Gson;

import org.chat.lib.adapter.ChatMessageAdapter;
import org.chat.lib.entity.ChatMsgEntity;
import org.chat.lib.entity.MessageCacheBean;
import org.chat.lib.entity.MessageItemBean;
import org.chat.lib.source.ChatDataSource;
import org.chat.lib.utils.Utils;
import org.elastos.sdk.elephantwallet.contact.internal.ContactInterface;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.node.CarrierPeerNode;

import java.util.ArrayList;
import java.util.List;

public class FragmentChatMessage extends BaseFragment {

    private static final String TAG = FragmentChatMessage.class.getSimpleName() + "_log";

    private ListView mListView;

    public static FragmentChatMessage newInstance(String title) {
        FragmentChatMessage f = new FragmentChatMessage();
        f.setTitle(title);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat_message, container, false);
        initView(rootView);
        initListener(rootView);
        EventBus.getDefault().register(this);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    public static class RefreshMessage {

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void friendStatusChange(CarrierPeerNode.FriendStatusInfo friendStatusInfo) {
        refreshData();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveMessageEvent(RefreshMessage refreshMessage) {
        refreshData();
    }

    private void refreshData() {
        entities.clear();
        List<MessageItemBean> messageItemBeans = ChatDataSource.getInstance(getContext()).getMessageItemInfos();
        for(MessageItemBean messageCacheBean : messageItemBeans) {
            String friendCode  = messageCacheBean.friendCode;
            List<MessageCacheBean> allMessageCacheBeans  = ChatDataSource.getInstance(getContext()).getMessage(BRSQLiteHelper.CHAT_MESSAGE_FRIENDCODE + " = ? ", new String[]{friendCode});
            List<MessageCacheBean> hasNotReadCacheBeans = ChatDataSource.getInstance(getContext()).getMessage(BRSQLiteHelper.CHAT_MESSAGE_FRIENDCODE + " = ? AND " + BRSQLiteHelper.CHAT_MESSAGE_HAS_READ + " = ? ", new String[]{friendCode, String.valueOf(0)});

            if(null != allMessageCacheBeans) {
                int count = allMessageCacheBeans.size();
                if(count > 0) {
                    Log.d("xidaokun", "FragmentChatMessage#refreshData#\nallMessageCacheBeans:"+ new Gson().toJson(allMessageCacheBeans));
                    Log.d("xidaokun", "FragmentChatMessage#refreshData#\nhasNotReadCacheBeans:"+ new Gson().toJson(hasNotReadCacheBeans));

                    MessageCacheBean lastBean = allMessageCacheBeans.get(count - 1);
                    ChatMsgEntity entity = new ChatMsgEntity();
                    //TODO daokun.xi
                    String nickname = ChatDataSource.getInstance(getContext()).getNickname(lastBean.MessageFriendCode);
                    entity.setName(SchemeStringUtils.isNullOrEmpty(nickname)?lastBean.MessageFriendCode:nickname);
                    entity.setMessage(lastBean.MessageContent);
                    entity.setTimeStamp(lastBean.MessageTimestamp);
                    entity.setFriendCode(lastBean.MessageFriendCode);
                    entity.setType(lastBean.MessageType);
                    ContactInterface.FriendInfo friendInfo = CarrierPeerNode.getInstance(getContext()).getFriendInfo(lastBean.MessageFriendCode);
                    entity.setOnline((friendInfo!=null&&(friendInfo.status==ContactInterface.Status.Online)));
                    entity.setCount((null!=hasNotReadCacheBeans && hasNotReadCacheBeans.size()>0)? hasNotReadCacheBeans.size() : 0);

                    entities.add(entity);
                }
                getActivity().getWindow().getDecorView().post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }

        }
    }

    List<ChatMsgEntity> entities = new ArrayList<>();
    ChatMessageAdapter mAdapter;
    private void initView(View view) {
        mListView = view.findViewById(R.id.side_delete_listview);
        mAdapter = new ChatMessageAdapter(getContext(), entities);
        mListView.setAdapter(mAdapter);
    }

    private void initListener(View rootView) {
        mAdapter.setListener(new ChatMessageAdapter.OnItemListener() {
            @Override
            public void onLongPress(View view, int position, float x, float y) {
//                showDeletePop(view, (int) x, (int) y, position);
            }

            @Override
            public void onMove(View view, int position) {

            }

            @Override
            public void onClick(View view, final int position) {
                String friendCode = entities.get(position).getFriendCode();
                String type = entities.get(position).getType();
                String nickName = entities.get(position).getName();
                UiUtils.startChatDetailActivity(getContext(), friendCode, type, nickName);
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        ChatMsgEntity chatMsgEntity = entities.get(position);
                        String friendCode = chatMsgEntity.getFriendCode();
                        List<MessageCacheBean> hasNotReadCacheBeans = ChatDataSource.getInstance(getContext()).getMessage(BRSQLiteHelper.CHAT_MESSAGE_FRIENDCODE + " = ? AND " + BRSQLiteHelper.CHAT_MESSAGE_HAS_READ + " = ? ", new String[]{friendCode, String.valueOf(0)});
                        ChatDataSource.getInstance(getContext()).updateMessage(hasNotReadCacheBeans, true);
//                        refreshData();
                    }
                });
            }
        });
    }

    PopupWindow popupWindow = null;
    private void showDeletePop(View headview, int x, int y, final int position) {
        if(popupWindow == null) {
            View view = getLayoutInflater().inflate(R.layout.chat_message_pop_layout, null);
            popupWindow = new PopupWindow(view, Utils.dp2px(getContext(), 100), Utils.dp2px(getContext(), 120), true);
            popupWindow.setOutsideTouchable(true);
            view.findViewById(R.id.has_read_tv).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String friendCode = entities.get(position).getFriendCode();
                    ChatDataSource.getInstance(getContext()).updateHasRead(friendCode, true);
                    entities.get(position).setCount(0);
                    mAdapter.notifyDataSetChanged();
                    popupWindow.dismiss();
                }
            });

            view.findViewById(R.id.roof_placement_tv).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            view.findViewById(R.id.delete_message_tv).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String friendCode = entities.get(position).getFriendCode();
                    ChatDataSource.getInstance(getContext()).deleteMessage(friendCode);
                    ChatDataSource.getInstance(getContext()).deleteMessageItemInfo(friendCode);
                    entities.remove(position);
                    mAdapter.notifyDataSetChanged();
                    popupWindow.dismiss();
                }
            });
        }

        if (popupWindow.isShowing()) {
            popupWindow.dismiss();
        } else {
            Log.d("xidaokun", "FragmentChatMessage#showDeletePop#x:"+x+" #y"+y);
            int headViewH = headview.getHeight();
            Log.d("xidaokun", "FragmentChatMessage#showDeletePop#headViewH:"+headViewH);
            popupWindow.showAsDropDown(headview, x, y - headViewH);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
