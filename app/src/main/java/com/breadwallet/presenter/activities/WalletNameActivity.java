package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.presenter.activities.intro.RecoverActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BREdit;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.util.StringUtil;

public class WalletNameActivity extends BRActivity {
    private static final String TAG = WalletNameActivity.class.getName();

    public static final String WALLET_NAME = "wallet.name";
    public static final String WALLET_NAME_PAGE_TYPE = "wallet.name_page_type";
    public static final String WALLET_SELECTED = "wallet.selected";

    public static final int WALLET_NAME_TYPE_RENAME = 0;
    public static final int WALLET_NAME_TYPE_NEW = 1;
    public static final int WALLET_NAME_TYPE_RECOVER = 2;


    public static final int REQUEST_WALLET_RENAME = 20005;

    private BREdit mEdit;
    private BaseTextView mClean;
    private int mType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_name);

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        initView();
    }

    boolean mIsSelected = false;
    private void initView() {
        Intent intent = getIntent();
        mType = intent.getIntExtra(WALLET_NAME_PAGE_TYPE, WALLET_NAME_TYPE_RENAME);
        mIsSelected = intent.getBooleanExtra(WALLET_SELECTED, false);
        String defaultName = intent.getStringExtra(WALLET_NAME);


        TextView save = findViewById(R.id.save_button);
        if (mType == WALLET_NAME_TYPE_RENAME) {
            save.setVisibility(View.VISIBLE);
            save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setResultAndFinish();
                }
            });
        } else {
            save.setVisibility(View.GONE);
        }

        BRButton next = findViewById(R.id.wallet_name_next);
        if (mType == WALLET_NAME_TYPE_RENAME) {
            next.setVisibility(View.GONE);
        } else {
            next.setVisibility(View.VISIBLE);
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setResultAndFinish();
                }
            });
        }

        mEdit = findViewById(R.id.wallet_name_edit);
        if (!StringUtil.isNullOrEmpty(defaultName)) {
            mEdit.setText(defaultName);
        }
        mEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = mEdit.getText().toString();
                mClean.setVisibility(StringUtil.isNullOrEmpty(text) ? View.GONE : View.VISIBLE);
            }
        });

        mClean = findViewById(R.id.wallet_name_clean);
        mClean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEdit.setText("");
            }
        });
    }

    private void setResultAndFinish() {
        if (!UiUtils.isClickAllowed()) return;

        String name = mEdit.getText().toString();
        Log.d(TAG, "input name: " + name);
        if (StringUtil.isNullOrEmpty(name)) {
            UiUtils.toast(getApplicationContext(), R.string.multi_wallet_name_required);
            return;
        }

        switch (mType) {
            case WALLET_NAME_TYPE_RENAME:
                Intent intent = new Intent();
                intent.putExtra(WALLET_NAME, name);
                setResult(Activity.RESULT_OK, intent);
                finish();
                break;
            case WALLET_NAME_TYPE_NEW:
                PostAuth.getInstance().onCreateWalletAuth(WalletNameActivity.this, false,
                        getIntent().getBooleanExtra(IntroActivity.INTRO_REENTER, false), name);
                break;
            case WALLET_NAME_TYPE_RECOVER:
                Intent recoverIntent = new Intent(WalletNameActivity.this, RecoverActivity.class);
                recoverIntent.putExtra(IntroActivity.INTRO_REENTER, getIntent().getBooleanExtra(IntroActivity.INTRO_REENTER, false));
                recoverIntent.putExtra(WALLET_NAME, name);
                startActivity(recoverIntent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                finish();
                break;
            default:
                Log.e(TAG, "not support page type!");
                break;
        }

    }

}
