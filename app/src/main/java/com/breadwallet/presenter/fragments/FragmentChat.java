package com.breadwallet.presenter.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.breadwallet.R;
import com.breadwallet.tools.animation.ElaphantDialogEdit;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;

import org.chat.lib.adapter.ChatPagerAdapter;
import org.chat.lib.presenter.BaseFragment;
import org.chat.lib.presenter.FragmentChatFriends;
import org.chat.lib.presenter.FragmentChatMessage;
import org.elastos.sdk.elephantwallet.contact.Contact;
import org.elastos.sdk.elephantwallet.contact.internal.ContactInterface;
import org.node.CarrierPeerNode;

import java.util.ArrayList;
import java.util.List;


public class FragmentChat extends Fragment implements View.OnClickListener {
    private static final String TAG = FragmentChat.class.getSimpleName() + "_log";

    private ViewPager mViewPager;
    private TabLayout mTabLayout;

    private View mPopParentView;
    private View mAddFriendView;
    private View mAddPopView;
    private View mAddDidView;
    private View mAddDeviceView;
    private View mJoinGroupView;
    private View mMyQrView;

    private FragmentChatFriends mChatFriendsFrg;

    public static FragmentChat newInstance(String text) {
        FragmentChat f = new FragmentChat();
        Bundle b = new Bundle();
        b.putString("text", text);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
        initView(rootView);
        initListener();
        return rootView;
    }

    private void initView(View view) {
        mTabLayout = view.findViewById(R.id.tab_layout);
        mPopParentView = view.findViewById(R.id.chat_add_pop_layout);
        mAddFriendView = view.findViewById(R.id.chat_add_icon);
        mViewPager = view.findViewById(R.id.viewpager);
        mAddPopView = view.findViewById(R.id.chat_add_pop_layout);
        mAddDidView = view.findViewById(R.id.chat_add_by_did);
        mAddDeviceView = view.findViewById(R.id.chat_add_by_device);
        mJoinGroupView = view.findViewById(R.id.chat_join_group);
        mMyQrView = view.findViewById(R.id.chat_my_qr);

        List<BaseFragment> fragments = new ArrayList<>();
        fragments.add(FragmentChatMessage.newInstance(getContext().getString(R.string.My_chat_tab_message_title)));
        fragments.add(mChatFriendsFrg = FragmentChatFriends.newInstance(getContext().getString(R.string.My_chat_tab_friends_title)));
        mViewPager.setAdapter(new ChatPagerAdapter(getActivity().getSupportFragmentManager(), fragments));
        mTabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    public void onResume() {
        super.onResume();
        ContactInterface.UserInfo userInfo = CarrierPeerNode.getInstance(getContext()).getUserInfo();
        if(userInfo == null) return;
        String nickName = userInfo.nickname;
        if(!StringUtil.isNullOrEmpty(nickName)) return;
        showNicknameDialog();
    }

    private void showNicknameDialog() {
        final ElaphantDialogEdit elaphantDialog = new ElaphantDialogEdit(getContext());
        elaphantDialog.setTitleStr("Set nickname to chat");
        elaphantDialog.setMessageStr("Input your nickname");
        elaphantDialog.setPositiveStr("Set Now");
        elaphantDialog.setNegativeStr("Cancel");
        elaphantDialog.setPositiveListener(new ElaphantDialogEdit.OnPositiveClickListener() {
            @Override
            public void onClick() {
                String nickName = elaphantDialog.getEditText();
                BRSharedPrefs.putNickname(getContext(), nickName);
                CarrierPeerNode.getInstance(getContext()).
                        setMyInfo(Contact.HumanInfo.Item.Nickname,
                                StringUtil.isNullOrEmpty(nickName)?"nickname":nickName);
                elaphantDialog.dismiss();
            }
        });
        elaphantDialog.setNegativeListener(new ElaphantDialogEdit.OnNegativeClickListener() {
            @Override
            public void onClick() {
                elaphantDialog.dismiss();
            }
        });
        elaphantDialog.show();
    }

    private void initListener() {
        mAddPopView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mAddFriendView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAddPopView.setVisibility(View.VISIBLE);
            }
        });
        mAddDidView.setOnClickListener(this);
        mAddDeviceView.setOnClickListener(this);
        mJoinGroupView.setOnClickListener(this);
        mMyQrView.setOnClickListener(this);
        mPopParentView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mPopParentView.setVisibility(View.GONE);
                return true;
            }
        });
    }

    public void selectFriendFragment(final String value) {
        if(StringUtil.isNullOrEmpty(value)) return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mViewPager.setCurrentItem(1);
                mChatFriendsFrg.addFriend(value);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.chat_add_by_did:
                UiUtils.startAddFriendActivity(getActivity(), BRConstants.CHAT_SINGLE_TYPE);
                break;
            case R.id.chat_add_by_device:
                UiUtils.startAddFriendActivity(getActivity(), BRConstants.CHAT_SINGLE_TYPE);
                break;
            case R.id.chat_join_group:
                UiUtils.startAddFriendActivity(getActivity(), BRConstants.CHAT_GROUP_TYPE);
                break;
            case R.id.chat_my_qr:
                UiUtils.startMyQrActivity(getActivity());
                break;
            default:
                break;
        }
        mAddPopView.setVisibility(View.GONE);
    }
}
