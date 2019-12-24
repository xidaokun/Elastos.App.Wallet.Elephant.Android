package com.breadwallet.presenter.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.breadwallet.R;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.util.StringUtil;

import org.chat.lib.adapter.ChatPagerAdapter;
import org.chat.lib.presenter.BaseFragment;
import org.chat.lib.presenter.FragmentChatFriends;
import org.chat.lib.presenter.FragmentChatMessage;

import java.util.ArrayList;
import java.util.List;

import app.elaphant.sdk.peernode.PeerNode;
import app.elaphant.sdk.peernode.PeerNodeListener;

import static android.support.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread;

public class FragmentChat extends Fragment implements View.OnClickListener {
    private static final String TAG = FragmentChat.class.getSimpleName() + "_log";

    private ViewPager mViewPager;
    private TabLayout mTabLayout;

    private PeerNode mPeerNode;

    private View mAddFriendView;
    private View mAddPopView;
    private View mAddDidView;
    private View mAddDeviceView;
    private View mJoinGroupView;

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
        initPeerNode();
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
        mAddFriendView = view.findViewById(R.id.chat_add_icon);
        mViewPager = view.findViewById(R.id.viewpager);
        mAddPopView = view.findViewById(R.id.chat_add_pop_layout);
        mAddDidView = view.findViewById(R.id.chat_add_by_did);
        mAddDeviceView = view.findViewById(R.id.chat_add_by_device);
        mJoinGroupView = view.findViewById(R.id.chat_join_group);

        List<BaseFragment> fragments = new ArrayList<>();
        fragments.add(FragmentChatMessage.newInstance(getContext().getString(R.string.My_chat_tab_message_title)));
        fragments.add(FragmentChatFriends.newInstance(getContext().getString(R.string.My_chat_tab_friends_title)));
        mViewPager.setAdapter(new ChatPagerAdapter(getActivity().getSupportFragmentManager(), fragments));
        mTabLayout.setupWithViewPager(mViewPager);
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
    }

    private void initPeerNode() {
        mPeerNode = PeerNode.getInstance(getContext().getFilesDir().getAbsolutePath(),
                Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID));
        mPeerNode.setListener(new PeerNodeListener.Listener() {

            @Override
            public byte[] onAcquire(org.elastos.sdk.elephantwallet.contact.Contact.Listener.AcquireArgs request) {
                byte[] response = null;
                switch (request.type) {
                    case PublicKey:
//                        response = mPublicKey.getBytes();
                        break;
                    case EncryptData:
                        response = request.data;
                        break;
                    case DecryptData:
                        response = request.data;
                        break;
                    case DidPropAppId:
                        break;
                    case DidAgentAuthHeader:
//                        response = getAgentAuthHeader();
                        break;
                    case SignData:
//                        response = signData(request.data);
                        break;
                    default:
                        throw new RuntimeException("Unprocessed request: " + request);
                }
                return new byte[0];
            }

            @Override
            public void onError(int errCode, String errStr, String ext) {

            }
        });

        mPeerNode.start();
    }

    public void setValue(String value) {
        if(StringUtil.isNullOrEmpty(value)) return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mViewPager.setCurrentItem(1);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPeerNode.stop();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.chat_add_by_did:
                UiUtils.startAddFriendActivity(getActivity());
                break;
            case R.id.chat_add_by_device:
                UiUtils.startAddFriendActivity(getActivity());
                break;
            case R.id.chat_join_group:
                UiUtils.startAddFriendActivity(getActivity());
                break;
            default:
                break;
        }
        mAddPopView.setVisibility(View.GONE);
    }
}
