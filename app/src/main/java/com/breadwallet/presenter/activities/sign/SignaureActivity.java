package com.breadwallet.presenter.activities.sign;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.did.CallbackData;
import com.breadwallet.did.CallbackEntity;
import com.breadwallet.did.DidDataSource;
import com.breadwallet.did.SignCallbackData;
import com.breadwallet.did.SignInfo;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.customviews.LoadingDialog;
import com.breadwallet.presenter.customviews.RoundImageView;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRDateUtil;
import com.breadwallet.tools.util.StringUtil;
import com.elastos.jni.AuthorizeManager;
import com.elastos.jni.Constants;
import com.elastos.jni.UriFactory;
import com.elastos.jni.Utility;
import com.google.gson.Gson;

public class SignaureActivity extends BRActivity {

    private static final String TAG =  SignaureActivity.class.getSimpleName() + "_debug";

    private ImageButton mBackBtn;
    private RoundImageView mAppIconIv;
    private BaseTextView mAppNameTv;
    private BaseTextView mAppIdTv;
    private BaseTextView mDidTv;
    private BaseTextView mTimestampTv;
    private BaseTextView mPurposeTv;
    private BaseTextView mContentTv;
    private BRButton mDenyBtn;
    private BRButton mSignBtn;
    private BaseTextView mAddLimitTv;
    private BaseTextView mViewAllTv;

    private String mUri;
    private UriFactory uriFactory;
    private SignInfo mSignInfo = new SignInfo();

    private LoadingDialog mLoadingDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signaure_layout);

        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            if (!StringUtil.isNullOrEmpty(action) && action.equals(Intent.ACTION_VIEW)) {
                Uri uri = intent.getData();
                mUri = uri.toString();
            } else {
                mUri = intent.getStringExtra(Constants.INTENT_EXTRA_KEY.META_EXTRA);
            }
        }

        mUri = "https://launch.elaphant.app/?appName=Vote%20For%20Me&autoRedirect=True&appTitle=Vote%20For%20Me&redirectURL=elaphant%3A%2F%2Fsign%3FAppID%3D3461ba97b110118ed25a66697e021004e3de05a52c05bb10470619d73d3932a59c59e81a6f6621f22dfc9b0182df5891d13bd2afcb86cb8665d02e608f03b3cf%26AppName%3DVote%2520For%2520Me%26DID%3DibxNTG1hBPK1rZuoc8fMy4eFQ96UYDAQ4J%26PublicKey%3D034c51ddc0844ff11397cc773a5b7d94d5eed05e7006fb229cf965b47f19d27c55%26RequestedContent%3D%257B%2522Name%2522%253A%25221125test%2522%252C%2522Desc%2522%253A%2522%2522%252C%2522Type%2522%253A%2522multipleChoice%2522%252C%2522Max-Selections%2522%253A3%252C%2522Limit-balance%2522%253A0%252C%2522Starting-height%2522%253A259203%252C%2522End-height%2522%253A265083%252C%2522Options%2522%253A%255B%257B%2522OptionID%2522%253A1%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B91%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A2%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B92%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A3%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B93%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A4%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B94%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A5%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B95%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A6%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B96%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A7%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B97%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A8%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B98%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A9%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B99%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A10%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B910%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A11%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B911%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A12%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B912%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A13%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B913%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A14%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B914%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A15%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B915%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A16%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B916%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A17%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B917%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A18%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B918%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A19%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B919%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A20%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B920%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A21%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B921%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A22%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B922%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A23%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B923%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A24%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B924%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A25%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B925%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A26%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B926%25E5%2593%2588%25E5%2593%2588%25E5%2593%2588%25E5%2593%2588%25E5%2593%2588%25E5%2593%2588%25E5%2593%2588%25E5%2593%2588%25E5%2593%2588%25E5%2593%2588%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%25E6%2588%2596%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%252C%257B%2522OptionID%2522%253A27%252C%2522Name%2522%253A%2522%25E9%2580%2589%25E9%25A1%25B927%25E5%2593%2588%25E5%2595%258A%25E5%2595%258A%25E5%2595%258A%25E5%2595%258A%25E9%2598%25BF%25E5%2591%2580%25E5%2591%2580%25E5%2591%2580%25E5%2591%2580%25E5%2591%2580%25E5%2591%2580%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%25E6%2599%2595%2522%252C%2522Desc%2522%253A%2522https%253A%252F%252Fwww.2345.com%252F%2522%257D%255D%252C%2522Starting-height-alias%2522%253A1574661639%252C%2522End-height-alias%2522%253A1575720039%257D%26UseStatement%3Ddidvote%26ReturnUrl%3Dhttp%253A%252F%252Fdev1.elapps.net%252FcreateResult.html";

        initView();
        initListener();
        initData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(null == data) return;
        if(BRConstants.SIGN_PURPOSE_REQUEST == requestCode){
            String purpose = data.getStringExtra("purpose");
            mSignInfo.setPurpose(purpose);
            if(null != purpose) mPurposeTv.setText(purpose);
        }
    }

    public void initView(){
        mBackBtn = findViewById(R.id.back_button);
        mAppIconIv = findViewById(R.id.app_icon);
        mAppNameTv = findViewById(R.id.app_name);
        mAppIdTv = findViewById(R.id.app_id);
        mDidTv = findViewById(R.id.developer_did);
        mTimestampTv = findViewById(R.id.timestamp);
        mPurposeTv = findViewById(R.id.purpose);
        mContentTv = findViewById(R.id.content);
        mDenyBtn = findViewById(R.id.deny_btn);
        mSignBtn = findViewById(R.id.sign_btn);
        mAddLimitTv = findViewById(R.id.add_limitation_btn);
        mViewAllTv = findViewById(R.id.view_all_details_btn);
        mLoadingDialog = new LoadingDialog(this, R.style.progressDialog);
        mLoadingDialog.setCanceledOnTouchOutside(false);
    }

    public void initListener(){
        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mDenyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mSignBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sign();
            }
        });
        mAddLimitTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.startSignEditActivity(SignaureActivity.this, "limit",mSignInfo.getPurpose(), BRConstants.SIGN_PURPOSE_REQUEST);
            }
        });
        mViewAllTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.startSignEditActivity(SignaureActivity.this, "viewAll",mSignInfo.getContent(), BRConstants.SIGN_CONTENT_REQUEST);
            }
        });
    }

    private void sign(){
        String phrase = getPhrase();
        if (StringUtil.isNullOrEmpty(phrase)) {
            Toast.makeText(this, "Not yet created Wallet", Toast.LENGTH_SHORT).show();
            return;
        }

        if (StringUtil.isNullOrEmpty(mUri)) {
            Toast.makeText(this, "invalid params", Toast.LENGTH_SHORT).show();
            return;
        }

        final String did = uriFactory.getDID();
        final String appId = uriFactory.getAppID();
        final String target = uriFactory.getTarget();
        String appName = uriFactory.getAppName();
        String PK = uriFactory.getPublicKey();
        if(StringUtil.isNullOrEmpty(did) || StringUtil.isNullOrEmpty(appId) || StringUtil.isNullOrEmpty(appName)
                || StringUtil.isNullOrEmpty(PK)) {
            Toast.makeText(this, "invalid params", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        boolean isValid = AuthorizeManager.verify(this, did, PK, appName, appId);
        if(!isValid) {
            Toast.makeText(this, "verify failed", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        final String backurl = uriFactory.getCallbackUrl();
        final String returnUrl = uriFactory.getReturnUrl();

        String pk = Utility.getInstance(this).getSinglePrivateKey(phrase);
        String myPK = Utility.getInstance(this).getSinglePublicKey(phrase);
        String myDid = Utility.getInstance(this).getDid(myPK);

        SignCallbackData callbackData = new SignCallbackData();
        callbackData.DID = myDid;
        callbackData.PublicKey = myPK;
        callbackData.RequesterDID = mSignInfo.getDid();
        callbackData.RequestedContent = mSignInfo.getContent();
        callbackData.Timestamp = mSignInfo.getTimestamp() / 1000;
        callbackData.UseStatement = mSignInfo.getPurpose();

        final String Data = new Gson().toJson(callbackData);
        final String Sign = AuthorizeManager.sign(this, pk, Data);

        final CallbackEntity entity = new CallbackEntity();
        entity.Data = Data;
        entity.Sign = Sign;

        if (!isFinishing()) mLoadingDialog.show();
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    UiUtils.callbackDataNeedSign(SignaureActivity.this, backurl, entity);
                    UiUtils.returnDataNeedSign(SignaureActivity.this, returnUrl, Data, Sign, appId, target);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    dialogDismiss();
                    finish();
                }
            }
        });
        DidDataSource.getInstance(this).cacheSignApp(mSignInfo);
    }

    private String getPhrase() {
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

    private void dialogDismiss() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing())
                    mLoadingDialog.dismiss();
            }
        });
    }

    public void initData(){
        if (StringUtil.isNullOrEmpty(mUri)) return;
        uriFactory = new UriFactory();
        uriFactory.parse(mUri);
        long timestamp = System.currentTimeMillis();
        String timeFormat =  BRDateUtil.getAuthorDate(timestamp);

        mSignInfo.setAppName(uriFactory.getAppName());
        mSignInfo.setDid(uriFactory.getDID());
        mSignInfo.setAppId(uriFactory.getAppID());
        mSignInfo.setContent(uriFactory.getRequestedContent());
        mSignInfo.setTimestamp(timestamp);
        mSignInfo.setPurpose(uriFactory.getUseStatement());

        String appName = mSignInfo.getAppName();
        if(!StringUtil.isNullOrEmpty(appName)) mAppNameTv.setText(appName);

        String appId = mSignInfo.getAppId();
        if(!StringUtil.isNullOrEmpty(appId)) mAppIdTv.setText("App ID:"+appId);

        String reqDid = mSignInfo.getDid();
        if(!StringUtil.isNullOrEmpty(reqDid)) mDidTv.setText(reqDid);

        if(!StringUtil.isNullOrEmpty(timeFormat)) mTimestampTv.setText(timeFormat);

        String purpose = mSignInfo.getPurpose();
        if(!StringUtil.isNullOrEmpty(purpose)) mPurposeTv.setText(purpose);

        String content = mSignInfo.getContent();
        if(!StringUtil.isNullOrEmpty(content)) mContentTv.setText(content);

        int iconResourceId = getResources().getIdentifier("unknow", BRConstants.DRAWABLE, getPackageName());
        if(!StringUtil.isNullOrEmpty(appId)) {
            if(appId.equals(BRConstants.REA_PACKAGE_ID)){
                iconResourceId = getResources().getIdentifier("redpackage", BRConstants.DRAWABLE, getPackageName());
            } else if(appId.equals(BRConstants.DEVELOPER_WEBSITE) || appId.equals(BRConstants.DEVELOPER_WEBSITE_TEST)){
                iconResourceId = getResources().getIdentifier("developerweb", BRConstants.DRAWABLE, getPackageName());
            } else if(appId.equals(BRConstants.HASH_ID)){
                iconResourceId = getResources().getIdentifier("hash", BRConstants.DRAWABLE, getPackageName());
            }
        }
        mAppIconIv.setImageDrawable(getDrawable(iconResourceId));
    }
}
