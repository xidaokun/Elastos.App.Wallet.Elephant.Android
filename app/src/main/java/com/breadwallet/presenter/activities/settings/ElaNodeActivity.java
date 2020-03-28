package com.breadwallet.presenter.activities.settings;

import android.accounts.NetworkErrorException;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.wallets.ela.ElaDataSource;
import com.breadwallet.wallet.wallets.ela.ElaDataUtils;
import com.breadwallet.wallet.wallets.ela.WalletElaManager;
import com.platform.APIClient;

import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

import okhttp3.Request;
import okhttp3.Response;

public class ElaNodeActivity extends BRActivity {

    private static String TAG = ElaNodeActivity.class.getSimpleName();

    BaseTextView mCurrentNode;
    BaseTextView mConnectStatus;
    BRButton mSwitchBtn;
    BRButton mSelectBtn;
    View mListBgView;
    ListView mNodeLv;
    private static final int CONNECT_SUCCESS = 0x01;
    private static final int CONNECT_FAILED = 0x02;

    //https://api-wallet-ela-testnet.elastos.org/api/1/currHeight

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ela_node);
        mCurrentNode = findViewById(R.id.node_text);
        mConnectStatus = findViewById(R.id.node_status);
        mSwitchBtn = findViewById(R.id.button_switch);
        mSelectBtn = findViewById(R.id.node_list_btn);
        mNodeLv = findViewById(R.id.node_listview);
        mListBgView = findViewById(R.id.list_bg);
        mCurrentNode.setText(BRSharedPrefs.getElaNode(this, ElaDataUtils.ELA_NODE_KEY));
        mConnectStatus.setText(getString(R.string.NodeSelector_connected));

        mSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDialog();
            }
        });
        mSelectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showList();
            }
        });
        final String[] nodes = {"node1.elaphant.app", /*"api-wallet-ela-testnet.elastos.org", */"default node"};
        ArrayAdapter adapter = new ArrayAdapter(this, R.layout.node_item_layout, nodes);
        mNodeLv.setAdapter(adapter);
        mNodeLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(i < nodes.length-1){
                    Log.i(TAG, "item:"+i);
                    String input = nodes[i];
                    if(!StringUtil.isNullOrEmpty(input)) {
                        testConnect(input);
                    }
//                    testConnect(input);
                }
                hideList();
            }
        });
        findViewById(R.id.back_arrow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void testConnect(final String node){
        if(StringUtil.isNullOrEmpty(node)) return;
        mConnectStatus.setText(getString(R.string.NodeSelector_connecting));
        final String url = "https://"+node+"/api/1/currHeight";
        new Thread(new Runnable() {
            @Override
            public void run() {
                String ret = null;
                try {
                     ret = urlGET(url);
                     //{"result":312513,"status":200}
                    JSONObject object = new JSONObject(ret);
                    long height = object.getLong("result");
                    int status = object.getInt("status");
                     if(height>0 && status==200) {
                         runOnUiThread(new Runnable() {
                             @Override
                             public void run() {
                                 changeConnectStatus(node, true);
                             }
                         });
                     } else {
                         runOnUiThread(new Runnable() {
                             @Override
                             public void run() {
                                 changeConnectStatus(node, false);
                             }
                         });
                     }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            changeConnectStatus(node, false);
                        }
                    });
                }
            }
        }).start();
    }

    private void changeConnectStatus(String node, boolean success){
        if (success) {
            BRSharedPrefs.putElaNode(ElaNodeActivity.this, ElaDataUtils.ELA_NODE_KEY, node.trim());
            wipeData();
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    WalletElaManager.getInstance(ElaNodeActivity.this).updateFee(ElaNodeActivity.this);
                }
            });
        }
        mCurrentNode.setText(node);
        mConnectStatus.setText(success?getString(R.string.NodeSelector_connected) : getString(R.string.NodeSelector_connect_error));
    }

    private void hideList(){
        mNodeLv.setVisibility(View.GONE);
        mListBgView.setVisibility(View.GONE);
        mSwitchBtn.setClickable(true);
        mSelectBtn.setClickable(true);
        mSwitchBtn.setEnabled(true);
        mSelectBtn.setEnabled(true);
    }

    private void showList(){
        mNodeLv.setVisibility(View.VISIBLE);
        mListBgView.setVisibility(View.VISIBLE);
        mSwitchBtn.setClickable(false);
        mSelectBtn.setClickable(false);
        mSwitchBtn.setEnabled(false);
        mSelectBtn.setEnabled(false);
    }

    private void createDialog() {

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        final TextView customTitle = new TextView(this);

        customTitle.setGravity(Gravity.CENTER);
        customTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        int pad32 = Utils.getPixelsFromDps(this, 32);
        int pad16 = Utils.getPixelsFromDps(this, 16);
        customTitle.setPadding(pad16, pad16, pad16, pad16);
        customTitle.setText(getString(R.string.NodeSelector_enterTitle));
        customTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        customTitle.setTypeface(null, Typeface.BOLD);
        alertDialog.setCustomTitle(customTitle);
        alertDialog.setMessage(getString(R.string.NodeSelector_enterBody));

        final EditText inputEdit = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        int pix = Utils.getPixelsFromDps(this, 24);

        inputEdit.setPadding(pix, 0, pix, pix);
        inputEdit.setLayoutParams(lp);
        alertDialog.setView(inputEdit);

        alertDialog.setNegativeButton(getString(R.string.Button_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.setPositiveButton(getString(R.string.Button_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String input = inputEdit.getText().toString().trim();
                        if(!StringUtil.isNullOrEmpty(input)) {
                            if(input.contains("http") || input.contains("https")){
                                Uri uri = Uri.parse(input);
                                input = uri.getHost();
                            }
                            mCurrentNode.setText(input);
                            testConnect(input);
                        }
//                        testConnect(input);
                    }
                });
        alertDialog.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                inputEdit.requestFocus();
                final InputMethodManager keyboard = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(inputEdit, 0);
            }
        }, 200);
    }

    private void wipeData(){
        BRSharedPrefs.putCachedBalance(this, "ELA",  new BigDecimal(0));
        ElaDataSource.getInstance(this).deleteAllTransactions();
    }

    public String urlGET(String myURL) throws IOException {
        Map<String, String> headers = BreadApp.getBreadHeaders();

        Request.Builder builder = new Request.Builder()
                .url(myURL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-agent", Utils.getAgentString(this, "android/HttpURLConnection"))
                .get();
        Iterator it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            builder.header((String) pair.getKey(), (String) pair.getValue());
        }

        Request request = builder.build();
        Response response = APIClient.testNodeClient.newCall(request).execute();

        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }
}
