package org.chat.lib.presenter;

import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.wallet.wallets.ela.WalletElaManager;

import org.chat.lib.adapter.FriendsAdapter;
import org.chat.lib.entity.ContactEntity;
import org.chat.lib.utils.ChatUiUtils;
import org.chat.lib.widget.DividerItemDecoration;
import org.chat.lib.widget.IndexBar;
import org.chat.lib.widget.SuspensionDecoration;
import org.elastos.sdk.elephantwallet.contact.Utils;
import org.elastos.sdk.elephantwallet.contact.internal.ContactInterface;
import org.elastos.sdk.keypair.ElastosKeypair;

import java.util.ArrayList;
import java.util.List;

import app.elaphant.sdk.peernode.PeerNode;
import app.elaphant.sdk.peernode.PeerNodeListener;

public class FragmentChatFriends extends BaseFragment {
    private static final String TAG = FragmentChatFriends.class.getSimpleName() + "_log";

    private static final String INDEX_STRING_TOP = "\uD83D\uDD0E";

    private RecyclerView mRecyclerView;
    private IndexBar mIndexBar;
    private FriendsAdapter mAdapter;
    private List<ContactEntity> mDatas = new ArrayList<>();
    private SuspensionDecoration mDecoration;
    private LinearLayoutManager mManager;
    private TextView mSideHintTv;

    private PeerNode mPeerNode;

    public static FragmentChatFriends newInstance(String title) {
        FragmentChatFriends f = new FragmentChatFriends();
        f.setTitle(title);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat_friends, container, false);
        initView(rootView);

        //mock data
        initDatas(getResources().getStringArray(R.array.provinces));
        initListener();
        mPeerNode = PeerNode.getInstance();
        return rootView;
    }

    private void initView(View rootView) {
        mSideHintTv = rootView.findViewById(R.id.side_bar_hint);
        mRecyclerView = rootView.findViewById(R.id.friends_rv);
        mRecyclerView.setLayoutManager(mManager = new LinearLayoutManager(getContext()));
        mIndexBar = rootView.findViewById(R.id.indexBar);
        mIndexBar.setmPressedShowTextView(mSideHintTv)
                .setNeedRealIndex(true)
                .setmLayoutManager(mManager);

        mAdapter = new FriendsAdapter(getContext(), mDatas);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(mDecoration = new SuspensionDecoration(getContext(), mDatas));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));
    }

    private void initListener() {
        mAdapter.setOnItemClickListener(new FriendsAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(View view, int position) {
                ChatUiUtils.startMomentActivity(getContext());
            }
        });
    }

    public void addFriend(String value) {
        getActivity().getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                //TODO daokun.xi
                int ret = mPeerNode.addFriend("iZmEF8QifH1tUXnqyqnS2KdhfqZ3aiXxYa", "");
                List<ContactInterface.FriendInfo> friendInfos = mPeerNode.listFriendInfo();
                if(null == friendInfos) return;
                List<ContactEntity> contacts = new ArrayList<>();
                contacts.clear();
                for(ContactInterface.FriendInfo info : friendInfos) {
                    ContactEntity contactEntity = new ContactEntity();
                    contactEntity.setContact(info.did);
                    contactEntity.setTokenAddress(info.elaAddress);

                    contacts.add(contactEntity);
                }
                mDatas.clear();
                mDatas.addAll(contacts);

                mIndexBar.setmSourceDatas(mDatas)
                        .invalidate();
                mDecoration.setmDatas(mDatas);

                mAdapter.setDatas(mDatas);
                mAdapter.notifyDataSetChanged();
            }
        }, 500);
    }


    private void initDatas(final String[] data) {
//        getActivity().getWindow().getDecorView().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mDatas = new ArrayList<>();
//                mDatas.add((ContactEntity) new ContactEntity("新的朋友").setTop(true).setBaseIndexTag(INDEX_STRING_TOP));
//                mDatas.add((ContactEntity) new ContactEntity("群聊").setTop(true).setBaseIndexTag(INDEX_STRING_TOP));
//                mDatas.add((ContactEntity) new ContactEntity("标签").setTop(true).setBaseIndexTag(INDEX_STRING_TOP));
//                mDatas.add((ContactEntity) new ContactEntity("公众号").setTop(true).setBaseIndexTag(INDEX_STRING_TOP));
//                for (int i = 0; i < data.length; i++) {
//                    ContactEntity ContactEntity = new ContactEntity();
//                    ContactEntity.setContact(data[i]);
//                    mDatas.add(ContactEntity);
//                }
//                mAdapter.setDatas(mDatas);
//                mAdapter.notifyDataSetChanged();
//
//                mIndexBar.setmSourceDatas(mDatas)
//                        .invalidate();
//                mDecoration.setmDatas(mDatas);
//            }
//        }, 500);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPeerNode.stop();
    }
}
