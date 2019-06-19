package com.breadwallet.presenter.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.customviews.RoundImageView;
import com.breadwallet.presenter.entities.AppAboutItem;
import com.breadwallet.presenter.entities.MyAppItem;
import com.breadwallet.tools.adapter.AppAboutAdapter;
import com.breadwallet.tools.sqlite.ProfileDataSource;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppAboutActivity extends BaseSettingsActivity {

    private RoundImageView mLogo;
    private BaseTextView mName;
    private BaseTextView mDesc;
    private BaseTextView mDeveloper;
    private BaseTextView mDid;
    private RecyclerView mRecycleView;
    private AppAboutAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
    }

    private void initView(){
        mLogo = findViewById(R.id.explore_about_logo);
        mName = findViewById(R.id.explore_about_name);
        mDesc = findViewById(R.id.explore_about_desc);
        mDeveloper = findViewById(R.id.explore_about_developer);
        mDid = findViewById(R.id.explore_about_did);
        mRecycleView = findViewById(R.id.exploer_about_rv);
    }

    private void initData(){
        String appId = getIntent().getStringExtra("appId");
        if(!StringUtil.isNullOrEmpty(appId)){
            MyAppItem myAppItem = ProfileDataSource.getInstance(this).getAppInfoById(appId);
            mName.setText(myAppItem.name);
            mDesc.setText(myAppItem.shortDesc);
            mDeveloper.setText(String.format(getString(R.string.explore_about_developer), myAppItem.developer));
            mDid.setText(String.format(getString(R.string.explore_about_did), myAppItem.did));
            Bitmap bitmap = null;
            if(!StringUtil.isNullOrEmpty(myAppItem.path)){
                bitmap = Utils.getIconFromPath(new File(myAppItem.path+"/"+myAppItem.icon_xxhdpi));
            }
            mLogo.setImageBitmap(bitmap);

            List<AppAboutItem> appAboutItems = new ArrayList<>();
            AppAboutItem appAboutItem = new AppAboutItem();
            appAboutItem.setTitle("App ID");
            appAboutItem.setContent(myAppItem.appId);
            appAboutItems.add(appAboutItem);

            mAdapter = new AppAboutAdapter(this, appAboutItems);
            mRecycleView.setLayoutManager(new LinearLayoutManager(this));
            mRecycleView.setAdapter(mAdapter);
        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_app_about_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }
}
