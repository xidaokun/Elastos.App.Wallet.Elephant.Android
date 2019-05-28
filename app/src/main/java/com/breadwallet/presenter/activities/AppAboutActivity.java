package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.adapter.AppAboutAdapter;

public class AppAboutActivity extends BaseSettingsActivity {

    private BaseTextView mLogo;
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
//        String from = BRSharedPrefs.getExploreFrom(this);
//        if(StringUtil.isNullOrEmpty(from)) return;
//        if(from.equalsIgnoreCase("vote")){
//            mDes1.setText(getString(R.string.explore_vote_about_desc1));
//            mDes2.setText(getString(R.string.explore_vote_about_desc2));
//        } else if(from.equalsIgnoreCase("redpacket")){
//            mDes1.setText(getString(R.string.redpackage_hint1));
//            mDes2.setText(getString(R.string.redpackage_hint2));
//        } else if(from.equalsIgnoreCase("exchange")){
//            mDes1.setText(getString(R.string.exchange_hint1));
//            mDes2.setText(getString(R.string.exchange_hint2));
//        } else {
//            mDes1.setText("");
//            mDes2.setText("");
//        }
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
