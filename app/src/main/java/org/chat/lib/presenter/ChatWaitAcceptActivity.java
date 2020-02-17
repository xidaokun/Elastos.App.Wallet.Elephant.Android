package org.chat.lib.presenter;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ListView;

import com.breadwallet.R;

import org.chat.lib.adapter.NewFriendAdapter;

public class ChatWaitAcceptActivity extends Activity {

    private ListView mListView;
    private NewFriendAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_accept_layout);

        initView();
        initListener();
    }


    private void initView() {
        mListView = findViewById(R.id.new_friends_lv);

    }

    private void initListener() {

    }
}
