package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.presenter.activities.settings.UnlinkActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.adapter.PhraseAdapter;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.security.PhraseInfo;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PhraseListActivity extends BRActivity {
    private final String TAG = PhraseListActivity.class.getName();

    private PhraseAdapter mAdapter;

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

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PhraseListActivity.this, IntroActivity.class);
                intent.putExtra(IntroActivity.INTRO_REENTER, true);
                startActivity(intent);
                finish();
            }
        });

        AuthManager.getInstance().authPrompt(PhraseListActivity.this, null,
            getString(R.string.VerifyPin_continueBody), true, false, new BRAuthCompletion() {
                @Override
                public void onComplete() {
                    initList();
                }

                @Override
                public void onCancel() {
                    finish();
                }
            });

    }

    private void initList() {
        ListView mListView = findViewById(R.id.multiwallet_phrase_list);

        List<PhraseInfo> list = null;
        try {
            list = BRKeyStore.getPhraseInfoList(this);
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
                        info.alias = "current";
                        break;
                    }
                }

            } catch (UserNotAuthenticatedException e) {
                e.printStackTrace();
            }
        }
        mAdapter = new PhraseAdapter(this, R.layout.phrase_item, list);

        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!UiUtils.isClickAllowed()) return;
                final PhraseInfo info = mAdapter.getItem(position);
                assert info != null;
                BRDialog.showCustomDialog(PhraseListActivity.this, "切换至", new String(info.phrase),
                        getString(R.string.Button_ok), getString(R.string.JailbreakWarnings_close), new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                recoverTo(info);
                                brDialogView.dismissWithAnimation();
                            }
                        }, new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                            }
                        }, null, 0);
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                unlink(position);
                return true;
            }
        });
    }

    private void recoverTo(final PhraseInfo info) {
        AuthManager.getInstance().authPrompt(PhraseListActivity.this, null,
                getString(R.string.VerifyPin_continueBody), true, false, new BRAuthCompletion() {
                    @Override
                    public void onComplete() {
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
                            UiUtils.switchPhrase(PhraseListActivity.this, new String(info.phrase), true);
                        }
                    }

                    @Override
                    public void onCancel() {
                        finish();
                    }
                });
    }

    private void unlink(final int position) {
        AuthManager.getInstance().authPrompt(PhraseListActivity.this, null,
                getString(R.string.VerifyPin_continueBody), true, false, new BRAuthCompletion() {
                    @Override
                    public void onComplete() {
                        final PhraseInfo info = mAdapter.getItem(position);
                        assert info != null;
                        BRDialog.showCustomDialog(PhraseListActivity.this, "解绑该助记词", new String(info.phrase),
                                getString(R.string.Button_ok), getString(R.string.JailbreakWarnings_close), new BRDialogView.BROnClickListener() {
                                    @Override
                                    public void onClick(BRDialogView brDialogView) {
                                        brDialogView.dismissWithAnimation();
                                        Intent intent = new Intent(PhraseListActivity.this, UnlinkActivity.class);
                                        intent.putExtra(UnlinkActivity.UNLINK_PHARE, info.phrase);
                                        startActivity(intent);
                                        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                                        finish();
                                    }
                                }, new BRDialogView.BROnClickListener() {
                                    @Override
                                    public void onClick(BRDialogView brDialogView) {
                                        brDialogView.dismissWithAnimation();
                                    }
                                }, null, 0);

                    }

                    @Override
                    public void onCancel() {
                        finish();
                    }
                });
    }

}
