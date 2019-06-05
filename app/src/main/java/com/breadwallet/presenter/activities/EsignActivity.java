package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.View;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BREdit;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.SignHistoryItem;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.sqlite.EsignDataSource;
import com.breadwallet.tools.util.StringUtil;
import com.elastos.jni.Utility;

import org.wallet.library.AuthorizeManager;

import java.util.Calendar;
import java.util.Date;

public class EsignActivity extends BaseSettingsActivity {

    private BRButton mSignBtn;
    private BREdit mSignEdt;
    private BaseTextView mHistoryBtn;
    private AppCompatCheckBox mCheckBox;

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
    }

    private boolean mIsSigning = false;
    private void initListener() {
        mSignBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mIsSigning) signData();
            }
        });

        mHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.startSignHistoryActivity(EsignActivity.this);
            }
        });
    }

   private void signData(){
        if(!mCheckBox.isChecked()){
            Toast.makeText(this, "还未勾选", Toast.LENGTH_SHORT).show();
            return;
        }
       mIsSigning = true;
       try {
           String mn = getMn();
           String pk = Utility.getInstance(this).getSinglePrivateKey(mn);
           String source = mSignEdt.getText().toString();
           if(StringUtil.isNullOrEmpty(mn)
                   || StringUtil.isNullOrEmpty(pk)
                   || StringUtil.isNullOrEmpty(source)){
               return;
           }
           String target = AuthorizeManager.sign(this, pk, mn);
           SignHistoryItem item = new SignHistoryItem();
           item.signData = source;
           item.signedData = target;
           item.time = getAuthorTime(0);
           cacheData(item);
           Toast.makeText(this, "签名完成", Toast.LENGTH_SHORT).show();
       } catch (Exception e) {
           e.printStackTrace();
       } finally {
           mIsSigning = false;
       }
   }

   private void cacheData(SignHistoryItem item){
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
