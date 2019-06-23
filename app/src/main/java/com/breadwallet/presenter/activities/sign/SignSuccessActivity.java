package com.breadwallet.presenter.activities.sign;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.BaseTextView;

public class SignSuccessActivity extends BaseSettingsActivity {

    private BaseTextView mCopyBtn;
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
        mCopyBtn = findViewById(R.id.copy_btn);
        
    }

    private void initListener() {

    }

    private void initData() {

    }
}
