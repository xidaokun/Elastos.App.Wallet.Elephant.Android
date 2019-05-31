package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.did.DidDataSource;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.jsbridge.JsInterface;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.wallet.wallets.ela.WalletElaManager;
import com.elastos.jni.Utility;
import com.google.gson.Gson;

import org.elastos.sdk.keypair.ElastosKeypairSign;
import org.wallet.library.AuthorizeManager;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

public class MultiSignCreateActivity extends BRActivity {
    private final String TAG = "MultiSignCreateActivity";

    private int mRequiredCount;
    private String[] mPublicKeys;
    private String mAddress;
    private String mReturnUrl;
    private String mCallbackUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_sign_create);

        if (!initData()) {
            finish();
            return;
        }

        initView();
        initPublicKey();

    }

    private boolean initData() {
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null) {
            return false;
        }

        Log.d(TAG, "uri: " + uri.toString());

        String appName = uri.getQueryParameter("AppName");
        String appID = uri.getQueryParameter("AppID");
        String publicKey = uri.getQueryParameter("PublicKey");
        String did = uri.getQueryParameter("DID");
        if (!AuthorizeManager.verify(this, did, publicKey, appName, appID)) {
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

        String require = uri.getQueryParameter("RequiredCount");
        if(StringUtil.isNullOrEmpty(require)) {
            Log.e(TAG, "no require count");
            return false;
        }
        mRequiredCount = Integer.parseInt(require);

        String returnurl = uri.getQueryParameter("ReturnUrl");
        if(!StringUtil.isNullOrEmpty(returnurl)) {
            mReturnUrl = URLDecoder.decode(returnurl);
        }

        String callbackurl = uri.getQueryParameter("CallbackUrl");
        if(!StringUtil.isNullOrEmpty(callbackurl)) {
            mCallbackUrl = URLDecoder.decode(returnurl);
        }

        mAddress = ElastosKeypairSign.getMultiSignAddress(mPublicKeys, mPublicKeys.length, mRequiredCount);
        Log.d(TAG, "addr: " + mAddress);
        if(StringUtil.isNullOrEmpty(mAddress)) {
            Log.e(TAG, "get multi sign wallet address failed");
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

    private void setResultAndFinish(String address) {
        final String mne = getMn();
        final String pk = Utility.getInstance(this).getSinglePrivateKey(mne);
        final String myPK = Utility.getInstance(this).getSinglePublicKey(mne);
        final String myDid = Utility.getInstance(this).getDid(myPK);

        MultiCreateReturn data = new MultiCreateReturn();
        data.DID = myDid;
        data.PublicKey = myPK;
        data.Address = address;

        String dataStr = new Gson().toJson(data);

        String sign = AuthorizeManager.sign(this, pk, dataStr);

        if (!StringUtil.isNullOrEmpty(mCallbackUrl)) {
            String body = "{\"Data\":\"" + dataStr.replace("\"", "\\\"") + "\", \"Sign\":\"" + sign + "\"}";
            Log.d(TAG, "post body: " + body);
            DidDataSource.getInstance(this).urlPost(mCallbackUrl, dataStr);
        }

        if (!StringUtil.isNullOrEmpty(mReturnUrl)) {
            String url;
            if (mReturnUrl.contains("?")) {
                url = mReturnUrl + "&Data=";
            } else {
                url = mReturnUrl + "?Data=";
            }
            url += Uri.encode(dataStr) + "&Sign=" + Uri.encode(sign) + "&Scheme=multicreate";

            Log.d(TAG, "url: " + url);

            if (url.startsWith("file://")) {
                UiUtils.startWebviewActivity(this, url);
            } else {
                UiUtils.openUrlByBrowser(this, url);
            }
        }

        finish();
    }

    private void initView() {
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

    private void initPublicKey() {
        String myPublicKey = WalletElaManager.getInstance(this).getPublicKey();
        // if public key is null, finish and return.
        // The app will show authentication screen
        if (myPublicKey == null) {
            finish();
            return;
        }

        LinearLayout page = findViewById(R.id.multisign_create_page);

        for (String publicKey : mPublicKeys) {
            TextView lableView = new TextView(this);
            if (myPublicKey.equals(publicKey)) {
                lableView.setText(myPublicKey + "(me)");
            } else {
                lableView.setText(publicKey);
            }

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = getResources().getDimensionPixelOffset(R.dimen.radius);

            lableView.setTextSize(12);
            lableView.setTextColor(getResources().getColor(R.color.black_333333));

            lableView.setLayoutParams(lp);
            page.addView(lableView);
        }
    }

    private void create() {
        JsInterface.MultiSignParam param = new JsInterface.MultiSignParam();
        param.PublicKeys = mPublicKeys;
        param.RequiredCount = mRequiredCount;

        BRSharedPrefs.putMultiSignInfo(this, mAddress, new Gson().toJson(param));

        Toast toast = Toast.makeText(getApplicationContext(),
                    R.string.multisign_created, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        setResultAndFinish(mAddress);
    }

    private class MultiCreateReturn {
        String DID;
        String PublicKey;
        String Address;
    }
}
