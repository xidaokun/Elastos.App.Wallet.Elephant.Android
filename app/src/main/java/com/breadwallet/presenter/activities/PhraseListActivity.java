package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.tools.adapter.PhraseAdapter;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.security.PhraseInfo;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PhraseListActivity extends BRActivity implements PhraseAdapter.WalletCardListener {
    private final String TAG = PhraseListActivity.class.getName();

    private PhraseAdapter mAdapter;
    private int mEditPosition = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phrase_list);

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        initList();
    }

    private void initList() {
        RecyclerView recyclerView = findViewById(R.id.multiwallet_phrase_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        List<PhraseInfo> list = null;
        try {
            list = BRKeyStore.getPhraseInfoList(this, BRConstants.GET_PHRASE_LIST_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        if (list == null) {
            list = new ArrayList<>();
        } else {
            byte[] phrase;
            try {
                phrase = BRKeyStore.getPhrase(this, 0);

                for (PhraseInfo info : list) {
                    if (Arrays.equals(info.phrase, phrase)) {
                        info.selected = true;
                    } else {
                        info.selected = false;
                    }
                }

            } catch (UserNotAuthenticatedException e) {
                e.printStackTrace();
            }
        }
        mAdapter = new PhraseAdapter(this, list,this);

        recyclerView.setAdapter(mAdapter);
    }

    private void recoverTo(final PhraseInfo info) {
        byte[] phrase;
        try {
            phrase = BRKeyStore.getPhrase(PhraseListActivity.this, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            return;
        }

        if (Arrays.equals(phrase, info.phrase)) {
            BRDialog.showCustomDialog(PhraseListActivity.this, "", "已经是当前助记词",
                    getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
        } else {
            UiUtils.switchPhrase(PhraseListActivity.this, new String(info.phrase), true, info.alias);
        }
    }

    @Override
    public void OnItemClick(View v, int position) {
        if (!UiUtils.isClickAllowed()) return;
        final PhraseInfo info = mAdapter.getItem(position);
        assert info != null;
        recoverTo(info);
    }

    @Override
    public void OnEditNameClick(View v, int position) {
        if (!UiUtils.isClickAllowed()) return;

        mEditPosition = position;
        Intent intent = new Intent(PhraseListActivity.this, WalletNameActivity.class);
        intent.putExtra(WalletNameActivity.WALLET_NAME_PAGE_TYPE, WalletNameActivity.WALLET_NAME_TYPE_RENAME);
        intent.putExtra(WalletNameActivity.WALLET_NAME, mAdapter.getItem(position).alias);
        startActivityForResult(intent, WalletNameActivity.REQUEST_WALLET_RENAME);
    }

    @Override
    public void OnNewClick(View v) {
        if (!UiUtils.isClickAllowed()) return;

        Intent intent = new Intent(PhraseListActivity.this, IntroActivity.class);
        intent.putExtra(IntroActivity.INTRO_REENTER, true);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult " + requestCode + " " + resultCode);

        if (requestCode == WalletNameActivity.REQUEST_WALLET_RENAME) {
            int pos = mEditPosition;
            mEditPosition = -1;

            if (resultCode == RESULT_OK) {
                String name = data.getStringExtra(WalletNameActivity.WALLET_NAME);
                if (StringUtil.isNullOrEmpty(name) || pos < 0) return;

                PhraseInfo info = mAdapter.getItem(pos);
                info.alias = name;
                try {
                    BRKeyStore.updatePhraseInfo(PhraseListActivity.this, info);
                    mAdapter.notifyDataSetChanged();
                } catch (UserNotAuthenticatedException e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == BRConstants.GET_PHRASE_LIST_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                initList();
            } else {
                finish();
            }
        }

    }
}
