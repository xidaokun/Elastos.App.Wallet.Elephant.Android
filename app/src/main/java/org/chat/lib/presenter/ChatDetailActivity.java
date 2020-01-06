package org.chat.lib.presenter;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import org.chat.lib.adapter.ChatAdapter;
import org.chat.lib.adapter.CommonFragmentPagerAdapter;
import org.chat.lib.entity.FullImageInfo;
import org.chat.lib.entity.MessageInfo;
import org.chat.lib.utils.Constants;
import org.chat.lib.utils.GlobalOnItemClickListener;
import org.chat.lib.widget.EmotionInputDetector;
import org.chat.lib.widget.NoScrollViewPager;
import org.chat.lib.widget.StateButton;
import org.easy.recycleview.EasyRecyclerView;
import org.elastos.sdk.elephantwallet.contact.Contact;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.moment.lib.node.CarrierPeerNode;

import java.util.ArrayList;
import java.util.List;

import app.elaphant.sdk.peernode.Connector;
import app.elaphant.sdk.peernode.PeerNode;

public class ChatDetailActivity extends FragmentActivity {

    EasyRecyclerView chatList;
    ImageView emotionVoice;
    EditText editText;
    TextView voiceText;
    ImageView emotionButton;
    ImageView emotionAdd;
    StateButton emotionSend;
    NoScrollViewPager viewpager;
    RelativeLayout emotionLayout;

    PeerNode mPeerNode;

    private Connector mConnector = null;

    private void initView() {
        chatList = findViewById(R.id.chat_list);
        emotionVoice = findViewById(R.id.emotion_voice);
        editText = findViewById(R.id.edit_text);
        voiceText = findViewById(R.id.voice_text);
        emotionButton = findViewById(R.id.emotion_button);
        emotionAdd = findViewById(R.id.emotion_add);
        emotionSend = findViewById(R.id.emotion_send);
        viewpager = findViewById(R.id.viewpager);
        emotionLayout = findViewById(R.id.emotion_layout);
    }

    private EmotionInputDetector mDetector;
    private ArrayList<Fragment> fragments;
    private ChatEmotionFragment chatEmotionFragment;
    private ChatFunctionFragment chatFunctionFragment;
    private CommonFragmentPagerAdapter adapter;

    private ChatAdapter chatAdapter;
    private LinearLayoutManager layoutManager;
    private List<MessageInfo> messageInfos;

    private String mFriendCode;
    int animationRes = 0;
    int res = 0;
    AnimationDrawable animationDrawable = null;
    private ImageView animView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail_layout);
        mFriendCode = getIntent().getStringExtra("friendCode");
        mPeerNode = PeerNode.getInstance();
        initView();
        EventBus.getDefault().register(this);
        initWidget();
    }

    private void initWidget() {
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
                .bindToContent(chatList)
                .bindToEditText(editText)
                .bindToEmotionButton(emotionButton)
                .bindToAddButton(emotionAdd)
                .bindToSendButton(emotionSend)
                .bindToVoiceButton(emotionVoice)
                .bindToVoiceText(voiceText)
                .build();

        GlobalOnItemClickListener globalOnItemClickListener = GlobalOnItemClickListener.getInstance(this);
        globalOnItemClickListener.attachToEditText(editText);

        chatAdapter = new ChatAdapter(this);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        chatList.setLayoutManager(layoutManager);
        chatList.setAdapter(chatAdapter);
        chatList.setOnScrollListener(new RecyclerView.OnScrollListener() {
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
        LoadData();
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
    };

    private void LoadData() {
        messageInfos = new ArrayList<>();

        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setContent("欢迎使用elephant IM");
        messageInfo.setType(Constants.CHAT_ITEM_TYPE_LEFT);
        messageInfo.setHeader("https://xidaokun.github.io/im_boy.png");
        messageInfos.add(messageInfo);

        MessageInfo messageInfo1 = new MessageInfo();
        messageInfo1.setFilepath("http://www.trueme.net/bb_midi/welcome.wav");
        messageInfo1.setVoiceTime(3000);
        messageInfo1.setType(Constants.CHAT_ITEM_TYPE_RIGHT);
        messageInfo1.setSendState(Constants.CHAT_ITEM_SEND_SUCCESS);
        messageInfo1.setHeader("https://xidaokun.github.io/im_girl.png");
        messageInfos.add(messageInfo1);

        MessageInfo messageInfo2 = new MessageInfo();
        messageInfo2.setImageUrl("https://xidaokun.github.io/im_boy.png");
        messageInfo2.setType(Constants.CHAT_ITEM_TYPE_LEFT);
        messageInfo2.setHeader("https://xidaokun.github.io/im_boy.png");
        messageInfos.add(messageInfo2);

        MessageInfo messageInfo3 = new MessageInfo();
        messageInfo3.setContent("[微笑][色][色][色]");
        messageInfo3.setType(Constants.CHAT_ITEM_TYPE_RIGHT);
        messageInfo3.setSendState(Constants.CHAT_ITEM_SEND_ERROR);
        messageInfo3.setHeader("https://xidaokun.github.io/im_girl.png");
        messageInfos.add(messageInfo3);

        chatAdapter.addAll(messageInfos);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void MessageEventBus(final MessageInfo messageInfo) {

//        messageInfo.setHeader("https://xidaokun.github.io/im_girl.png");
//        messageInfo.setType(Constants.CHAT_ITEM_TYPE_RIGHT);
//        messageInfo.setSendState(Constants.CHAT_ITEM_SENDING);
//        messageInfos.add(messageInfo);
//        chatAdapter.add(messageInfo);
//        chatList.scrollToPosition(chatAdapter.getCount() - 1);
//        new Handler().postDelayed(new Runnable() {
//            public void run() {
//                messageInfo.setSendState(Constants.CHAT_ITEM_SEND_SUCCESS);
//                chatAdapter.notifyDataSetChanged();
//            }
//        }, 2000);
//        new Handler().postDelayed(new Runnable() {
//            public void run() {
//                MessageInfo message = new MessageInfo();
//                message.setContent("mock message reply");
//                message.setType(Constants.CHAT_ITEM_TYPE_LEFT);
//                message.setHeader("https://xidaokun.github.io/im_boy.png");
//                messageInfos.add(message);
//                chatAdapter.add(message);
//                chatList.scrollToPosition(chatAdapter.getCount() - 1);
//            }
//        }, 3000);


        messageInfos.add(messageInfo);
        chatAdapter.add(messageInfo);
        chatList.scrollToPosition(chatAdapter.getCount() - 1);
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
