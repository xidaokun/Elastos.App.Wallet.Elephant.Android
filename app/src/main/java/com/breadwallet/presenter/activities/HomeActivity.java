package com.breadwallet.presenter.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.crc.CrcDataSource;
import com.breadwallet.presenter.activities.crc.CrcUtils;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.fragments.FragmentChat;
import com.breadwallet.presenter.fragments.FragmentExplore;
import com.breadwallet.presenter.fragments.FragmentSetting;
import com.breadwallet.presenter.fragments.FragmentWallet;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.sqlite.ProfileDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.vote.CityEntity;
import com.breadwallet.vote.CrcEntity;
import com.elastos.jni.Utility;
import com.elastos.jni.utils.SchemeStringUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.chat.lib.entity.MessageInfo;
import org.chat.lib.presenter.FragmentChatMessage;
import org.chat.lib.push.PushClient;
import org.chat.lib.source.ChatDataSource;
import org.elastos.sdk.wallet.BlockChainNode;
import org.elastos.sdk.wallet.Did;
import org.elastos.sdk.wallet.DidManager;
import org.elastos.sdk.wallet.Identity;
import org.elastos.sdk.wallet.IdentityManager;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.node.CarrierPeerNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by byfieldj on 1/17/18.
 * <p>
 * Home activity that will show a list of a user's wallets
 */

public class HomeActivity extends BRActivity implements InternetManager.ConnectionReceiverListener, FragmentExplore.AboutShowListener {

    private static final String TAG = HomeActivity.class.getSimpleName() + "_test";
    private FragmentWallet mWalletFragment;
    private FragmentChat mChatFragment;
    private Fragment mSettingFragment;
    private FragmentExplore mExploreFragment;
    private FragmentManager mFragmentManager;
    private BottomNavigationView navigation;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    showFragment(mWalletFragment);
                    return true;
                case R.id.navigation_explore:
                    showFragment(mExploreFragment);
                    return true;
//                case R.id.navigation_chat:
//                    showFragment(mChatFragment);
//                    return true;
                case R.id.navigation_notifications:
                    showFragment(mSettingFragment);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setItemTextAppearanceActive(R.style.bottom_selected_text);
        navigation.setItemTextAppearanceInactive(R.style.bottom_normal_text);

        disableShiftingMode(navigation);
//        disableItemScale(navigation);

        mFragmentManager = getSupportFragmentManager();
        mWalletFragment = FragmentWallet.newInstance("Wallet");
        mExploreFragment = FragmentExplore.newInstance("Explore");
        mChatFragment = FragmentChat.newInstance("Chat");
        mSettingFragment = FragmentSetting.newInstance("Setting");

        mExploreFragment.setAboutShowListener(this);
//        clearFragment();
        mCurrentFragment = mWalletFragment;
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.add(R.id.frame_layout, mCurrentFragment).show(mCurrentFragment).commitAllowingStateLoss();

        didIsOnchain();
        mHomeActivity = this;

        Intent intent = getIntent();
        if (intent != null) {
            String dowloadUrl = intent.getStringExtra("url");
            if (!StringUtil.isNullOrEmpty(dowloadUrl)) {
                showAndDownloadCapsule(dowloadUrl);
            }
        }

//        initNode();
        initCrcCities();
        EventBus.getDefault().register(this);
    }

    private void initNode() {
        CarrierPeerNode.getInstance(this).start();
    }

    private void initCrcCities() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                //cache crc cities
                if (!BRSharedPrefs.isCacheCity(HomeActivity.this)) {
                    String cityStr = CrcUtils.INSTANCE.readCities(HomeActivity.this, "city/cities");
                    if (StringUtil.isNullOrEmpty(cityStr)) return;
                    List<CityEntity> cityEntities = new Gson().fromJson(cityStr, new TypeToken<List<CityEntity>>() {
                    }.getType());
                    if (cityEntities != null)
                        CrcDataSource.getInstance(HomeActivity.this).cacheCrcCity(cityEntities);
                    BRSharedPrefs.hasCacheCity(HomeActivity.this);
                }
                // refresh crcs
                CrcDataSource.getInstance(HomeActivity.this).getAndCacheActiveCrcs();
            }
        });
    }
    //replace with IM
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void acceptFriend(final CarrierPeerNode.RequestFriendInfo requestFriendInfo) {
//        Log.d("xidaokun", "HomeActivity#acceptFriend#\nhumancode:"+ requestFriendInfo.humanCode + "\ncontent:" + requestFriendInfo.content);
//        final ElaphantDialogText elaphantDialog = new ElaphantDialogText(this);
//        elaphantDialog.setMessageStr("添加好友请求");
//        elaphantDialog.setPositiveStr("接受");
//        elaphantDialog.setNegativeStr("拒绝");
//        elaphantDialog.setPositiveListener(new ElaphantDialogText.OnPositiveClickListener() {
//            @Override
//            public void onClick() {
//                CarrierPeerNode.getInstance(HomeActivity.this).acceptFriend(requestFriendInfo.humanCode, requestFriendInfo.content);
//                EventBus.getDefault().post(requestFriendInfo.humanCode);
//                elaphantDialog.dismiss();
//            }
//        });
//        elaphantDialog.setNegativeListener(new ElaphantDialogText.OnNegativeClickListener() {
//            @Override
//            public void onClick() {
//                elaphantDialog.dismiss();
//            }
//        });
//        elaphantDialog.show();
//    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void MessageEvent(MessageInfo messageInfo) {
        String friendCode = messageInfo.getFriendCode();

        if (StringUtil.isNullOrEmpty(friendCode)) return; //only receive message
        Log.d("xidaokun", "HomeActivity#MessageEvent#cacheMessgeInfo#begin");
        ChatDataSource.getInstance(this)
                .setType(BRConstants.CHAT_GROUP_TYPE)
                .setContentType(ChatDataSource.TYPE_MESSAGE_TEXT)
                .setContent(messageInfo.getContent())
                .hasRead(false)
                .setNickname(messageInfo.getNickName())
                .setTimestamp(messageInfo.getTime())
                .setOrientation(messageInfo.getType())
                .setFriendCode(friendCode)
                .setSendState(messageInfo.getSendState())
                .cacheMessgeInfo();

        EventBus.getDefault().post(new FragmentChatMessage.RefreshMessage());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void friendStatusChange(CarrierPeerNode.FriendStatusInfo friendStatusInfo) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        InternetManager.registerConnectionReceiver(this, this);
    }

    @Override
    public void show() {
        navigation.setVisibility(View.VISIBLE);
    }

    @Override
    public void hide() {
        navigation.setVisibility(View.GONE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && (navigation.getVisibility() != View.VISIBLE)) {
            mExploreFragment.hideAboutView();
            navigation.setVisibility(View.VISIBLE);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    class KeyValue {
        public String Key;
        public String Value;
    }

    private String getKeyVale(String path, String value) {
        KeyValue key = new KeyValue();
        key.Key = path;
        key.Value = value;
        List<KeyValue> keys = new ArrayList<>();
        keys.add(key);
        return new Gson().toJson(keys, new TypeToken<List<KeyValue>>() {
        }.getType());
    }

    private Did mDid;
    private String mSeed;
    private String publicKey;

    private void initDid() {
        if (null == mDid) {
            String mnemonic = getMn();
            if (StringUtil.isNullOrEmpty(mnemonic)) return;
            mSeed = IdentityManager.getSeed(mnemonic, "");
            if (StringUtil.isNullOrEmpty(mSeed)) return;
            Identity identity = IdentityManager.createIdentity(getFilesDir().getAbsolutePath());
            DidManager didManager = identity.createDidManager(mSeed);
            BlockChainNode node = new BlockChainNode(ProfileDataSource.DID_URL);
            mDid = didManager.createDid(0);
            mDid.setNode(node);
            publicKey = Utility.getInstance(HomeActivity.this).getSinglePublicKey(mnemonic);
//            bindDid();
        }
    }

    private void didIsOnchain() {
        long nowTime = System.currentTimeMillis();
        long didTime = BRSharedPrefs.getDid2ChainTime(this);
        Log.d("didIsOnchain", "nowTime-didTime:" + (nowTime - didTime));
        if (nowTime - didTime < 15 * 60 * 1000) return;
        Log.d("didIsOnchain", "nowTime-didTime > 15*60*1000");
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                initDid();
                if (null == mDid) return;
                mDid.syncInfo();
                String value = mDid.getInfo("PublicKey", false, mSeed);
                Log.i("didIsOnchain", "value:" + value);
                if (StringUtil.isNullOrEmpty(value) || !value.contains("PublicKey")) {
                    if (StringUtil.isNullOrEmpty(publicKey)) return;
                    String data = getKeyVale("PublicKey", publicKey);
                    if (StringUtil.isNullOrEmpty(data) || StringUtil.isNullOrEmpty(mSeed)) return;
                    String info = mDid.signInfo(mSeed, data, false);
                    if (StringUtil.isNullOrEmpty(info)) return;
                    ProfileDataSource.getInstance(HomeActivity.this).upchain(info);
                    String did = BRSharedPrefs.getMyDid(HomeActivity.this);
                    if (StringUtil.isNullOrEmpty(did)) {
                        did = Utility.getInstance(HomeActivity.this).getDid(publicKey);
                        BRSharedPrefs.cacheMyDid(HomeActivity.this, did);
                    }
                    BRSharedPrefs.putDid2ChainTime(HomeActivity.this, System.currentTimeMillis());
                }
            }
        });
    }

    private void bindDid() {
        String did = BRSharedPrefs.getMyDid(HomeActivity.this);
        if (StringUtil.isNullOrEmpty(did)) {
            did = Utility.getInstance(HomeActivity.this).getDid(publicKey);
            BRSharedPrefs.cacheMyDid(HomeActivity.this, did);
        }
        Log.d("xidaokun_push", "bind did:" + did);
        PushClient.getInstance().bindAlias(did, null);
    }

    private String getMn() {
        byte[] phrase = null;
        try {
            phrase = BRKeyStore.getPhrase(this, 0);
            if (phrase != null) {
                return new String(phrase);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        InternetManager.unregisterConnectionReceiver(this, this);
    }

    private Fragment mCurrentFragment;

    private void showFragment(Fragment fragment) {
        if (mCurrentFragment != fragment) {
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.hide(mCurrentFragment);
            mCurrentFragment = fragment;
            if (!fragment.isAdded()) {
                transaction.add(R.id.frame_layout, fragment).show(fragment).commitAllowingStateLoss();
            } else {
                transaction.show(fragment).commitAllowingStateLoss();
            }
        }
    }

    public void showChatFragment(String value) {
        if (mChatFragment != null && !StringUtil.isNullOrEmpty(value)) {
            mChatFragment.selectFriendFragment(value);
        }
    }

    public void showAndDownloadCapsule(String url) {
        if (mExploreFragment != null && !StringUtil.isNullOrEmpty(url)) {
            boolean isValid = url.toLowerCase().contains(".capsule") && (SchemeStringUtils.isElaphantPrefix(url.trim()) || SchemeStringUtils.isHttpPrefix(url.trim()));
            if (!isValid) {
                Toast.makeText(this, getString(R.string.mini_app_invalid_url), Toast.LENGTH_SHORT).show();
                return;
            }
            showFragment(mExploreFragment);
            mExploreFragment.downloadCapsule(url.trim());
            navigation.setSelectedItemId(R.id.navigation_explore);
        }
    }

    @Override
    public void onConnectionChanged(boolean isConnected) {
        if (mWalletFragment != null)
            mWalletFragment.onConnectionChanged(isConnected);
        if (isConnected)
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    CrcDataSource.getInstance(HomeActivity.this).getAndCacheActiveCrcs();
                }
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        mHomeActivity = null;
    }

    @SuppressLint("RestrictedApi")
    public static void disableShiftingMode(BottomNavigationView view) {

        BottomNavigationMenuView menuView = (BottomNavigationMenuView) view.getChildAt(0);
        try {
            menuView.setLabelVisibilityMode(1);
            for (int i = 0; i < menuView.getChildCount(); i++) {
                BottomNavigationItemView item = (BottomNavigationItemView) menuView.getChildAt(i);
                item.setShifting(false);
            }
        } catch (Exception e) {

        }

//        try {
//            BottomNavigationMenuView mMenuView = (BottomNavigationMenuView) view.getChildAt(0);
//            Field mShiftingModeField = BottomNavigationMenuView.class.getDeclaredField("mShiftingMode");
//            mShiftingModeField.setAccessible(true);
//            mShiftingModeField.set(mMenuView, false);
//            for (int i = 0; i < mMenuView.getChildCount(); i++) {
//                BottomNavigationItemView itemView = (BottomNavigationItemView) mMenuView.getChildAt(i);
//                itemView.setShifting(false);
//                itemView.setChecked(itemView.getItemData().isChecked());
//            }
//        } catch (NoSuchFieldException | IllegalAccessException e) {
//            e.printStackTrace();
//        }
    }

    @SuppressLint("RestrictedApi")
    public static void disableItemScale(BottomNavigationView view) {
        try {
            BottomNavigationMenuView menuView = (BottomNavigationMenuView) view.getChildAt(0);

            Field mLargeLabelField = BottomNavigationItemView.class.getDeclaredField("mLargeLabel");
            Field mSmallLabelField = BottomNavigationItemView.class.getDeclaredField("mSmallLabel");
            Field mShiftAmountField = BottomNavigationItemView.class.getDeclaredField("mShiftAmount");
            Field mScaleUpFactorField = BottomNavigationItemView.class.getDeclaredField("mScaleUpFactor");
            Field mScaleDownFactorField = BottomNavigationItemView.class.getDeclaredField("mScaleDownFactor");

            mSmallLabelField.setAccessible(true);
            mLargeLabelField.setAccessible(true);
            mShiftAmountField.setAccessible(true);
            mScaleUpFactorField.setAccessible(true);
            mScaleDownFactorField.setAccessible(true);


            final float fontScale = view.getResources().getDisplayMetrics().scaledDensity;

            for (int i = 0; i < menuView.getChildCount(); i++) {
                BottomNavigationItemView itemView = (BottomNavigationItemView) menuView.getChildAt(i);

                TextView lagerObj = (TextView) mLargeLabelField.get(itemView);
                TextView smallObj = (TextView) mSmallLabelField.get(itemView);
                lagerObj.setTextSize(smallObj.getTextSize() / fontScale + 0.5f);


                mShiftAmountField.set(itemView, 0);
                mScaleUpFactorField.set(itemView, 1f);
                mScaleDownFactorField.set(itemView, 1f);

                itemView.setChecked(itemView.getItemData().isChecked());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}