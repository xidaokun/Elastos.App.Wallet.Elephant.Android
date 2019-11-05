package com.breadwallet.presenter.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;

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
import java.util.Locale;

public class AppAboutActivity extends BaseSettingsActivity {

    private RoundImageView mLogo;
    private BaseTextView mName;
    private BaseTextView mDesc;
//    private BaseTextView mDeveloper;
    private BaseTextView mDid;
    private RecyclerView mRecycleView;
    private AppAboutAdapter mAdapter;
    private BaseTextView mTitle;

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
//        mDeveloper = findViewById(R.id.explore_about_developer);
        mDid = findViewById(R.id.explore_about_did);
        mTitle = findViewById(R.id.title);
        mRecycleView = findViewById(R.id.exploer_about_rv);
    }

    private void initData(){
        String appId = getIntent().getStringExtra("appId");
        if(!StringUtil.isNullOrEmpty(appId)){
            MyAppItem myAppItem = ProfileDataSource.getInstance(this).getAppInfoById(appId);
            if(myAppItem == null){
                return;
            }
            mTitle.setText(String.format(getString(R.string.explore_pop_about), myAppItem.name_en));
            String languageCode = Locale.getDefault().getLanguage();
            if(!StringUtil.isNullOrEmpty(languageCode) && languageCode.contains("zh")){
                mName.setText(myAppItem.name_zh_CN);
                mDesc.setText(myAppItem.shortDesc_zh_CN);
            } else {
                mName.setText(myAppItem.name_en);
                mDesc.setText(myAppItem.shortDesc_en);
            }
            if(StringUtil.isNullOrEmpty(myAppItem.name_zh_CN) && StringUtil.isNullOrEmpty(myAppItem.name_en))
                mName.setText(myAppItem.name);
            if(StringUtil.isNullOrEmpty(myAppItem.shortDesc_zh_CN) && StringUtil.isNullOrEmpty(myAppItem.shortDesc_en))
                mDesc.setText(myAppItem.shortDesc);

            mDid.setText(String.format(getString(R.string.explore_about_developer), myAppItem.did));
            Bitmap bitmap = null;
            if(!StringUtil.isNullOrEmpty(myAppItem.icon)){
                bitmap = Utils.getIconFromPath(new File(myAppItem.icon));
            }
            if(null != bitmap){
                mLogo.setImageBitmap(bitmap);
            } else {
                mLogo.setImageResource(R.drawable.unknow);
            }

            List<AppAboutItem> appAboutItems = new ArrayList<>();
            AppAboutItem appIdItem = new AppAboutItem();
            appIdItem.setTitle(getString(R.string.mini_app_app_id));
            appIdItem.setContent(myAppItem.appId);
            appAboutItems.add(appIdItem);

            AppAboutItem websiteItem = new AppAboutItem();
            websiteItem.setTitle(getString(R.string.mini_app_website));
            websiteItem.setContent(myAppItem.url);
            appAboutItems.add(websiteItem);

            AppAboutItem nameItem = new AppAboutItem();
            nameItem.setTitle(getString(R.string.mini_app_name));
            if(!StringUtil.isNullOrEmpty(languageCode) && languageCode.contains("zh")){
                nameItem.setContent(myAppItem.name_zh_CN);
            } else {
                nameItem.setContent(myAppItem.name_en);
            }
            appAboutItems.add(nameItem);

            AppAboutItem publickeyItem = new AppAboutItem();
            publickeyItem.setTitle(getString(R.string.mini_app_publickey));
            publickeyItem.setContent(myAppItem.publicKey);
            appAboutItems.add(publickeyItem);

            AppAboutItem categoryItem = new AppAboutItem();
            categoryItem.setTitle(getString(R.string.mini_app_category));
            categoryItem.setContent(myAppItem.category);
            appAboutItems.add(categoryItem);

            AppAboutItem platformItem = new AppAboutItem();
            platformItem.setTitle(getString(R.string.mini_app_platform));
            platformItem.setContent(myAppItem.platform);
            appAboutItems.add(platformItem);

            AppAboutItem versionItem = new AppAboutItem();
            versionItem.setTitle(getString(R.string.mini_app_version));
            versionItem.setContent(myAppItem.version);
            appAboutItems.add(versionItem);

            AppAboutItem shortdescItem = new AppAboutItem();
            shortdescItem.setTitle(getString(R.string.mini_app_shortdesc));
            if(StringUtil.isNullOrEmpty(myAppItem.shortDesc_zh_CN) && StringUtil.isNullOrEmpty(myAppItem.shortDesc_en)) {
                shortdescItem.setContent(myAppItem.shortDesc);
            } else {
                if(!StringUtil.isNullOrEmpty(languageCode) && languageCode.contains("zh")){
                    shortdescItem.setContent(myAppItem.shortDesc_zh_CN);
                } else {
                    shortdescItem.setContent(myAppItem.shortDesc_en);
                }
            }
            appAboutItems.add(shortdescItem);

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
