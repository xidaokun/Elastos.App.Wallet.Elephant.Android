
package com.breadwallet.presenter.activities.intro;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.InputPinActivity;
import com.breadwallet.presenter.activities.WalletNameActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRPublicSharedPrefs;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.security.SmartValidator;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.io.File;
import java.util.Map;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/4/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class IntroActivity extends BRActivity {
    private static final String TAG = IntroActivity.class.getName();
    private Button mNewWalletButton;
    private Button mRecoverWalletButton;

    public static final String INTRO_REENTER = "intro.reenter";

    private boolean mReenter = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            if (!StringUtil.isNullOrEmpty(action) && action.equals(Intent.ACTION_VIEW)) {
                Uri uri = intent.getData();
                PostAuth.getInstance().onCanaryCheck(IntroActivity.this, false, uri.toString());
                finish();
                return;
            }
        }

        setContentView(R.layout.activity_intro);
        mNewWalletButton = findViewById(R.id.button_new_wallet);
        mRecoverWalletButton = findViewById(R.id.button_recover_wallet);
        TextView subtitle = findViewById(R.id.intro_subtitle);


//        String aa = android.os.Build.CPU_ABI;
//        if(!"armeabi-v7a".equals(android.os.Build.CPU_ABI)){
//            BRDialog.showSimpleDialog(this, "Incompatible", "not support "+android.os.Build.CPU_ABI);
//            return;
//        }
        setListeners();
//        updateBundles();
        ImageButton faq = findViewById(R.id.faq_button);

        Shader shader = new LinearGradient(
                90, 0, 100, 100,
                getResources().getColor(R.color.button_gradient_start_color, null), getResources().getColor(R.color.button_gradient_end_color, null),
                Shader.TileMode.CLAMP);

        subtitle.getPaint().setShader(shader);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                BaseWalletManager wm = WalletsMaster.getInstance(IntroActivity.this).getCurrentWallet(IntroActivity.this);
                UiUtils.showSupportFragment(IntroActivity.this, BRConstants.FAQ_START_VIEW, wm);
            }
        });

        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);

        if (Utils.isEmulatorOrDebug(this)) {
            Utils.printPhoneSpecs();
        }

        initGlobal();

        byte[] masterPubKey = BRKeyStore.getMasterPublicKey(this);

        boolean isFirstAddressCorrect = false;
        if (masterPubKey != null && masterPubKey.length != 0) {
            isFirstAddressCorrect = SmartValidator.checkFirstAddress(this, masterPubKey);
        }
        if (!isFirstAddressCorrect) {
            WalletsMaster.getInstance(this).wipeWalletButKeystore(this);
        }

        mReenter = getIntent().getBooleanExtra(INTRO_REENTER, false);
        if (mReenter) return;

        PostAuth.getInstance().onCanaryCheck(IntroActivity.this, false);

//        checkPermisson();
    }

//    private void checkPermisson(){
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    0x01);
//        }
//    }

//    private void updateBundles() {
//        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
//            @Override
//            public void run() {
//                final long startTime = System.currentTimeMillis();
//                APIClient apiClient = APIClient.getInstance(IntroActivity.this);
//                apiClient.updateBundle();
//                long endTime = System.currentTimeMillis();
//                Log.d(TAG, "updateBundle DONE in " + (endTime - startTime) + "ms");
//            }
//        });
//    }

    private void setListeners() {
        mNewWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) {
                    return;
                }

                String pin = BRKeyStore.getPinCode(IntroActivity.this);
                if (mReenter || !pin.isEmpty()) {
                    UiUtils.startWalletNameActivity(IntroActivity.this, WalletNameActivity.WALLET_NAME_TYPE_NEW, mReenter);
                    return;
                }

                Intent intent = new Intent(IntroActivity.this, InputPinActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                startActivityForResult(intent, InputPinActivity.SET_PIN_REQUEST_CODE);
            }
        });

        mRecoverWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) {
                    return;
                }

                UiUtils.startWalletNameActivity(IntroActivity.this, WalletNameActivity.WALLET_NAME_TYPE_RECOVER, mReenter);
            }
        });
    }


    private void initGlobal() {
        byte[] phrase;
        try {
            phrase = BRKeyStore.getPhrase(this, BRConstants.INIT_GLOBAL_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            return;
        }

        if (phrase == null) return;
        if(UiUtils.isSingleWallet(this, phrase) &&
                StringUtil.isNullOrEmpty(BRSharedPrefs.getSingleWalletHash(this))) {
            BRPublicSharedPrefs.putUseFingerprint(this, BRSharedPrefs.getUseFingerprint(this));
            BRSharedPrefs.setSingleWalletHash(this,  UiUtils.getSha256(phrase));
        }

        UiUtils.setStorageName(phrase);
    }



    private void backupSp(byte[] phrase) {
        Map<String, ?> srcData = BRSharedPrefs.getAll(this, "MyPrefsFile");
        String hash = UiUtils.getSha256(phrase);
        for(Map.Entry<String, ?>  entry : srcData.entrySet()){
            BRSharedPrefs.putAll(this, "profile_" + hash, entry.getKey(), entry.getValue());
        }
    }

    private void backupDatabase(byte[] phrase) {
        File path = new File(getFilesDir().getParent(), "databases");
        String hash = UiUtils.getSha256(phrase);
        Utils.copyFile(new File(path, "breadwallet.db"), path, hash + ".db");
        Log.i("tag", "tag");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult " + requestCode + " " + resultCode);

        if (requestCode == BRConstants.INIT_GLOBAL_REQUEST_CODE && resultCode == RESULT_OK) {
            initGlobal();
        }
    }

}
