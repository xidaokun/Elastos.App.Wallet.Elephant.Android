package org.chat.lib.presenter;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.google.gson.Gson;

import org.chat.lib.adapter.ChatAdapter;
import org.chat.lib.adapter.CommonFragmentPagerAdapter;
import org.chat.lib.entity.FullImageInfo;
import org.chat.lib.entity.MessageCacheBean;
import org.chat.lib.entity.MessageInfo;
import org.chat.lib.source.ChatDataSource;
import org.chat.lib.utils.Constants;
import org.chat.lib.utils.GlobalOnItemClickListener;
import org.chat.lib.widget.EmotionInputDetector;
import org.chat.lib.widget.NoScrollViewPager;
import org.chat.lib.widget.RoundImageView;
import org.chat.lib.widget.StateButton;
import org.easy.recycleview.EasyRecyclerView;
import org.elastos.sdk.elephantwallet.contact.internal.ContactInterface;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.node.CarrierPeerNode;

import java.util.ArrayList;
import java.util.List;

public class ChatDetailActivity extends BRActivity {

    View mBackBtn;
    RoundImageView mHeaderImg;
    TextView mAliasTv;
    EasyRecyclerView chatLv;
    ImageView emotionIv;
    EditText editEdt;
    TextView voiceTv;
    TextView titleTv;
    TextView chatIdTv;
    ImageView emotionBtn;
    ImageView emotionAddIv;
    StateButton emotionSendBtn;
    NoScrollViewPager viewpager;
    RelativeLayout emotionLayout;

    private void initView() {
        mBackBtn = findViewById(R.id.back_button);
        mAliasTv = findViewById(R.id.chat_detail_alias);
        mHeaderImg = findViewById(R.id.chat_detail_header);
        titleTv = findViewById(R.id.title);
        chatIdTv = findViewById(R.id.chat_detail_id);
        chatLv = findViewById(R.id.chat_list);
        emotionIv = findViewById(R.id.emotion_voice);
        editEdt = findViewById(R.id.edit_text);
        voiceTv = findViewById(R.id.voice_text);
        emotionBtn = findViewById(R.id.emotion_button);
        emotionAddIv = findViewById(R.id.emotion_add);
        emotionSendBtn = findViewById(R.id.emotion_send);
        viewpager = findViewById(R.id.viewpager);
        emotionLayout = findViewById(R.id.emotion_layout);
        refreshStatus();
    }

    private void refreshStatus() {
        ContactInterface.Status status = CarrierPeerNode.getInstance(this).getFriendStatus(mFriendCodeStr);
        if(status!=null && status==ContactInterface.Status.Online) {
            mHeaderImg.setImageResource(R.drawable.chat_head_online_bg);
        } else {
            mHeaderImg.setImageResource(R.drawable.chat_head_offline_bg);
        }
    }

    private EmotionInputDetector mDetector;
    private ArrayList<Fragment> fragments;
    private ChatEmotionFragment chatEmotionFragment;
    private ChatFunctionFragment chatFunctionFragment;
    private CommonFragmentPagerAdapter adapter;

    private ChatAdapter chatAdapter;
    private LinearLayoutManager layoutManager;
    private List<MessageInfo> messageInfos;

    private String mFriendCodeStr;
    private String mType;
    private String mTitle;
    int animationRes = 0;
    int res = 0;
    AnimationDrawable animationDrawable = null;
    private ImageView animView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail_layout);
        mFriendCodeStr = getIntent().getStringExtra("did");
        mType = getIntent().getStringExtra("type");
        mTitle = getIntent().getStringExtra("chatName");

        initView();
        EventBus.getDefault().register(this);
        initWidget();
    }

    private void initWidget() {
        if(!StringUtil.isNullOrEmpty(mTitle)) {
            titleTv.setText(mTitle);
            mAliasTv.setText(mTitle.substring(0, 1));
        }
        if(!StringUtil.isNullOrEmpty(mFriendCodeStr)) chatIdTv.setText(mFriendCodeStr);

        fragments = new ArrayList<>();
        chatEmotionFragment = new ChatEmotionFragment();
        fragments.add(chatEmotionFragment);
        chatFunctionFragment = new ChatFunctionFragment();
        fragments.add(chatFunctionFragment);
        adapter = new CommonFragmentPagerAdapter(getSupportFragmentManager(), fragments);
        viewpager.setAdapter(adapter);
        viewpager.setCurrentItem(0);

        mDetector = EmotionInputDetector.with(this)
                .setEmotionView(emotionLayout)
                .setViewPager(viewpager)
                .bindToContent(chatLv)
                .bindToEditText(editEdt)
                .bindToEmotionButton(emotionBtn)
                .bindToAddButton(emotionAddIv)
                .bindToSendButton(emotionSendBtn)
                .bindToVoiceButton(emotionIv)
                .bindToVoiceText(voiceTv)
                .build();

        GlobalOnItemClickListener globalOnItemClickListener = GlobalOnItemClickListener.getInstance(this);
        globalOnItemClickListener.attachToEditText(editEdt);

        chatAdapter = new ChatAdapter(this);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        chatLv.setLayoutManager(layoutManager);
        chatLv.setAdapter(chatAdapter);
        chatLv.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:
                        chatAdapter.handler.removeCallbacksAndMessages(null);
                        chatAdapter.notifyDataSetChanged();
                        break;
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        chatAdapter.handler.removeCallbacksAndMessages(null);
                        mDetector.hideEmotionLayout(false);
                        mDetector.hideSoftInput();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        chatAdapter.addItemClickListener(itemClickListener);
        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        LoadData();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void friendStatusChange(CarrierPeerNode.FriendStatusInfo friendStatusInfo) {
        refreshStatus();
    }

    private ChatAdapter.onItemClickListener itemClickListener = new ChatAdapter.onItemClickListener() {
        @Override
        public void onHeaderClick(int position) {
            Toast.makeText(ChatDetailActivity.this, "onHeaderClick", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onImageClick(View view, int position) {
            int location[] = new int[2];
            view.getLocationOnScreen(location);
            FullImageInfo fullImageInfo = new FullImageInfo();
            fullImageInfo.setLocationX(location[0]);
            fullImageInfo.setLocationY(location[1]);
            fullImageInfo.setWidth(view.getWidth());
            fullImageInfo.setHeight(view.getHeight());
            fullImageInfo.setImageUrl(messageInfos.get(position).getImageUrl());
            EventBus.getDefault().postSticky(fullImageInfo);
            startActivity(new Intent(ChatDetailActivity.this, FullImageActivity.class));
            overridePendingTransition(0, 0);
        }

        @Override
        public void onVoiceClick(final ImageView imageView, final int position) {
            if (animView != null) {
                animView.setImageResource(res);
                animView = null;
            }
            switch (messageInfos.get(position).getType()) {
                case 1:
                    animationRes = R.drawable.voice_left;
                    res = R.drawable.icon_voice_left3;
                    break;
                case 2:
                    animationRes = R.drawable.voice_right;
                    res = R.drawable.icon_voice_right3;
                    break;
            }
            animView = imageView;
            animView.setImageResource(animationRes);
            animationDrawable = (AnimationDrawable) imageView.getDrawable();
            animationDrawable.start();
            //anr error
//            MediaManager.playSound(messageInfos.get(position).getFilepath(), new MediaPlayer.OnCompletionListener() {
//                @Override
//                public void onCompletion(MediaPlayer mp) {
//                    animView.setImageResource(res);
//                }
//            });
        }

        @Override
        public void onFailedClick(View view, final int position) {
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    handleSend(messageInfos.get(position));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            chatAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });
        }
    };

    private void LoadData() {
        messageInfos = new ArrayList<>();

//        MessageInfo messageInfo = new MessageInfo();
//        messageInfo.setContent("欢迎使用elephant IM");
//        messageInfo.setType(Constants.CHAT_ITEM_TYPE_LEFT);
//        messageInfo.setHeader("https://xidaokun.github.io/im_boy.png");
//        messageInfos.add(messageInfo);
//
//        MessageInfo messageInfo1 = new MessageInfo();
//        messageInfo1.setFilepath("http://www.trueme.net/bb_midi/welcome.wav");
//        messageInfo1.setVoiceTime(3000);
//        messageInfo1.setType(Constants.CHAT_ITEM_TYPE_RIGHT);
//        messageInfo1.setSendState(Constants.CHAT_ITEM_SEND_SUCCESS);
//        messageInfo1.setHeader("https://xidaokun.github.io/im_girl.png");
//        messageInfos.add(messageInfo1);
//
//        MessageInfo messageInfo2 = new MessageInfo();
//        messageInfo2.setImageUrl("https://xidaokun.github.io/im_boy.png");
//        messageInfo2.setType(Constants.CHAT_ITEM_TYPE_LEFT);
//        messageInfo2.setHeader("https://xidaokun.github.io/im_boy.png");
//        messageInfos.add(messageInfo2);
//
//        MessageInfo messageInfo3 = new MessageInfo();
//        messageInfo3.setContent("[微笑][色][色][色]");
//        messageInfo3.setType(Constants.CHAT_ITEM_TYPE_RIGHT);
//        messageInfo3.setSendState(Constants.CHAT_ITEM_SEND_ERROR);
//        messageInfo3.setHeader("https://xidaokun.github.io/im_girl.png");
//        messageInfos.add(messageInfo3);



        List<MessageCacheBean> allMessageCacheBeans  = ChatDataSource.getInstance(this).getMessage(BRSQLiteHelper.CHAT_MESSAGE_FRIENDCODE + " = ? ", new String[]{mFriendCodeStr});

        if(allMessageCacheBeans==null || allMessageCacheBeans.size()<=0) return;

        for(MessageCacheBean messageCacheBean : allMessageCacheBeans) {
            MessageInfo messageInfo = new MessageInfo();
            messageInfo.setContent(messageCacheBean.MessageContent);
            messageInfo.setTime(messageCacheBean.MessageTimestamp);
            messageInfo.setType(messageCacheBean.MessageOrientation);
            messageInfo.setSendState(messageCacheBean.MessageSendState);
            messageInfo.setHeader("https://xidaokun.github.io/im_boy.png");
            messageInfos.add(messageInfo);
        }
        chatAdapter.addAll(messageInfos);
        ChatDataSource.getInstance(ChatDetailActivity.this).updateMessage(allMessageCacheBeans, true);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void MessageEventBus(final MessageInfo messageInfo) {

        final int type = messageInfo.getType();
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {

                if(Constants.CHAT_ITEM_TYPE_RIGHT == type) { //send handle
                    handleSend(messageInfo);
                }

                if(Constants.CHAT_ITEM_TYPE_LEFT == type){ //receive handle
                    handleReceive(messageInfo);
                }

                runOnUiThread(new Runnable() {  //refresh ui
                    @Override
                    public void run() {
                        messageInfos.add(messageInfo);
                        chatAdapter.add(messageInfo);
                        chatLv.scrollToPosition(chatAdapter.getCount() - 1);
                    }
                });
            }
        });
    }

    private void handleSend(MessageInfo messageInfo) {
        messageInfo.setSendState(Constants.CHAT_ITEM_SENDING);
        messageInfo.setHeader("https://xidaokun.github.io/im_boy.png");
//        MsgProtocol msgProtocol = new MsgProtocol();
//        msgProtocol.content = messageInfo.getContent();
        long ret = 0;
        String type = null;
        if(mType==null || mType.contains(BRConstants.CHAT_SINGLE_TYPE)) {
            ret = CarrierPeerNode.getInstance(ChatDetailActivity.this).sendMessage(mFriendCodeStr, /*new Gson().toJson(msgProtocol)*/messageInfo.getContent());
            type = BRConstants.CHAT_SINGLE_TYPE;
        } else if(mType.contains(BRConstants.CHAT_GROUP_TYPE)) {
            ret = CarrierPeerNode.getInstance(ChatDetailActivity.this).sendGroupMessage(mFriendCodeStr, /*new Gson().toJson(msgProtocol)*/messageInfo.getContent());
            type = BRConstants.CHAT_GROUP_TYPE;
        } else {
            ret = -1;
        }
        messageInfo.setSendState((0<ret)?Constants.CHAT_ITEM_SEND_ERROR:Constants.CHAT_ITEM_SEND_SUCCESS);
        Log.d("xidaokun", "ChatDetailActivity#handleSend#ret:"+ret);
        //TODO daokun.xi test log
        final long finalRet = ret;
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ChatDetailActivity.this, "sendMessage ret:"+ finalRet, Toast.LENGTH_SHORT).show();
            }
        });

        ChatDataSource.getInstance(ChatDetailActivity.this)
                .setType(type)
                .setNickname(mTitle)
                .setContentType(ChatDataSource.TYPE_MESSAGE_TEXT)
                .setContent(messageInfo.getContent())
                .hasRead(true)
                .setTimestamp(ret<0?messageInfo.getTime():ret)
                .setOrientation(Constants.CHAT_ITEM_TYPE_RIGHT)
                .setFriendCode(mFriendCodeStr)
                .setSendState((0<ret)?Constants.CHAT_ITEM_SEND_ERROR:Constants.CHAT_ITEM_SEND_SUCCESS)
                .cacheMessgeInfo();
    }

    private void handleReceive(MessageInfo messageInfo) {
        //TODO daokun.xi only change read status
        MessageCacheBean messageCacheBean = new MessageCacheBean();
        messageCacheBean.MessageFriendCode = messageInfo.getFriendCode();

        List<MessageCacheBean> messageCacheBeans = new ArrayList<>();
        messageCacheBeans.add(messageCacheBean);
        Log.d("xidaokun", "ChatDetailActivity#handleReceive#\ncacheMessage:"+ new Gson().toJson(messageCacheBeans));
        ChatDataSource.getInstance(ChatDetailActivity.this).updateMessage(messageCacheBeans, true);
    }

    @Override
    public void onBackPressed() {
        if (!mDetector.interceptBackPress()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().removeStickyEvent(this);
        EventBus.getDefault().unregister(this);
    }

}
