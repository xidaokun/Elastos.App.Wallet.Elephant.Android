package com.breadwallet.presenter.activities;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.LoadingDialog;
import com.breadwallet.tools.util.StringUtil;

import org.wallet.library.AuthorizeManager;

public class ExploreWebActivity extends BRActivity {

    private WebView webView;
    private String mUrl;
    private LoadingDialog mLoadingDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expolre_web_layout);

        mUrl = getIntent().getStringExtra("explore_url");

        webView = findViewById(R.id.web_view);
        webviewSetting();

        mLoadingDialog = new LoadingDialog(this, R.style.progressDialog);
        mLoadingDialog.setCanceledOnTouchOutside(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!StringUtil.isNullOrEmpty(mUrl))
            webView.loadUrl(mUrl);
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
                if(StringUtil.isNullOrEmpty(url)) return false;
                if(url.startsWith("elaphant") && url.contains("identity")) {
                    AuthorizeManager.startWalletActivity(ExploreWebActivity.this, url, "com.breadwallet.presenter.activities.did.DidAuthorizeActivity");
                } else if(url.startsWith("elaphant") && url.contains("elapay")) {
                    AuthorizeManager.startWalletActivity(ExploreWebActivity.this, url, "com.breadwallet.presenter.activities.WalletActivity");
                } else {
                    view.loadUrl(url);
                }

                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLoadingDialog.show();
                    }
                });
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLoadingDialog.dismiss();
                    }
                });
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
                Log.i("xidaokun", "onReceivedSslError");
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                Log.i("xidaokun", "onReceivedHttpError");
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.i("xidaokun", "onReceivedError");
            }
        });
        webView.loadUrl(mUrl);
    }
}
