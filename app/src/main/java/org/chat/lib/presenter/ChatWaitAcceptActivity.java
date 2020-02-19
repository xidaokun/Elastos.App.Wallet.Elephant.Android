package org.chat.lib.presenter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.alibaba.sdk.android.push.AndroidPopupActivity;
import com.breadwallet.R;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.util.BRConstants;

import org.chat.lib.adapter.NewFriendAdapter;
import org.chat.lib.entity.NewFriendBean;
import org.chat.lib.source.ChatDataSource;
import org.greenrobot.eventbus.EventBus;
import org.node.CarrierPeerNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatWaitAcceptActivity extends AndroidPopupActivity implements NewFriendAdapter.OnItemListener {

    private ListView mListView;
    private NewFriendAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_accept_layout);

        initView();
        initListener();
        initData();
    }

    private List<NewFriendBean> mWaitAcceptBeans = new ArrayList<>();
    private void initView() {
        mListView = findViewById(R.id.new_friends_lv);
        mAdapter = new NewFriendAdapter(this, mWaitAcceptBeans);
        mListView.setAdapter(mAdapter);
    }

    private void initData() {
        mWaitAcceptBeans.clear();
        List<NewFriendBean> waitAcceptBeans = ChatDataSource.getInstance(this).getAllNewFriends();
        mWaitAcceptBeans.addAll(waitAcceptBeans);
        mAdapter.notifyDataSetChanged();
    }

    private void initListener() {
        mAdapter.setOnItemListener(this);
    }

    @Override
    public void accept(View view, int position) {
        String friendCode = mWaitAcceptBeans.get(position).friendCode;
        CarrierPeerNode.getInstance(this).acceptFriend(friendCode, BRConstants.CHAT_SINGLE_TYPE);
        ChatDataSource.getInstance(this).updateAcceptState(friendCode, true);
        mWaitAcceptBeans.get(position).hasAccept = true;
        mAdapter.notifyDataSetChanged();
        EventBus.getDefault().post(friendCode);
    }

    @Override
    public void onItemClick(View view, int position) {

    }

    @Override
    protected void onSysNoticeOpened(String s, String s1, Map<String, String> map) {
        Log.d("xidaokun_push", "onSysNoticeOpened, title: " + s + ", summary: " + s1 + ", extraMap: " + map);
    }
}
