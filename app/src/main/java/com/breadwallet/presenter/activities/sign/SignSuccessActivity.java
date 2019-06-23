package com.breadwallet.presenter.activities.sign;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.util.StringUtil;

public class SignSuccessActivity extends BaseSettingsActivity {

    private BRButton mCopyBtn;
    private BaseTextView mHistoryBtn;
    private BaseTextView mSignedContent;

    @Override
    public int getLayoutId() {
        return R.layout.activity_sign_success_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();
        initListener();
        initData();
    }

    private void initView() {
        mHistoryBtn = findViewById(R.id.esign_history);
        mCopyBtn = findViewById(R.id.copy_btn);
        mSignedContent = findViewById(R.id.sign_result);
    }

    private void initListener() {
        mCopyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String signedContent = mSignedContent.getText().toString();
                copyText(signedContent);
            }
        });

        mHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.startSignHistoryActivity(SignSuccessActivity.this);
            }
        });
    }

    private void initData() {
        mSignedContent.setText(getSignedFromIntent());
        UiUtils.toastMessage(this, R.layout.sign_success_toast_layout);
    }

    private String getSignedFromIntent(){
        Intent intent = getIntent();
        if(null == intent) return null;
        return intent.getStringExtra("signed");
    }

    private void copyText(String content) {
        if(!StringUtil.isNullOrEmpty(content)){
            BRClipboardManager.putClipboard(this, content);
            UiUtils.toastMessage(this, R.layout.sign_copy_toast_layout);
        }
    }
}
