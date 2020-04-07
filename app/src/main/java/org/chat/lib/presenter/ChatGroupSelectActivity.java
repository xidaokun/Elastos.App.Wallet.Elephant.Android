package org.chat.lib.presenter;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;

import org.chat.lib.adapter.ChatGroupSelectAdapter;
import org.chat.lib.entity.ChatGroupSelectEntity;
import org.elastos.sdk.elephantwallet.contact.internal.ContactInterface;
import org.node.CarrierPeerNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatGroupSelectActivity extends BRActivity {

    private ListView mListView;
    private View mBackView;
    private View mFinishView;
    private String mFriendCode;
    private ChatGroupSelectAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_group_select_layout);

        mFriendCode = getIntent().getStringExtra("friendCodes");
        initView();
        initListener();
        initData();
    }

    private List<ChatGroupSelectEntity> mEntities = new ArrayList<>();
    private void initView() {
        mListView = findViewById(R.id.friends_list);
        mBackView = findViewById(R.id.back_button);
        mFinishView = findViewById(R.id.finish);
        mAdapter = new ChatGroupSelectAdapter(this, mEntities);
        mListView.setAdapter(mAdapter);

//        Intent intent = new Intent();
//        intent.putExtra("purpose", mLimitEdt.getText().toString());
//        setResult(RESULT_OK, intent);
//        finish();
    }

    private void initListener() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                boolean isSelected = mEntities.get(position).isSelected();
                mEntities.get(position).setSelected(!isSelected);
            }
        });

        mBackView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mFinishView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(ChatGroupSelectEntity entity : mEntities) {
                    List<String> friendCodes = new ArrayList<>();
                    friendCodes.add(entity.getFriendCode());
                    Collections.sort(friendCodes);
//                    ChatDataSource.getInstance(getApplicationContext()).updateMessage(mFriendCode, BRSQLiteHelper.CHAT_MESSAGE_FRIENDCODE, friendCodes.toString());

                    Intent intent = new Intent();
                    intent.putExtra("friendCodes", friendCodes.toString());
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
    }

    private void initData() {
        List<ContactInterface.FriendInfo> friendInfos = CarrierPeerNode.getInstance(this).getFriends();
        if(friendInfos==null || friendInfos.size()<=0) return;
        mEntities.clear();
        for(ContactInterface.FriendInfo friendInfo : friendInfos) {
            ChatGroupSelectEntity chatGroupSelectEntity = new ChatGroupSelectEntity();
            chatGroupSelectEntity.setName(friendInfo.humanCode);
            chatGroupSelectEntity.setFriendCode(friendInfo.humanCode);
            chatGroupSelectEntity.setSelected(false);
            mAdapter.notifyDataSetChanged();
        }
    }


}
