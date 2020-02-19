package org.chat.lib.presenter;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ListView;

import com.breadwallet.R;
import com.breadwallet.tools.util.BRConstants;

import org.chat.lib.adapter.NewFriendAdapter;
import org.chat.lib.entity.WaitAcceptBean;
import org.chat.lib.source.ChatDataSource;
import org.node.CarrierPeerNode;

import java.util.ArrayList;
import java.util.List;

public class ChatWaitAcceptActivity extends Activity implements NewFriendAdapter.OnItemListener {

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

    private List<WaitAcceptBean> mWaitAcceptBeans = new ArrayList<>();
    private void initView() {
        mListView = findViewById(R.id.new_friends_lv);
        mAdapter = new NewFriendAdapter(this, mWaitAcceptBeans);
        mListView.setAdapter(mAdapter);
    }

    private void initData() {
        mWaitAcceptBeans.clear();
        List<WaitAcceptBean> waitAcceptBeans = ChatDataSource.getInstance(this).getWaitAcceptFriends();
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
    }

    @Override
    public void onItemClick(View view, int position) {

    }

}
