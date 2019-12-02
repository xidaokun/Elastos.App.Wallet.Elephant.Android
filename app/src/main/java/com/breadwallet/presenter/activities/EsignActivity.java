package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BREdit;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.SignHistoryItem;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.sqlite.EsignDataSource;
import com.breadwallet.tools.util.StringUtil;
import com.elastos.jni.AuthorizeManager;
import com.elastos.jni.Utility;
import java.util.Calendar;
import java.util.Date;

public class EsignActivity extends BaseSettingsActivity {

    private BRButton mSignBtn;
    private BREdit mSignEdt;
    private BaseTextView mHistoryBtn;
    private BaseTextView mPasteBtn;
    private BaseTextView mCheckBox;

    @Override
    public int getLayoutId() {
        return R.layout.activity_esign_layout;
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

    private void initView() {
        mSignBtn = findViewById(R.id.sign_btn);
        mSignEdt = findViewById(R.id.doc_to_sign_content);
        mCheckBox = findViewById(R.id.esign_check_box);
        mHistoryBtn = findViewById(R.id.esign_history);
        mPasteBtn = findViewById(R.id.esign_paste_btn);
        mSignBtn.setColor(getColor(R.color.esigin_btn_unable_color));
        boolean isEmpty = StringUtil.isNullOrEmpty(mSignEdt.getText().toString());
        mSignBtn.setColor(getColor(isEmpty ? R.color.esigin_btn_unable_color : R.color.tx_send_color));
    }

    private boolean mIsSigning = false;

    private void initListener() {
        mSignEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String tmp = mSignEdt.getText().toString();
                boolean isEmpty = StringUtil.isNullOrEmpty(tmp);
                mSignBtn.setColor(getColor(isEmpty?R.color.esigin_btn_unable_color:R.color.tx_send_color));
            }
        });

        mSignBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isEmpty = StringUtil.isNullOrEmpty(mSignEdt.getText().toString());
                if (!mIsSigning) {
                    if (!isEmpty) {
                        signData();
                    } else {
                        Toast.makeText(EsignActivity.this, getString(R.string.esign_please_contnet_empty_hint), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        mHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.startSignHistoryActivity(EsignActivity.this);
            }
        });

        mPasteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = BRClipboardManager.getClipboard(EsignActivity.this);
                if (!StringUtil.isNullOrEmpty(content)) {
                    mSignEdt.setText(content);
                }
            }
        });

//        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                mSignBtn.setColor(getColor(!isChecked?R.color.esigin_btn_unable_color:R.color.tx_send_color));
//            }
//        });
    }

    private void signData() {
//        if(!mCheckBox.isChecked()){
//            return;
//        }
        mIsSigning = true;
        try {
            String mn = getMn();
            if(StringUtil.isNullOrEmpty(mn)) {
                finish();
                return;
            }
            String pk = Utility.getInstance(this).getSinglePrivateKey(mn);
            String source = mSignEdt.getText().toString();
            if (StringUtil.isNullOrEmpty(mn)
                    || StringUtil.isNullOrEmpty(pk)
                    || StringUtil.isNullOrEmpty(source)) {
                return;
            }
            String target = AuthorizeManager.sign(this, pk, source);
            SignHistoryItem item = new SignHistoryItem();
            item.signData = source;
            item.signedData = target;
            item.time = getAuthorTime(0);
            cacheData(item);
            UiUtils.startSignSuccessActivity(this, item.signedData);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mIsSigning = false;
        }
    }

    private void cacheData(SignHistoryItem item) {
        EsignDataSource.getInstance(this).putSignData(item);
    }

    private long getAuthorTime(int day) {
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(calendar.DATE, day);
        date = calendar.getTime();
        long time = date.getTime();

        return time;
    }

    private String getMn() {
        byte[] phrase = null;
        try {
            phrase = BRKeyStore.getPhrase(this, 0);
            if (phrase != null) {
                return new String(phrase);
            }
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
