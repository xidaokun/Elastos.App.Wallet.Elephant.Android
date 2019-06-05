package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BREdit;

public class EsignActivity extends BaseSettingsActivity {

    private BRButton mSignBtn;
    private BREdit mSignEdt;

    @Override
    public int getLayoutId() {
        return R.layout.activity_esign_layout;
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
        mSignBtn = findViewById(R.id.sign_btn);
        mSignEdt = findViewById(R.id.doc_to_sign_content);
    }

    private void initListener() {
        mSignBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void initData() {

    }
}
