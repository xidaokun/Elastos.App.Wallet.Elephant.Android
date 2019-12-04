package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.did.DidDataSource;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.NoScrollListView;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.wallet.wallets.ela.WalletElaManager;
import com.elastos.jni.AuthorizeManager;
import com.elastos.jni.Utility;
import com.google.gson.Gson;
import org.elastos.sdk.keypair.ElastosKeypairSign;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiSignCreateActivity extends BRActivity {
    private final String TAG = "MultiSignCreateActivity";

    private NoScrollListView mListView;

    private int mRequiredCount;
    private String[] mPublicKeys;
    private String mAddress;
    private String mReturnUrl;
    private String mCallbackUrl;
    private String mAppID;
    private int mMyIndex = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_sign_create);

        if (!initData()) {
            finish();
            return;
        }

        initView();
    }

    private boolean initData() {
        String mn = getMn();
        String myPublicKey = null;
        if(StringUtil.isNullOrEmpty(mn)){
            myPublicKey = WalletElaManager.getInstance(this).getPublicKey();
        } else {
            myPublicKey = Utility.getInstance(this).getSinglePublicKey(mn);
        }
//        String myPublicKey = WalletElaManager.getInstance(this).getPublicKey();
        // if public key is null, finish and return.
        // The app will show authentication screen
        if (myPublicKey == null) {
            return false;
        }

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null) {
            return false;
        }

        Log.d(TAG, "uri: " + uri.toString());

        String appName;
        try {
            appName = URLDecoder.decode(uri.getQueryParameter("AppName"), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
        mAppID = uri.getQueryParameter("AppID");
        String publicKey = uri.getQueryParameter("PublicKey");
        String did = uri.getQueryParameter("DID");
        if (!AuthorizeManager.verify(this, did, publicKey, appName, mAppID)) {
            Log.e(TAG, "check app ID failed!");
            return false;
        }

        String publicKeys = uri.getQueryParameter("PublicKeys");
        if(StringUtil.isNullOrEmpty(publicKeys)) {
            Log.e(TAG, "no public keys");
            return false;
        }
        String pubkeys = URLDecoder.decode(publicKeys);
        Log.d(TAG, "publickeys: " + pubkeys);
        List<String> list= Arrays.asList(pubkeys.split(","));
        mPublicKeys = (String[]) list.toArray();

        for (int i = 0; i < mPublicKeys.length; i++) {
            if (myPublicKey.equals(mPublicKeys[i])) {
                mMyIndex = i;
                break;
            }
        }
        if (mMyIndex < 0) {
            Log.e(TAG, "my public key is not in the pulickey list");
            UiUtils.toast(getApplicationContext(), R.string.multisign_create_pubkey_not_in_list);
            return false;
        }

        String require = uri.getQueryParameter("RequiredCount");
        if(StringUtil.isNullOrEmpty(require)) {
            Log.e(TAG, "no require count");
            return false;
        }
        try {
            mRequiredCount = Integer.parseInt(require);
        } catch (NumberFormatException ignored) {
            mRequiredCount = 0;
        }
        if (mRequiredCount <= 0 || mRequiredCount > mPublicKeys.length) {
            Log.d(TAG, "require count is illegal. requireCount: " + require);
            return false;
        }

        String returnurl = uri.getQueryParameter("ReturnUrl");
        if(!StringUtil.isNullOrEmpty(returnurl)) {
            mReturnUrl = URLDecoder.decode(returnurl);
        }

        String callbackurl = uri.getQueryParameter("CallbackUrl");
        if(!StringUtil.isNullOrEmpty(callbackurl)) {
            mCallbackUrl = URLDecoder.decode(callbackurl);
        }

        mAddress = ElastosKeypairSign.getMultiSignAddress(mPublicKeys, mPublicKeys.length, mRequiredCount);
        Log.d(TAG, "addr: " + mAddress);
        if(StringUtil.isNullOrEmpty(mAddress)) {
            Log.e(TAG, "get multi sign wallet address failed");
            UiUtils.toast(getApplicationContext(), R.string.multisign_create_failed);
            return false;
        }

        return true;
    }

    private String getMn() {
        byte[] phrase = null;
        try {
            phrase = BRKeyStore.getPhrase(this, 0);
            if (phrase != null) {
                return new String(phrase);
            }
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean setResult(String address) {
        final String mne = getMn();
        if (StringUtil.isNullOrEmpty(mne)) {
            Log.e(TAG, "get men failed");
            BRDialog.showSimpleDialog(this, getString(R.string.multisign_create_failed_title),
                    getString(R.string.multisign_create_failed));
            return false;
        }
        final String pk = Utility.getInstance(this).getSinglePrivateKey(mne);
        final String myPK = Utility.getInstance(this).getSinglePublicKey(mne);
        final String myDid = Utility.getInstance(this).getDid(myPK);

        MultiCreateReturn data = new MultiCreateReturn();
        data.DID = myDid;
        data.PublicKey = myPK;
        data.Address = address;

        final String dataStr = new Gson().toJson(data);

        final String sign = AuthorizeManager.sign(this, pk, dataStr);

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                if (!StringUtil.isNullOrEmpty(mCallbackUrl)) {
                    try {
                        String body = "{\"Data\":\"" + dataStr.replace("\"", "\\\"") + "\", \"Sign\":\"" + sign + "\"}";
                        Log.d(TAG, "post body: " + body);
                        DidDataSource.getInstance(MultiSignCreateActivity.this).urlPost(mCallbackUrl, body);
                    } catch (Exception e) {
                        Toast.makeText(MultiSignCreateActivity.this, "callback error", Toast.LENGTH_SHORT);
                        e.printStackTrace();
                    }
                }

                UiUtils.returnDataNeedSign(MultiSignCreateActivity.this, mReturnUrl, dataStr, sign, mAppID, "");
                finish();
            }
        });

        return true;
    }

    private void initView() {
        initListView();
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Button denyBtn = findViewById(R.id.multisign_create_deny);
        denyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        Button createBtn = findViewById(R.id.multisign_create);
        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                create();
            }
        });

        TextView addr = findViewById(R.id.multisign_create_addr);
        addr.setText(mAddress);

        TextView pubkeyTitle = findViewById(R.id.multisign_create_pubkeys);
        pubkeyTitle.setText(String.format(getResources().getString(R.string.multisign_create_keys), mPublicKeys.length));

        TextView required = findViewById(R.id.multisign_create_required);
        required.setText(String.format(getResources().getString(R.string.multisign_create_unlock_keys), mRequiredCount));
    }

    private void initListView() {
        mListView = findViewById(R.id.multisign_create_pubkey_list);
        View headerView = LayoutInflater.from(this).inflate(R.layout.multi_sign_create_header, mListView, false);
        mListView.addHeaderView(headerView);

        View footerView = LayoutInflater.from(this).inflate(R.layout.multi_sign_create_footer, mListView, false);
        mListView.addFooterView(footerView);

        ArrayList<PublicKeyAdapter.PublicKey> publicKeys = new ArrayList<>();

        for (int i = 0; i < mPublicKeys.length; i++) {
            String str = mPublicKeys[i];
            if (i == mMyIndex) {
                str += getString(R.string.multisign_wallet_pubkey_me);
            }

            PublicKeyAdapter.PublicKey pubObj = new PublicKeyAdapter.PublicKey();
            pubObj.mPublicKey = str;
            pubObj.mSigned = false;
            publicKeys.add(pubObj);
        }

        ArrayAdapter adapter = new PublicKeyAdapter(this, R.layout.publickey_label, publicKeys);
        mListView.setAdapter(adapter);
    }

    private void create() {
        if (!setResult(mAddress)) {
            return;
        }

        MultiSignParam param = new MultiSignParam();
        param.PublicKeys = mPublicKeys;
        param.RequiredCount = mRequiredCount;

        BRSharedPrefs.putMultiSignInfo(this, mAddress, new Gson().toJson(param));
        UiUtils.toast(getApplicationContext(), R.string.multisign_created);
    }

    private class MultiCreateReturn {
        String DID;
        String PublicKey;
        String Address;
    }

    public static class MultiSignParam {
        public String[] PublicKeys;
        public int RequiredCount;
    }
}
