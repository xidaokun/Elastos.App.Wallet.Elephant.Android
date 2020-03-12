package org.chat.lib.presenter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.util.StringUtil;

import org.chat.lib.source.ChatDataSource;
import org.elastos.sdk.elephantwallet.contact.Contact;
import org.elastos.sdk.elephantwallet.contact.internal.ContactInterface;
import org.node.CarrierPeerNode;

import java.util.List;
import java.util.Map;

public class FriendProfileEditActivity extends BRActivity {

    private TextView mTitleTv;
    private TextView mSaveTv;
    private EditText mNicknameEdt;
    private TextView mNickCleanTv;
    private String mFriendCode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profile_layout);

        mFriendCode = getIntent().getStringExtra("did");

        initView();
        initListener();
    }

    private void initView() {
        mTitleTv = findViewById(R.id.title);
        mNicknameEdt = findViewById(R.id.did_nickname_edt);
        mNickCleanTv = findViewById(R.id.did_nickname_clean);
        mSaveTv = findViewById(R.id.close_button);

        mTitleTv.setText(R.string.My_Profile_Nickname);

        String nickname = getIntent().getStringExtra("nickname");
        mNicknameEdt.setText(nickname);
    }

    private void initListener() {
        mNickCleanTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNicknameEdt.setText("");
            }
        });

        mSaveTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nickname = mNicknameEdt.getText().toString();
                if(StringUtil.isNullOrEmpty(nickname)) {
                    Toast.makeText(FriendProfileEditActivity.this, getResources().getString(R.string.My_chat_edit_nickname_empty_toast), Toast.LENGTH_SHORT).show();
                    return;
                }

                List<ContactInterface.FriendInfo> friendInfos = CarrierPeerNode.getInstance(FriendProfileEditActivity.this).getFriends();
                Map<String, String> friendsNickname = ChatDataSource.getInstance(FriendProfileEditActivity.this).getAllFriendName();

                String friendCode = null;
                for (ContactInterface.FriendInfo info : friendInfos) {
                    friendCode = getFriendCode(friendsNickname, mFriendCode, info.boundCarrierArray);
                }
                if(!StringUtil.isNullOrEmpty(friendCode)) {
                    ChatDataSource.getInstance(FriendProfileEditActivity.this).updateFriendName(friendCode, nickname);
                    finish();
                } else {
                    Toast.makeText(FriendProfileEditActivity.this, "can not find did", Toast.LENGTH_SHORT).show();
                }
            }
        });
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private String getFriendCode(Map<String,String> friends, String humancode, List<Contact.HumanInfo.CarrierInfo> carrierInfos) {
        String nickname = friends.get(humancode);
        if(!StringUtil.isNullOrEmpty(nickname)) {
            return humancode;
        } else {
            for(Contact.HumanInfo.CarrierInfo carrierInfo : carrierInfos) {
                nickname = friends.get(carrierInfo.usrAddr);
                if(!StringUtil.isNullOrEmpty(nickname)) return carrierInfo.usrAddr;
            }
        }

        return null;
    }

}
