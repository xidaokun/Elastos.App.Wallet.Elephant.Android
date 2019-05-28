package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.BREdit;
import com.breadwallet.presenter.customviews.BaseTextView;

public class AddAppsActivity extends BaseSettingsActivity {

    private BREdit mUrl;
    private BaseTextView mAdd;
    private BaseTextView mClean;

    @Override
    public int getLayoutId() {
        return R.layout.activity_add_apps_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.close_button;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initListener();
    }

    private void initView(){
        mUrl = findViewById(R.id.add_apps_edt);
        mAdd = findViewById(R.id.close_button);
        mClean = findViewById(R.id.add_apps_clean);
    }

    private void initListener(){
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("url", mUrl.getText().toString());
                setResult(RESULT_OK, intent);
            }
        });

        mClean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUrl.setText("");
            }
        });
    }
}
