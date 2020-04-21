package com.breadwallet.presenter.activities;

import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.customviews.LoadingDialog;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.util.StringUtil;
import com.elastos.jni.utils.SchemeStringUtils;

public class ExploreWebActivity extends BRActivity {
    private final String TAG = ExploreWebActivity.class.getName();

    private WebView webView;
    private LoadingDialog mLoadingDialog;
    private BaseTextView mTitleTv;
    private BaseTextView mBackTv;
    private View mMenuLayout;
    private BaseTextView mAboutTv;
    private BaseTextView mCancelTv;

    private String mAppId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expolre_web_layout);

        String url = getIntent().getStringExtra("explore_url");
        mAppId = getIntent().getStringExtra("app_id");

        initView();
        initListener();

        webView.loadUrl(url);
    }

    private void initView(){
        webView = findViewById(R.id.web_view);
        mTitleTv = findViewById(R.id.explore_web_title);
        mBackTv = findViewById(R.id.explore_web_back);
        mMenuLayout = findViewById(R.id.explore_web_about_layout);
        mAboutTv = findViewById(R.id.explore_web_about);
        mCancelTv = findViewById(R.id.explore_web_cancle);
        webviewSetting();

        mLoadingDialog = new LoadingDialog(this, R.style.progressDialog);
        mLoadingDialog.setCanceledOnTouchOutside(false);
    }

    private void initListener(){
        findViewById(R.id.explore_web_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMenuLayout.setVisibility(View.VISIBLE);
            }
        });


        findViewById(R.id.explore_web_finish).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mBackTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(webView.canGoBack()) webView.goBack();
            }
        });

        mMenuLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        mCancelTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMenuLayout.setVisibility(View.GONE);
            }
        });
        mAboutTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(ExploreWebActivity.this, AppAboutActivity.class);
//                startActivity(intent);
                mMenuLayout.setVisibility(View.GONE);
                if(!StringUtil.isNullOrEmpty(mAppId)){
                    UiUtils.startMiniAppAboutActivity(ExploreWebActivity.this, mAppId);
                }
            }
        });
    }

    private void webviewSetting() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setLoadsImagesAutomatically(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(StringUtil.isNullOrEmpty(url)) return true;
                loadUrl(url);
                Log.d(TAG, "shouldOverrideUrlLoading:"+url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "onPageStarted:"+url);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!isFinishing() && !mLoadingDialog.isShowing()){
                            mLoadingDialog.show();
                        }
                    }
                });
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                Log.i("schemeLoadurl", "url:"+url);

                String title = view.getTitle();

                WebBackForwardList mWebBackForwardList = webView.copyBackForwardList();
                int account = mWebBackForwardList.getCurrentIndex();
                if(url.contains("redpacket")){
                    mTitleTv.setText(getResources().getString(R.string.redpackage_title));
                    mBackTv.setVisibility((account>1)?View.VISIBLE:View.GONE);
                } else {
                    mTitleTv.setText(title);
                    mBackTv.setVisibility((account>0)?View.VISIBLE:View.GONE);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!isFinishing() && mLoadingDialog.isShowing()){
                            mLoadingDialog.dismiss();
                        }
                    }
                });
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            WebBackForwardList mWebBackForwardList = webView.copyBackForwardList();
            int account = mWebBackForwardList.getCurrentIndex();
            if(account > 0){
                webView.goBack();
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private synchronized void loadUrl(String url){
        Log.d("schemeLoadurl", "url:"+url);
        if(StringUtil.isNullOrEmpty(url)) return;

        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme != null && scheme.equals("elaphant") && host != null) {
            switch (host) {
                case "multitx":
                    UiUtils.startMultiTxActivity(this, uri);
                    return;
                case "multicreate":
                    UiUtils.startMultiCreateActivity(this, uri);
                    return;
                default:
                    break;
            }
        }

        if(url.contains("elaphant") && url.contains("identity")) {
            UiUtils.startAuthorActivity(ExploreWebActivity.this, url);
            finish();
        } else if(url.contains("elaphant") && url.contains("elapay")) {
            UiUtils.startWalletActivity(ExploreWebActivity.this, url);
            finish();
        } else if(url.contains("elaphant") && url.contains("eladposvote")) {
            UiUtils.startCrcActivity(ExploreWebActivity.this, url);
            finish();
        } else if(url.contains("elaphant") && url.contains("sign")) {
            UiUtils.startSignActivity(ExploreWebActivity.this, url);
            finish();
        } else if(url.contains("elaphant") && url.contains("elacrcvote")) {
            UiUtils.startCrcActivity(ExploreWebActivity.this, url);
            finish();
        }else if(mHomeActivity!=null && SchemeStringUtils.isElaphantPrefix(url)) {
            mHomeActivity.showAndDownloadCapsule(url);
            finish();
        } else {
            webView.loadUrl(url);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mLoadingDialog != null){
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
    }
}
