package com.breadwallet.presenter.activities.moment;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;

public class MyMomentActivity extends BRActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_moment_layout);
    }
}