package com.breadwallet.presenter.activities.sign;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.SignHistoryItem;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.sqlite.EsignDataSource;
import com.breadwallet.tools.util.StringUtil;

import java.util.List;

public class SignDetailActivity extends BaseSettingsActivity {

    private BaseTextView mSignedCopyBtn;
    private BaseTextView mSignCopyBtn;
    private BaseTextView mSignedContent;
    private BaseTextView mSignContent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initListener();
        initData();
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_sign_details_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }

    private void initView() {
        mSignedCopyBtn = findViewById(R.id.signed_copy);
        mSignedContent = findViewById(R.id.signed_content);
        mSignCopyBtn = findViewById(R.id.sign_copy);
        mSignContent = findViewById(R.id.sign_content);
        mSignContent.setMovementMethod(ScrollingMovementMethod.getInstance());
        mSignedContent.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    private void initListener() {
        mSignedCopyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String signed = getSignedFromIntent();
                if(!StringUtil.isNullOrEmpty(signed)){
                    copyText(signed);
                }
            }
        });

        mSignCopyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sign = getSignFromIntent();
                if(!StringUtil.isNullOrEmpty(sign)){
                    copyText(sign);
                }
            }
        });
    }

    private void initData() {
        String sign = getSignFromIntent();
        String signed = getSignedFromIntent();
        mSignContent.setText(StringUtil.isNullOrEmpty(sign) ? "" : sign);
        mSignedContent.setText(StringUtil.isNullOrEmpty(signed) ? "" : signed);
    }

    private String getSignFromIntent(){
        Intent intent = getIntent();
        if(null == intent) return null;
        return intent.getStringExtra("sign");
    }

    private String getSignedFromIntent(){
        Intent intent = getIntent();
        if(null == intent) return null;
        return intent.getStringExtra("signed");
    }

    private void copyText(String content) {
        if(!StringUtil.isNullOrEmpty(content)){
            BRClipboardManager.putClipboard(this, content);
            Toast.makeText(this, getString(R.string.Receive_copied), Toast.LENGTH_SHORT).show();
        }
    }

}
