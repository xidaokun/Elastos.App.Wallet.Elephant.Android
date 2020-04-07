package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.LoadingDialog;
import com.breadwallet.presenter.customviews.NoScrollListView;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.wallet.wallets.ela.ElaDataSource;
import com.breadwallet.wallet.wallets.ela.ElaDataUtils;
import com.breadwallet.wallet.wallets.ela.WalletElaManager;
import com.breadwallet.wallet.wallets.ela.response.create.ElaAttribute;
import com.breadwallet.wallet.wallets.ela.response.create.ElaTransaction;
import com.breadwallet.wallet.wallets.ela.response.create.ElaTransactionRes;
import com.elastos.jni.AuthorizeManager;
import com.elastos.jni.Utility;
import com.elastos.jni.utils.SchemeStringUtils;
import com.google.gson.Gson;
import com.platform.APIClient;

import org.elastos.sdk.keypair.ElastosKeypairSign;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MultiSignTxActivity extends BRActivity {

    private final String TAG = "MultiSignTxActivity";

    private Button mAcceptBtn;
    private TextView mBalanceText;
    private NoScrollListView mListView;

    private LoadingDialog mLoadingDialog;

    private int mRequiredCount;
    private String[] mPublicKeys;
    private String mTransaction;
    private String mAddress;

    private String mReturnUrl;
    private String mCallbackUrl;

    private String mMyPublicKey;
    private int mMyIndex = -1;

    private boolean mSend = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_sign_tx);

        if (!initData()) {
            finish();
            return;
        }

        initView();
    }

    private boolean initData() {
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null) return false;

        Log.d(TAG, "uri: " + uri.toString());

        if (!uri.getScheme().equals("elaphant")) {
            uri = readTxFromFile(uri);
            if (uri == null) return false;
        }

        String appName = null;
        try {
            appName = URLDecoder.decode(uri.getQueryParameter("AppName"), "utf-8");
        } catch (UnsupportedEncodingException | UnsupportedOperationException | NullPointerException e) {
            e.printStackTrace();
            return false;
        }
        String appID = uri.getQueryParameter("AppID");
        String publicKey = uri.getQueryParameter("PublicKey");
        String did = uri.getQueryParameter("DID");
        if (!AuthorizeManager.verify(this, did, publicKey, appName, appID)) {
            Log.e(TAG, "check app ID failed!");
            return false;
        }

        try {
            mTransaction = URLDecoder.decode(uri.getQueryParameter("Tx"), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (StringUtil.isNullOrEmpty(mTransaction)) {
            Log.e(TAG, "transaction is empty");
            return false;
        }
        Log.d(TAG, "tx: " + mTransaction);

        String returnurl = uri.getQueryParameter("ReturnUrl");
        if(!StringUtil.isNullOrEmpty(returnurl)) {
            mReturnUrl = URLDecoder.decode(returnurl);
        }

        String callbackurl = uri.getQueryParameter("CallbackUrl");
        if(!StringUtil.isNullOrEmpty(callbackurl)) {
            mCallbackUrl = URLDecoder.decode(callbackurl);
        }

        String mn = getMn();
        if (!SchemeStringUtils.isNullOrEmpty(mn)) {
            mMyPublicKey = Utility.getInstance(this).getSinglePublicKey(mn);
        }

//        mMyPublicKey = WalletElaManager.getInstance(this).getPublicKey();
        // if public key is null, finish and return.
        // The app will show authentication screen
        if (mMyPublicKey == null) {
            return false;
        }

        return true;
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

    private void initView() {
        ElaTransactionRes res = new Gson().fromJson(mTransaction, ElaTransactionRes.class);
        ElaTransaction tx = res.Transactions.get(0);

        String pref = BRSharedPrefs.getMultiSignInfo(this, tx.UTXOInputs.get(0).address);
        if(StringUtil.isNullOrEmpty(pref)) {
            Log.e(TAG, tx.UTXOInputs.get(0).address + " is not created");
            UiUtils.toast(getApplicationContext(), R.string.multisign_wallet_not_created);
            finish();
            return;
        }

        MultiSignCreateActivity.MultiSignParam param = new Gson().fromJson(pref, MultiSignCreateActivity.MultiSignParam.class);
        mRequiredCount = param.RequiredCount;
        mPublicKeys = param.PublicKeys;


        for (int i = 0; i < mPublicKeys.length; i++) {
            if (mMyPublicKey.equals(mPublicKeys[i])) {
                mMyIndex = i;
                break;
            }
        }
        if (mMyIndex < 0) {
            Log.e(TAG, "my public key is not in the pulickey list");
            UiUtils.toast(getApplicationContext(), R.string.multisign_tx_pubkey_not_in_list);
            finish();
            return;
        }

        if (initListView()) {
            startNextAndFinish("", "");
            return;
        }

        TextView from = mListView.findViewById(R.id.multisign_tx_from);
        TextView to = mListView.findViewById(R.id.multisign_tx_to);
        TextView memo = mListView.findViewById(R.id.multisign_tx_memo);

        TextView amount = findViewById(R.id.multisign_tx_amount);
        TextView toAmount = mListView.findViewById(R.id.multisign_tx_to_amount);

        TextView signedText = mListView.findViewById(R.id.multisign_tx_signed_text);

        mBalanceText = mListView.findViewById(R.id.multisign_tx_balance);

        double ela =  (double) tx.Outputs.get(0).amount / 100000000L;
        DecimalFormat df = new DecimalFormat(",###.########");
        String amountStr = df.format(ela) + " ELA";
        amount.setText(amountStr);

        mAddress = tx.UTXOInputs.get(0).address;
        from.setText(mAddress);
        to.setText(tx.Outputs.get(0).address);
        toAmount.setText(amountStr);
        if (tx.Memo != null && !tx.Memo.isEmpty()) {
            int index = tx.Memo.indexOf("msg:");
            memo.setText(getString(R.string.TransactionDetails_commentsHeader)
                    + ": " + tx.Memo.substring(index < 0 ? 0 : index + 4));
            memo.setVisibility(View.VISIBLE);
        } else if (tx.Attributes != null && tx.Attributes.size() != 0) {
            for (ElaAttribute attr : tx.Attributes) {
                if (attr.usage != 0x81) continue;

                try {
                    String str = new String(hexStringToByteArray(attr.data), "UTF-8");
                    int index = str.indexOf("msg:");
                    memo.setText(getString(R.string.TransactionDetails_commentsHeader)
                            + ": " + str.substring(index < 0 ? 0 : index + 4));
                    memo.setVisibility(View.VISIBLE);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                break;
            }
        }

        signedText.setText(String.format(getResources().getString(R.string.multisign_signed_title),
                mRequiredCount, mPublicKeys.length));


        Button denyBtn = findViewById(R.id.multisign_tx_deny);
        denyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mAcceptBtn = findViewById(R.id.multisign_tx_approve);
        mAcceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                auth();
            }
        });

        mAcceptBtn.setText(mSend ? R.string.multisign_approve_send : R.string.multisign_approve);

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mLoadingDialog = new LoadingDialog(this, R.style.progressDialog);
        mLoadingDialog.setCanceledOnTouchOutside(false);

        getBalance();
    }

    private boolean initListView() {
        mListView = findViewById(R.id.multisign_tx_pubkey_list);
        View headerView = LayoutInflater.from(this).inflate(R.layout.multi_sign_tx_header, mListView, false);
        mListView.addHeaderView(headerView);

        Integer outLength = 0;
        String[] signedSigners = ElastosKeypairSign.getSignedSigners(mTransaction, outLength);

        mSend = signedSigners != null && signedSigners.length >= mRequiredCount - 1;

        //TODO: for debug,remove later
        if (signedSigners != null) {
            for(String signed : signedSigners) {
                Log.d(TAG, "signed: " + signed);
            }
        }

        ArrayList<PublicKeyAdapter.PublicKey> publicKeys = new ArrayList<>();
        for (int i = 0; i < mPublicKeys.length; i++) {
            Log.d(TAG, "publicKey: " + mPublicKeys[i]);
            String str = mPublicKeys[i];
            if (i == mMyIndex) {
                str += getString(R.string.multisign_wallet_pubkey_me);
            }

            boolean signed = false;
            if (signedSigners != null) {
                for (String signedSigner : signedSigners) {
                    if (mMyPublicKey.equals(signedSigner)) {
                        return true;
                    }
                    if (signedSigner.equals(mPublicKeys[i])) {
                        signed = true;
                        break;
                    }
                }

            }

            PublicKeyAdapter.PublicKey pubObj = new PublicKeyAdapter.PublicKey();
            pubObj.mPublicKey = str;
            pubObj.mSigned = signed;
            publicKeys.add(pubObj);
        }

        ArrayAdapter adapter = new PublicKeyAdapter(this, R.layout.publickey_label, publicKeys);
        mListView.setAdapter(adapter);

        return false;
    }

    private Uri readTxFromFile(Uri uri) {
        InputStream in = null;
        BufferedReader reader = null;
        try {
            in = getContentResolver().openInputStream(uri);
            reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return Uri.parse(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(reader != null) reader.close();
                if(in != null) in.close();
            } catch (Exception e) {

            }

        }

        return null;
    }

    private void getBalance() {
        if (StringUtil.isNullOrEmpty(mAddress)) return;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String url = ElaDataUtils.getUrl(MultiSignTxActivity.this, "balance/" + mAddress);
                try {
                    String result = APIClient.urlGET(MultiSignTxActivity.this, url);
                    JSONObject jsonObject = new JSONObject(result);
                    final String balance = jsonObject.getString("result");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBalanceText.setText(getResources().getString(R.string.multisign_balance) + " " + balance + " ELA");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    private void auth() {
        Log.d(TAG, "enter auth");
        AuthManager.getInstance().authPrompt(this, getString(R.string.VerifyPin_title),
                getString(R.string.VerifyPin_authorize), true, false, new BRAuthCompletion() {
            @Override
            public void onComplete() {
                showDialog();
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        approve();
                    }
                });
            }

            @Override
            public void onCancel() {
                //nothing
            }
        });
    }

    private void approve() {
        String privateKey = WalletElaManager.getInstance(this).getPrivateKey();
        String signed =  ElastosKeypairSign.multiSignTransaction(privateKey, mPublicKeys,
                mPublicKeys.length, mRequiredCount, mTransaction);
        Log.d(TAG, "signed: " + signed);
        if (!mSend) {
            startNextAndFinish(signed, "");
            return;
        }

        String serialize = ElastosKeypairSign.serializeMultiSignTransaction(signed);
        Log.d(TAG, "serialize: " + serialize);
        if (!StringUtil.isNullOrEmpty(serialize)) {
            String txid = ElaDataSource.getInstance(this).sendSerializedRawTx(serialize);
            Log.d(TAG, "txid:" + txid);
            closeDialog();
            if (!StringUtil.isNullOrEmpty(txid)) {
                startNextAndFinish("", txid);
                return;
            }
        }

        BRDialog.showSimpleDialog(this, getString(R.string.Alerts_sendFailure), "Failed to send transaction");
    }

    private void startNextAndFinish(String tx, String txid) {
        Intent intent = new Intent();
        intent.setClass(this, MultiSignQrActivity.class);
        if (!StringUtil.isNullOrEmpty(tx)) {
            intent.putExtra("tx", tx);
        }
        if (!StringUtil.isNullOrEmpty(txid)) {
            intent.putExtra("txid", txid);
        }
        startActivity(intent);
        finish();
    }

    private void showDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && mLoadingDialog != null)
                    mLoadingDialog.show();
            }
        });
    }


    private void closeDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(null != mLoadingDialog && mLoadingDialog.isShowing()){
                    mLoadingDialog.hide();
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(null != mLoadingDialog){
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }
}
