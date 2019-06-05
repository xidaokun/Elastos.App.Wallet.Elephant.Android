package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.entities.SignHistoryItem;
import com.breadwallet.tools.adapter.SignHistoryAdapter;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.sqlite.EsignDataSource;

import java.util.List;

public class EsignHistoryActivity extends BaseSettingsActivity {

    private ListView mHistoryLv;
    private SignHistoryAdapter mAdapter;

    @Override
    public int getLayoutId() {
        return R.layout.activity_esign_history_layout;
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
        mHistoryLv = findViewById(R.id.signature_history_lv);
    }

    private void initListener() {
        mHistoryLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SignHistoryItem signHistoryItem = mData.get(position);
                UiUtils.startSignDetailActivity(EsignHistoryActivity.this, signHistoryItem.signedData, signHistoryItem.signData);
            }
        });
    }

    List<SignHistoryItem> mData;
    private void initData() {
        mData = EsignDataSource.getInstance(this).getSignData();
        if (null != mData) {
            mAdapter = new SignHistoryAdapter(this, mData);
            mHistoryLv.setAdapter(mAdapter);
        }
    }
}
