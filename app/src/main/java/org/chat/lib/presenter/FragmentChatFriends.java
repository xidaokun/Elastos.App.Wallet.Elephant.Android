package org.chat.lib.presenter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.tools.animation.ElaphantDialogText;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.elastos.jni.utils.SchemeStringUtils;

import org.chat.lib.adapter.FriendsAdapter;
import org.chat.lib.entity.ContactEntity;
import org.chat.lib.entity.NewFriendBean;
import org.chat.lib.push.PushServer;
import org.chat.lib.source.ChatDataSource;
import org.chat.lib.widget.DividerItemDecoration;
import org.chat.lib.widget.IndexBar;
import org.chat.lib.widget.SuspensionDecoration;
import org.elastos.sdk.elephantwallet.contact.Contact;
import org.elastos.sdk.elephantwallet.contact.internal.ContactInterface;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.node.CarrierPeerNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        initListener();
        EventBus.getDefault().register(this);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshFriendView();
    }


    private void initView(View rootView) {
        mSideHintTv = rootView.findViewById(R.id.side_bar_hint);
        mRecyclerView = rootView.findViewById(R.id.friends_rv);
        mRecyclerView.setLayoutManager(mManager = new LinearLayoutManager(getContext()));
        mIndexBar = rootView.findViewById(R.id.indexBar);
        mIndexBar.setmPressedShowTextView(mSideHintTv)
                .setNeedRealIndex(false)
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
                if(position == 0) {
                    UiUtils.startWaitAcceptActivity(getContext());
                }
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
                String friendCode = mDatas.get(position).getFriendCode();
                String type = mDatas.get(position).getType();
                String chatName = mDatas.get(position).getContact();
                Log.d("xidaokun", "FragementChatFriends#sendMessage#type:"+type);
                if (!StringUtil.isNullOrEmpty(friendCode)) {
                    Log.d("xidaokun", "FragementChatFriends#sendMessage#did:"+friendCode);
                    UiUtils.startChatDetailActivity(getContext(), friendCode, type, chatName);
                }
            }

            @Override
            public void editNickname(View view, int position) {
                String friendCode = mDatas.get(position).getFriendCode();
                String nickname = mDatas.get(position).getContact();
                UiUtils.startProfileEditActivity(getContext(), friendCode, nickname);
            }

            @Override
            public void longPress(View view, int position) {
            }

            @Override
            public void deleteFriends(View view, final int position) {

                //mRemoveHint.setText(Html.fromHtml(String.format(getString(R.string.esign_remove_nini_app_hint), item.name_zh_CN)));

                final ElaphantDialogText elaphantDialog = new ElaphantDialogText(getContext());
                Spanned deleteHint = Html.fromHtml(String.format(getContext().getString(R.string.My_chat_friends_delete_pop_hint), mDatas.get(position).getContact()));
                elaphantDialog.setMessageSpan(deleteHint);
                elaphantDialog.setPositiveStr(getContext().getString(R.string.My_chat_friends_delete_pop_confirm));
                elaphantDialog.setNegativeStr(getContext().getString(R.string.My_chat_friends_delete_pop_cancel));
                elaphantDialog.setPositiveListener(new ElaphantDialogText.OnPositiveClickListener() {
                    @Override
                    public void onClick() {
                        //TODO daokun.xi
                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                String friendCode = mDatas.get(position).getFriendCode();
                                if(StringUtil.isNullOrEmpty(friendCode)) return;
                                int ret = CarrierPeerNode.getInstance(getContext()).removeFriend(mDatas.get(position).getFriendCode());
                                Log.d("xidaokun", "FragementChatFriends#deleteFriends#ret:"+ret);
                                if(0 != ret) {
                                    return;
                                }
                                ChatDataSource.getInstance(getContext()).deleteMessage(friendCode);
                                ChatDataSource.getInstance(getContext()).deleteMessageItemInfo(friendCode);
                                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        mDatas.remove(position);
                                        mAdapter.notifyDataSetChanged();
                                        EventBus.getDefault().post(new FragmentChatMessage.RefreshMessage());
                                    }
                                });
                            }
                        });
                        elaphantDialog.dismiss();
                    }
                });

                elaphantDialog.setNegativeListener(new ElaphantDialogText.OnNegativeClickListener() {
                    @Override
                    public void onClick() {
                        elaphantDialog.dismiss();
                    }
                });
                elaphantDialog.show();

            }
        });
    }

    public void addFriend(final String friendCode) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {

                NewFriendBean newFriendBean = ChatDataSource.getInstance(getContext()).getFriendByCode(friendCode);
                if(null!=newFriendBean && newFriendBean.acceptStatus==BRConstants.RECEIVE_ACCEPT) {
                    final int ret = CarrierPeerNode.getInstance(getContext()).acceptFriend(friendCode, BRConstants.CHAT_SINGLE_TYPE);
                    if(ret == 0) {
                        ChatDataSource.getInstance(getContext()).updateAcceptState(friendCode, BRConstants.ACCEPTED);
                    }
                    if(ret != 0) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), "carrier return ret:"+ret, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    Log.d("xidaokun", "FragementChatFriends#acceptFriend#ret:"+ret);
                } else {
                    final int ret = CarrierPeerNode.getInstance(getContext()).addFriend(friendCode);
                    Log.d("xidaokun", "FragementChatFriends#addFriend#ret:"+ret);
                    String myDid = BRSharedPrefs.getMyDid(getContext());
                    String myCarrierAddr = BRSharedPrefs.getCarrierId(getContext());
                    String nickName = BRSharedPrefs.getNickname(getContext());
                    PushServer.sendNotice(myDid, friendCode, nickName, myCarrierAddr);
//                            PushServer.sendIosNotice(myDid, friendCode, nickName, myCarrierAddr);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(0 != ret) {
                                Toast.makeText(getContext(), "carrier return ret:"+ret, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

                refreshFriendView();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void acceptFriendEvent(CarrierPeerNode.FriendStatusInfo friendStatusInfo) {
        Log.d("xidaokun", "FragementChatFriends#acceptFriendEvent#did:"+friendStatusInfo.humanCode+"\n#status:"+friendStatusInfo.status);
        ChatDataSource.getInstance(getContext()).updateAcceptState(friendStatusInfo.humanCode, BRConstants.ACCEPTED);
        refreshFriendView();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestAddEvent(String friendCode) {
        Log.d("xidaokun", "FragementChatFriends#requestAddEvent#did:"+friendCode);
        refreshFriendView();
    }

    private String getFriendNickname(Map<String,String> friends, String humancode, List<Contact.HumanInfo.CarrierInfo> carrierInfos) {
        String nickname = null;
        nickname = friends.get(humancode);
        if(StringUtil.isNullOrEmpty(nickname)) {
            for(Contact.HumanInfo.CarrierInfo carrierInfo : carrierInfos) {
                nickname = friends.get(carrierInfo.usrAddr);
                if(!StringUtil.isNullOrEmpty(nickname)) return nickname;
            }
        }

        return nickname;
    }

    private void refreshFriendView() {
        Log.d("xidaokun", "FragmentChatFriends#refreshFriendView");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<ContactInterface.FriendInfo> friendInfos = CarrierPeerNode.getInstance(getContext()).getFriends();
                Map<String, String> friendsNickname = ChatDataSource.getInstance(getContext()).getAllFriendName();
                List<ContactEntity> contacts = new ArrayList<>();
                if (null != friendInfos) {
                    for (ContactInterface.FriendInfo info : friendInfos) {
                        if(info.status==ContactInterface.Status.WaitForAccept ||
                                info.status==ContactInterface.Status.Removed ||
                                info.status==ContactInterface.Status.Invalid) continue;
                        ContactEntity contactEntity = new ContactEntity();
                        String nickname = getFriendNickname(friendsNickname, info.humanCode, info.boundCarrierArray);
                        contactEntity.setContact(SchemeStringUtils.isNullOrEmpty(nickname)?"nickname":nickname);
                        contactEntity.setTokenAddress(info.elaAddress);
                        contactEntity.setFriendCode(info.humanCode);
                        contactEntity.setOnline(info.status==ContactInterface.Status.Online);
                        contactEntity.setType(info.addition);
                        contacts.add(contactEntity);
                    }
                }

                mDatas.clear();

                List<NewFriendBean> waitAcceptBeans = ChatDataSource.getInstance(getContext()).getNotAcceptFriends();
                mDatas.add((ContactEntity) new ContactEntity("新的朋友").setTop(true).setWaitAcceptCount(waitAcceptBeans.size()).setBaseIndexTag(INDEX_STRING_TOP));
//                mDatas.add((ContactEntity) new ContactEntity("群聊").setTop(true).setBaseIndexTag(INDEX_STRING_TOP));
//                mDatas.add((ContactEntity) new ContactEntity("标签").setTop(true).setBaseIndexTag(INDEX_STRING_TOP));
//                mDatas.add((ContactEntity) new ContactEntity("公众号").setTop(true).setBaseIndexTag(INDEX_STRING_TOP));

                //TODO daokun.xi test
//                ContactEntity contactEntitya = new ContactEntity();
//                contactEntitya.setContact("a");
//                contactEntitya.setFriendCode("aaaaaa");
//                contactEntitya.setOnline(true);
//                contactEntitya.setTokenAddress("Exxxxxxxxxxxxxxx");
//                mDatas.add(contactEntitya);
//
//                ContactEntity contactEntityb = new ContactEntity();
//                contactEntityb.setContact("b");
//                contactEntityb.setFriendCode("bbbbb");
//                contactEntityb.setOnline(false);
//                contactEntityb.setTokenAddress("Exxxxxxxxxxxxxxx");
//                mDatas.add(contactEntityb);
//
//                ContactEntity contactEntityc = new ContactEntity();
//                contactEntityc.setContact("c");
//                contactEntityc.setFriendCode("cccccc");
//                contactEntityc.setOnline(true);
//                contactEntityc.setTokenAddress("Exxxxxxxxxxxxxxx");
//                mDatas.add(contactEntityc);
//
//                ContactEntity contactEntityd = new ContactEntity();
//                contactEntityd.setContact("d");
//                contactEntityd.setFriendCode("dddddd");
//                contactEntityd.setOnline(true);
//                contactEntityd.setTokenAddress("Exxxxxxxxxxxxxxx");
//                mDatas.add(contactEntityd);
//
//                ContactEntity contactEntitye = new ContactEntity();
//                contactEntitye.setContact("e");
//                contactEntitye.setFriendCode("eeeeeeeee");
//                contactEntitye.setOnline(true);
//                contactEntitye.setTokenAddress("Exxxxxxxxxxxxxxx");
//                mDatas.add(contactEntitye);
//
//                ContactEntity contactEntityf = new ContactEntity();
//                contactEntityf.setContact("f");
//                contactEntityf.setFriendCode("ffffff");
//                contactEntityf.setOnline(true);
//                contactEntityf.setTokenAddress("Exxxxxxxxxxxxxxx");
//                mDatas.add(contactEntityf);
//
//                ContactEntity contactEntityg = new ContactEntity();
//                contactEntityg.setContact("g");
//                contactEntityg.setFriendCode("ggggg");
//                contactEntityg.setOnline(true);
//                contactEntityg.setTokenAddress("Exxxxxxxxxxxxxxx");
//                mDatas.add(contactEntityg);

                mDatas.addAll(contacts);

                mIndexBar.setmSourceDatas(mDatas)
                        .invalidate();
                mDecoration.setmDatas(mDatas);

                mAdapter.setDatas(mDatas);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
