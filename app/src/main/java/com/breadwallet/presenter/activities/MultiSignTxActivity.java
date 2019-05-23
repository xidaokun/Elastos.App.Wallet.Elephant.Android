package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.LoadingDialog;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.jsbridge.JsInterface;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.wallet.wallets.ela.ElaDataSource;
import com.breadwallet.wallet.wallets.ela.WalletElaManager;
import com.breadwallet.wallet.wallets.ela.response.create.ElaTransactionRes;
import com.breadwallet.wallet.wallets.ela.response.create.ElaTransactions;
import com.google.gson.Gson;

import org.elastos.sdk.keypair.ElastosKeypairSign;
import org.json.JSONObject;
import org.wallet.library.AuthorizeManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
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
    private ListView mListView;

    private LoadingDialog mLoadingDialog;

    private int mRequiredCount;
    private String[] mPublicKeys;
    private String mTransaction;
    private String mAddress;

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
        getBalance();
    }

    private boolean initData() {
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null) return false;

        if (!uri.getScheme().equals("elaphant")) {
            uri = readTxFromFile(uri);
            if (uri == null) return false;
        }

        String appName = uri.getQueryParameter("AppName");
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
        return true;
    }

    private void initView() {


        ElaTransactionRes res = new Gson().fromJson(mTransaction, ElaTransactionRes.class);
        ElaTransactions tx = res.Transactions.get(0);

        String pref = BRSharedPrefs.getMultiSignInfo(this, tx.UTXOInputs.get(0).address);
        if(StringUtil.isNullOrEmpty(pref)) {
            Log.e(TAG, tx.UTXOInputs.get(0).address + " is not created");
            finish();
            return;
        }

        JsInterface.MultiSignParam param = new Gson().fromJson(pref, JsInterface.MultiSignParam.class);
        mRequiredCount = param.RequiredCount;
        mPublicKeys = param.PublicKeys;

        initListView();

        TextView from = mListView.findViewById(R.id.multisign_tx_from);
        TextView to = mListView.findViewById(R.id.multisign_tx_to);
        TextView memo = mListView.findViewById(R.id.multisign_tx_memo);

        TextView amount = findViewById(R.id.multisign_tx_amount);
        TextView toAmount = mListView.findViewById(R.id.multisign_tx_to_amount);

        TextView signedText = mListView.findViewById(R.id.multisign_tx_signed_text);

        mBalanceText = mListView.findViewById(R.id.multisign_tx_balance);

        double ela =  (double) tx.Outputs.get(0).amount / 100000000L;
        DecimalFormat df = new DecimalFormat("#.######");
        String amountStr = df.format(ela) + " ELA";
        amount.setText(amountStr);

        mAddress = tx.UTXOInputs.get(0).address;
        from.setText(mAddress);
        to.setText(tx.Outputs.get(0).address);
        toAmount.setText(amountStr);
        if (tx.Memo != null && !tx.Memo.isEmpty()) {
            int index = tx.Memo.indexOf("msg:");
            memo.setText("memo: " + tx.Memo.substring(index + 4));
            memo.setVisibility(View.VISIBLE);
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
    }

    private void initListView() {
        mListView = findViewById(R.id.multisign_tx_pubkey_list);
        View headerView = LayoutInflater.from(this).inflate(R.layout.multi_sign_tx_header, mListView, false);
        mListView.addHeaderView(headerView);

        String mPublicKey = WalletElaManager.getInstance(this).getPublicKey();
        Integer outLength = 0;
        String[] signedSigners = ElastosKeypairSign.getSignedSigners(mTransaction, outLength);

        mSend = signedSigners != null && signedSigners.length >= mRequiredCount - 1;

        ArrayList<PublicKeyAdapter.PublicKey> publicKeys = new ArrayList<>();
        for (String publicKey : mPublicKeys) {
            String str;
            if (mPublicKey.equals(publicKey)) {
                str = publicKey + "(me)";
            } else {
                str = publicKey;
            }

            boolean signed = false;
            if (signedSigners != null) {
                for (String signedSigner : signedSigners) {
                    if (signedSigner.equals(publicKey)) {
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
    }

    private Uri readTxFromFile(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            in.close();
            return Uri.parse(sb.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void getBalance() {
        if (StringUtil.isNullOrEmpty(mAddress)) return;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String url = ElaDataSource.getUrl("/api/1/balance/" + mAddress);
                try {
                    String result = ElaDataSource.getInstance(getApplicationContext()).urlGET(url);
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
        Log.d(TAG, "private key: " + privateKey);
        Log.d(TAG, "public key: " + WalletElaManager.getInstance(this).getPublicKey());
        String signed =  ElastosKeypairSign.multiSignTransaction(privateKey, mPublicKeys,
                mPublicKeys.length, mRequiredCount, mTransaction);
        if (!mSend) {
            startNextAndFinish(signed);
            return;
        }

        String serialize = ElastosKeypairSign.serializeMultiSignTransaction(signed);
        Log.d(TAG, "serialize: " + serialize);
        if (!StringUtil.isNullOrEmpty(serialize)) {
            String txid = ElaDataSource.getInstance(this).sendSerializedRawTx(serialize);
            Log.d(TAG, "txid:" + txid);
            closeDialog();
            if (!StringUtil.isNullOrEmpty(txid)) {
                Intent intent = new Intent();
                intent.setClass(this, MultiSignQrActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        }

        BRDialog.showSimpleDialog(this, getString(R.string.Alerts_sendFailure), "Failed to send transaction");
    }

    private void startNextAndFinish(String tx) {
        Intent intent = new Intent();
        intent.setClass(this, MultiSignQrActivity.class);
        intent.putExtra("tx", tx);
        startActivity(intent);
        finish();
    }

    private void showDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing())
                    mLoadingDialog.show();
            }
        });
    }


    private void closeDialog() {
        if(null != mLoadingDialog){
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeDialog();
    }

}
