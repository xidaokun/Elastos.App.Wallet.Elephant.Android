package org.chat.lib.presenter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;

import org.chat.lib.adapter.FriendsAdapter;
import org.chat.lib.entity.ContactEntity;
import org.chat.lib.entity.MessageInfo;
import org.chat.lib.utils.ChatUiUtils;
import org.chat.lib.utils.Constants;
import org.chat.lib.widget.DividerItemDecoration;
import org.chat.lib.widget.IndexBar;
import org.chat.lib.widget.SuspensionDecoration;
import org.elastos.sdk.elephantwallet.contact.Contact;
import org.elastos.sdk.elephantwallet.contact.internal.ContactInterface;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;
import org.moment.lib.node.CarrierPeerNode;

import java.util.ArrayList;
import java.util.List;

import app.elaphant.sdk.peernode.Connector;
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
        mAdapter.setOnClickListener(new FriendsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(getContext(), "onItemClick", Toast.LENGTH_SHORT).show();
//                ChatUiUtils.startMomentActivity(getContext());
            }

            @Override
            public void sendToken(View view, int position) {

                String receivingAddress = mDatas.get(position).getTokenAddress();
                if (StringUtil.isNullOrEmpty(receivingAddress)) {
                    Toast.makeText(getContext(), "receiving address is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                Uri.Builder builder = new Uri.Builder();
                builder.scheme("elaphant")
                        .authority("elapay")
                        .appendQueryParameter("AppID", BRConstants.ELAPHANT_APP_ID)
                        .appendQueryParameter("PublicKey", BRConstants.ELAPHANT_APP_PUBLICKEY)
                        .appendQueryParameter("Did", BRConstants.ELAPHANT_APP_DID)
                        .appendQueryParameter("AppName", BRConstants.ELAPHANT_APP_NAME)
                        .appendQueryParameter("ReceivingAddress", receivingAddress)
                        .appendQueryParameter("Amount", "0")
                        .appendQueryParameter("CoinName", "ELA");

                String tmp = builder.build().toString();
                Uri scheme = Uri.parse(tmp);
                Intent intent = new Intent(Intent.ACTION_VIEW, scheme);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            @Override
            public void sendMessage(View view, int position) {
                String friendCode = mDatas.get(position - 1).getFriendCode();
                if (!StringUtil.isNullOrEmpty(friendCode)) {
                    ContactInterface.Status status = CarrierPeerNode.getInstance(getContext()).getFriendStatus(friendCode);
                    if (status == ContactInterface.Status.Online) {
                        Intent intent = new Intent(getContext(), ChatDetailActivity.class);
                        intent.putExtra("friendCode", friendCode);
                        getContext().startActivity(intent);
                        return;
                    }
                }
                Toast.makeText(getContext(), "send Message failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void deleteFriends(View view, int position) {
                //TODO daokun.xi
                CarrierPeerNode.getInstance(getContext()).removeFriend(mDatas.get(position).getFriendCode());
                mDatas.remove(position);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    public void addFriend(final String friendCode) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                int ret = CarrierPeerNode.getInstance(getContext()).addFriend(friendCode, "summary");
                refreshFriendView();
            }
        });
    }

    private void refreshFriendView() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //TODO daokun.xi
                List<ContactInterface.FriendInfo> friendInfos = CarrierPeerNode.getInstance(getContext()).getFriends();
                if (null == friendInfos) return;
                List<ContactEntity> contacts = new ArrayList<>();
                contacts.clear();
                for (ContactInterface.FriendInfo info : friendInfos) {
                    ContactEntity contactEntity = new ContactEntity();
                    contactEntity.setContact(/*info.nickname*/info.did);
                    contactEntity.setTokenAddress(info.elaAddress);
                    contactEntity.setFriendCode(info.humanCode);
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
        });
//        getActivity().getWindow().getDecorView().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                //TODO daokun.xi
//                List<ContactInterface.FriendInfo> friendInfos = CarrierPeerNode.getInstance(getContext()).getFriends();
//                if (null == friendInfos) return;
//                List<ContactEntity> contacts = new ArrayList<>();
//                contacts.clear();
//                for (ContactInterface.FriendInfo info : friendInfos) {
//                    ContactEntity contactEntity = new ContactEntity();
//                    contactEntity.setContact(/*info.nickname*/info.did);
//                    contactEntity.setTokenAddress(info.elaAddress);
//                    contactEntity.setFriendCode(info.humanCode);
//                    contacts.add(contactEntity);
//                }
//
//                mDatas.clear();
//                mDatas.addAll(contacts);
//
//                mIndexBar.setmSourceDatas(mDatas)
//                        .invalidate();
//                mDecoration.setmDatas(mDatas);
//
//                mAdapter.setDatas(mDatas);
//                mAdapter.notifyDataSetChanged();
//            }
//        }, 500);
    }

    private void initDatas(final String[] data) {

        refreshFriendView();

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
        CarrierPeerNode.getInstance(getContext()).stop();
    }
}
