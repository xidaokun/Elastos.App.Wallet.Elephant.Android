package org.chat.lib.presenter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;

import org.chat.lib.adapter.NewFriendAdapter;
import org.chat.lib.entity.NewFriendBean;
import org.chat.lib.source.ChatDataSource;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.node.CarrierPeerNode;

import java.util.ArrayList;
import java.util.List;

public class NewFriendListActivity extends BRActivity implements NewFriendAdapter.OnItemListener {

    private ListView mListView;
    private NewFriendAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_accept_layout);

        initView();
        initListener();
        refreshData();

        EventBus.getDefault().register(this);
    }

    private List<NewFriendBean> mWaitAcceptBeans = new ArrayList<>();
    private void initView() {
        mListView = findViewById(R.id.new_friends_lv);
        mAdapter = new NewFriendAdapter(this, mWaitAcceptBeans);
        mListView.setAdapter(mAdapter);
    }

    private void refreshData() {
        mWaitAcceptBeans.clear();
        List<NewFriendBean> waitAcceptBeans = ChatDataSource.getInstance(this).getAllNewFriends();
        mWaitAcceptBeans.addAll(waitAcceptBeans);
        mAdapter.notifyDataSetChanged();
    }

    private void initListener() {
        mAdapter.setOnItemListener(this);
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void accept(View view, final int position) {
        String carrierAddr = mWaitAcceptBeans.get(position).carrierAddr;
        String did = mWaitAcceptBeans.get(position).did;
        if(StringUtil.isNullOrEmpty(carrierAddr) && StringUtil.isNullOrEmpty(did)) return;
        final String friendCode = StringUtil.isNullOrEmpty(carrierAddr)?did:carrierAddr;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final int ret = CarrierPeerNode.getInstance(NewFriendListActivity.this).acceptFriend(friendCode, BRConstants.CHAT_SINGLE_TYPE);
                if(0==ret) {
                    ChatDataSource.getInstance(NewFriendListActivity.this).updateAcceptState(friendCode, BRConstants.ACCEPTED);
                    mWaitAcceptBeans.get(position).acceptStatus = BRConstants.ACCEPTED;
                    BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.notifyDataSetChanged();
                            EventBus.getDefault().post(friendCode);
                        }
                    });
                } else {
                    BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(NewFriendListActivity.this, "CarrierPeerNode accept ret:" + ret, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onItemClick(View view, int position) {

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveAddAcceptEvent(CarrierPeerNode.FriendStatusInfo friendStatusInfo) {
        ChatDataSource.getInstance(this).updateAcceptState(friendStatusInfo.humanCode, BRConstants.ACCEPTED);
        refreshData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
