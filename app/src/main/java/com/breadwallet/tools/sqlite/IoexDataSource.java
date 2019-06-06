package com.breadwallet.tools.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.wallets.ela.BRElaTransaction;
import com.breadwallet.wallet.wallets.ela.data.HistoryTransactionEntity;
import com.breadwallet.wallet.wallets.ela.request.CreateTx;
import com.breadwallet.wallet.wallets.ela.request.Outputs;
import com.breadwallet.wallet.wallets.ela.response.create.ElaTransactionRes;
import com.breadwallet.wallet.wallets.ela.response.create.ElaUTXOInputs;
import com.breadwallet.wallet.wallets.ela.response.create.Meno;
import com.breadwallet.wallet.wallets.ioex.WalletIoexManager;
import com.google.gson.Gson;
import com.platform.APIClient;

import org.elastos.sdk.keypair.ElastosKeypairSign;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class IoexDataSource implements BRDataSourceInterface {

    private static final String TAG = IoexDataSource.class.getSimpleName();

    private static IoexDataSource mInstance;

    private static final String IOEX_NODE = "http://54.92.80.92:20334";

    private final BRSQLiteHelper dbHelper;

    private SQLiteDatabase database;

    private Context mContext;

    private IoexDataSource(Context context) {
        this.mContext = context;
        dbHelper = BRSQLiteHelper.getInstance(context);
    }

    public static IoexDataSource getInstance(Context context){
        if(mInstance == null){
            mInstance = new IoexDataSource(context);
        }

        return mInstance;
    }


    public static String getUrl(String api){
        return new StringBuilder("http://").append(IOEX_NODE).append("/").append(api).toString();
    }

    @WorkerThread
    public String getBalance(String address){
        if(address==null || address.isEmpty()) return null;
        String balance = null;
        try {
            String url = getUrl("api/1/balance/"+address);
            Log.i(TAG, "balance url:"+url);
            String result = urlGET(url);
            JSONObject object = new JSONObject(result);
            balance = object.getString("result");
            Log.i(TAG, "balance:"+balance);
            int status = object.getInt("status");
            if(result==null || !result.contains("result") || !result.contains("status") || balance==null || status!=200) {
//                toast("balance crash result:");
//                throw new Exception("address:"+ address + "\n" + "result:" +result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return balance;
    }


    public synchronized BRElaTransaction createTx(final String inputAddress, final String outputsAddress, final long amount, String memo){
        if(StringUtil.isNullOrEmpty(inputAddress) || StringUtil.isNullOrEmpty(outputsAddress)) return null;
        BRElaTransaction brElaTransaction = null;
//        if(mActivity!=null) toast(mActivity.getResources().getString(R.string.SendTransacton_sending));
        try {
            String url = getUrl("/api/1/ioex/createTx");
            Log.i(TAG, "create tx url:"+url);
            CreateTx tx = new CreateTx();
            tx.inputs.add(inputAddress);

            Outputs outputs = new Outputs();
            outputs.addr = outputsAddress;
            outputs.amt = amount;

            tx.outputs.add(outputs);

            String json = new Gson().toJson(tx);
            String result = urlPost(url, json)/*getCreateTx()*/;

            JSONObject jsonObject = new JSONObject(result);
            String tranactions = jsonObject.getString("result");
            ElaTransactionRes res = new Gson().fromJson(tranactions, ElaTransactionRes.class);
            if(!StringUtil.isNullOrEmpty(memo)) res.Transactions.get(0).Memo = new Meno("text", memo).toString();

            List<ElaUTXOInputs> inputs = res.Transactions.get(0).UTXOInputs;
            for(int i=0; i<inputs.size(); i++){
                ElaUTXOInputs utxoInputs = inputs.get(i);
                utxoInputs.privateKey  = WalletIoexManager.getInstance(mContext).getPrivateKey();
            }

            String transactionJson =new Gson().toJson(res);

            brElaTransaction = new BRElaTransaction();
            brElaTransaction.setTx(transactionJson);
            brElaTransaction.setTxId(inputs.get(0).txid);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return brElaTransaction;
    }

    public synchronized String sendElaRawTx(final String transaction){
        String result = null;
        try {
            String url = getUrl("api/1/sendRawTx");
            Log.i(TAG, "send raw url:"+url);
            String rawTransaction = ElastosKeypairSign.generateRawTransaction(transaction, BRConstants.IOEX_ASSET_ID);
            String json = "{"+"\"data\"" + ":" + "\"" + rawTransaction + "\"" +"}";
            String tmp = urlPost(url, json);
            JSONObject jsonObject = new JSONObject(tmp);
            result = jsonObject.getString("result");
            if(result==null || result.contains("ERROR") || result.contains(" ")) {
//                Thread.sleep(3000);
//                toast(result);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private final String[] allColumns = {
            BRSQLiteHelper.IOEX_COLUMN_ISRECEIVED,
            BRSQLiteHelper.IOEX_COLUMN_TIMESTAMP,
            BRSQLiteHelper.IOEX_COLUMN_BLOCKHEIGHT,
            BRSQLiteHelper.IOEX_COLUMN_HASH,
            BRSQLiteHelper.IOEX_COLUMN_TXREVERSED,
            BRSQLiteHelper.IOEX_COLUMN_FEE,
            BRSQLiteHelper.IOEX_COLUMN_TO,
            BRSQLiteHelper.IOEX_COLUMN_FROM,
            BRSQLiteHelper.IOEX_COLUMN_BALANCEAFTERTX,
            BRSQLiteHelper.IOEX_COLUMN_TXSIZE,
            BRSQLiteHelper.IOEX_COLUMN_AMOUNT,
            BRSQLiteHelper.IOEX_COLUMN_MENO,
            BRSQLiteHelper.IOEX_COLUMN_ISVALID,
            BRSQLiteHelper.IOEX_COLUMN_ISVOTE
    };

    public List<HistoryTransactionEntity> getHistoryTransactions(){
        List<HistoryTransactionEntity> currencies = new ArrayList<>();
        Cursor cursor = null;

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.IOEX_TX_TABLE_NAME, allColumns, null, null, null, null, "timeStamp desc");

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                HistoryTransactionEntity curEntity = cursorToTxEntity(cursor);
                currencies.add(curEntity);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return currencies;
    }

    private HistoryTransactionEntity cursorToTxEntity(Cursor cursor) {
        return new HistoryTransactionEntity(cursor.getInt(0)==1,
                cursor.getLong(1),
                cursor.getInt(2),
                cursor.getBlob(3),
                cursor.getString(4),
                cursor.getLong(5),
                cursor.getString(6),
                cursor.getString(7),
                cursor.getLong(8),
                cursor.getInt(9),
                cursor.getLong(10),
                cursor.getString(11),
                cursor.getInt(12)==1,
                cursor.getInt(13)==1);
    }

    @Override
    public SQLiteDatabase openDatabase() {
        return null;
    }

    @Override
    public void closeDatabase() {

    }


    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public String urlPost(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = APIClient.elaClient.newCall(request).execute();
        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

    @WorkerThread
    public String urlGET(String myURL) throws IOException {
        Map<String, String> headers = BreadApp.getBreadHeaders();

        Request.Builder builder = new Request.Builder()
                .url(myURL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-agent", Utils.getAgentString(mContext, "android/HttpURLConnection"))
                .get();
        Iterator it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            builder.header((String) pair.getKey(), (String) pair.getValue());
        }

        Request request = builder.build();
        Response response = APIClient.elaClient.newCall(request).execute();

        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

}
