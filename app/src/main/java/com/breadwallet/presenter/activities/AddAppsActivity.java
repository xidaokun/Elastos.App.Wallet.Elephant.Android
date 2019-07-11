package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.BREdit;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.util.StringUtil;

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
        return R.id.back_button;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initListener();
    }

    private void initView(){
        mUrl = findViewById(R.id.add_apps_edt);
        mAdd = findViewById(R.id.add_button);
        mClean = findViewById(R.id.add_apps_clean);
    }

    private void initListener(){
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = mUrl.getText().toString();
                if(!StringUtil.isNullOrEmpty(url)){
                    Intent intent = new Intent();
                    intent.putExtra("result", url);
                    setResult(RESULT_OK, intent);
                }
                finish();
            }
        });

        mClean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = BRClipboardManager.getClipboard(AddAppsActivity.this);
                if(!StringUtil.isNullOrEmpty(content)){
                    mUrl.setText(content);
                }
            }
        });
    }
}
