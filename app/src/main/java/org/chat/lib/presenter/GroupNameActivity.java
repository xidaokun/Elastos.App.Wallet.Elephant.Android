package org.chat.lib.presenter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;

import org.elastos.sdk.elephantwallet.contact.Contact;
import org.node.CarrierPeerNode;

public class GroupNameActivity extends BRActivity {

    private ImageButton mBackBtn;
    private TextView mSaveBtn;
    private EditText mGroupEdit;
    private TextView mCleanTv;

    private String mGroupId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_name_layout);

        mGroupId = getIntent().getStringExtra("friendCode");
        initView();
        initListener();
        initData();
    }

    private void initView() {
        mBackBtn = findViewById(R.id.back_button);
        mSaveBtn = findViewById(R.id.save_btn);
        mGroupEdit = findViewById(R.id.group_name_edit);
        mCleanTv = findViewById(R.id.did_nickname_clean);
    }

    private void initListener() {
        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!StringUtil.isNullOrEmpty(mGroupId)) {
                    String groupName = mGroupEdit.getText().toString();
                    Log.d("xidaokun", "CarrierPeerNode#mSaveBtn#groupName:"+ groupName);
                    CarrierPeerNode.getInstance(GroupNameActivity.this).setGroupFriendInfo(mGroupId, Contact.HumanInfo.Item.Nickname, groupName);
                    UiUtils.startChatDetailActivity(GroupNameActivity.this, mGroupId, BRConstants.CHAT_GROUP_TYPE, groupName);
                    finish();
                }
            }
        });

        mCleanTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGroupEdit.setText("");
            }
        });
    }

    private void initData() {
        joinGroup(mGroupId);
    }

    private void joinGroup(final String friendCode) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                CarrierPeerNode.getInstance(GroupNameActivity.this).addGroupFriend(friendCode);
            }
        });
    }
}
