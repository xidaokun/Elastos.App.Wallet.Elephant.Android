package com.breadwallet.presenter.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
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
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.wallet.wallets.ela.WalletElaManager;
import com.elastos.jni.Utility;
import com.elastos.jni.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.elastos.sdk.elephantwallet.contact.Utils;
import org.elastos.sdk.keypair.ElastosKeypair;
import org.elastos.sdk.wallet.BlockChainNode;
import org.elastos.sdk.wallet.Did;
import org.elastos.sdk.wallet.DidManager;
import org.elastos.sdk.wallet.Identity;
import org.elastos.sdk.wallet.IdentityManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import app.elaphant.sdk.peernode.PeerNode;
import app.elaphant.sdk.peernode.PeerNodeListener;

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

    private PeerNode mPeerNode;

    private String mPrivateKey /*= "b8e923f4e5c5a3c704bcc02a90ee0e4fa34a5b8f0dd1de1be4eb2c37ffe8e3ea"*/;
    private String mPublicKey /*= "021e53dc2b8af1548175cba357ae321096065f8d49e3935607bc8844c157bb0859"*/;

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
                case R.id.navigation_chat:
                    showFragment(mChatFragment);
                    return true;
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

        disableShiftingMode(navigation);

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

        initPeerNode();
    }

    private void initPeerNode() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                mPrivateKey = WalletElaManager.getInstance(HomeActivity.this).getPrivateKey();
                mPublicKey = WalletElaManager.getInstance(HomeActivity.this).getPublicKey();
                mPeerNode = PeerNode.getInstance(getFilesDir().getAbsolutePath(),
                        Settings.Secure.getString(HomeActivity.this.getContentResolver(), Settings.Secure.ANDROID_ID));
                mPeerNode.setListener(new PeerNodeListener.Listener() {

                    @Override
                    public byte[] onAcquire(org.elastos.sdk.elephantwallet.contact.Contact.Listener.AcquireArgs request) {
                        byte[] response = null;
                        switch (request.type) {
                            case PublicKey:
                                if(StringUtil.isNullOrEmpty(mPublicKey))
                                    Toast.makeText(HomeActivity.this, "mPublicKey is null", Toast.LENGTH_SHORT).show();
                                response = mPublicKey.getBytes();
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
                                response = getAgentAuthHeader();
                                break;
                            case SignData:
                                response = signData(request.data);
                                break;
                            default:
                                throw new RuntimeException("Unprocessed request: " + request);
                        }
                        return response;
                    }

                    @Override
                    public void onError(int errCode, String errStr, String ext) {

                    }
                });

                int ret = mPeerNode.start();
            }
        });
    }

    private byte[] getAgentAuthHeader() {
        String appid = "org.elastos.debug.didplugin";
        String appkey = "b2gvzUM79yLhCbbGNWCuhSsGdqYhA7sS";
        long timestamp = System.currentTimeMillis();
        String auth = Utils.getMd5Sum(appkey + timestamp);
        String headerValue = "id=" + appid + ";time=" + timestamp + ";auth=" + auth;
        Log.i(TAG, "getAgentAuthHeader() headerValue=" + headerValue);

        return headerValue.getBytes();
    }

    private byte[] signData(byte[] data) {

        ElastosKeypair.Data originData = new ElastosKeypair.Data();
        originData.buf = data;

        ElastosKeypair.Data signedData = new ElastosKeypair.Data();

        if(StringUtil.isNullOrEmpty(mPrivateKey))
            Toast.makeText(this, "mPrivateKey is null", Toast.LENGTH_SHORT).show();
        int signedSize = ElastosKeypair.sign(mPrivateKey, originData, originData.buf.length, signedData);
        if(signedSize <= 0) {
            return null;
        }

        return signedData.buf;
    }

    @Override
    protected void onResume() {
        super.onResume();
//        boolean iscrash = getIntent().getBooleanExtra("crash", false);
//        Log.i(TAG, "iscrash:" + iscrash);
//        if (iscrash) navigation.setSelectedItemId(R.id.navigation_home);

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
        if (keyCode==KeyEvent.KEYCODE_BACK && (navigation.getVisibility()!=View.VISIBLE)) {
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

    private String getKeyVale(String path, String value){
        KeyValue key = new KeyValue();
        key.Key = path;
        key.Value = value;
        List<KeyValue> keys = new ArrayList<>();
        keys.add(key);
        return new Gson().toJson(keys, new TypeToken<List<KeyValue>>(){}.getType());
    }

    private Did mDid;
    private String mSeed;
    private String publicKey;
    private void initDid(){
        if(null == mDid){
            String mnemonic = getMn();
            if(StringUtil.isNullOrEmpty(mnemonic)) return;
            mSeed = IdentityManager.getSeed(mnemonic, "");
            if(StringUtil.isNullOrEmpty(mSeed)) return;
            Identity identity = IdentityManager.createIdentity(getFilesDir().getAbsolutePath());
            DidManager didManager = identity.createDidManager(mSeed);
            BlockChainNode node = new BlockChainNode(ProfileDataSource.DID_URL);
            mDid = didManager.createDid(0);
            mDid.setNode(node);
            publicKey = Utility.getInstance(HomeActivity.this).getSinglePublicKey(mnemonic);
        }
    }

    private void didIsOnchain(){
        long nowTime = System.currentTimeMillis();
        long didTime = BRSharedPrefs.getDid2ChainTime(this);
        Log.d("didIsOnchain", "nowTime-didTime:"+(nowTime-didTime));
        if(nowTime-didTime < 15*60*1000) return;
        Log.d("didIsOnchain", "nowTime-didTime > 15*60*1000");
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                initDid();
                if(null == mDid) return;
                mDid.syncInfo();
                String value = mDid.getInfo("PublicKey", false, mSeed);
                Log.i("didIsOnchain", "value:"+value);
                if(StringUtil.isNullOrEmpty(value) || !value.contains("PublicKey")){
                    if(StringUtil.isNullOrEmpty(publicKey)) return;
                    String data = getKeyVale("PublicKey", publicKey);
                    if(StringUtil.isNullOrEmpty(data) || StringUtil.isNullOrEmpty(mSeed)) return;
                    String info = mDid.signInfo(mSeed, data, false);
                    if(StringUtil.isNullOrEmpty(info)) return;
                    ProfileDataSource.getInstance(HomeActivity.this).upchain(info);
                    BRSharedPrefs.putDid2ChainTime(HomeActivity.this, System.currentTimeMillis());
                }
            }
        });
    }

    private String getMn(){
        byte[] phrase = null;
        try {
            phrase = BRKeyStore.getPhrase(this, 0);
            if(phrase != null) {
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
        if(mChatFragment!=null && !StringUtil.isNullOrEmpty(value)) {
            mChatFragment.selectFriendFragment(value);
        }
    }

    public void showAndDownloadCapsule(String url) {
        if(mExploreFragment!=null && !StringUtil.isNullOrEmpty(url)){
            boolean isValid = StringUtils.isElaphantCapsule(url) || StringUtils.isHttpCapsule(url);
            if (!isValid) {
                Toast.makeText(this, getString(R.string.mini_app_invalid_url), Toast.LENGTH_SHORT).show();
                return;
            }
            showFragment(mExploreFragment);
            mExploreFragment.downloadCapsule(url);
            navigation.setSelectedItemId(R.id.navigation_explore);
        }
    }

    @Override
    public void onConnectionChanged(boolean isConnected) {
        if (mWalletFragment != null)
            mWalletFragment.onConnectionChanged(isConnected);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHomeActivity = null;
    }

    @SuppressLint("RestrictedApi")
    public static void disableShiftingMode(BottomNavigationView view) {
        try {
            BottomNavigationMenuView mMenuView = (BottomNavigationMenuView) view.getChildAt(0);
            Field mShiftingModeField = BottomNavigationMenuView.class.getDeclaredField("mShiftingMode");
            mShiftingModeField.setAccessible(true);
            mShiftingModeField.set(mMenuView, false);
            for (int i = 0; i < mMenuView.getChildCount(); i++) {
                BottomNavigationItemView itemView = (BottomNavigationItemView) mMenuView.getChildAt(i);
                itemView.setShifting(false);
                itemView.setChecked(itemView.getItemData().isChecked());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
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