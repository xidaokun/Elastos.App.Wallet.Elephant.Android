package com.breadwallet.tools.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.wallets.ela.data.HistoryTransactionEntity;
import com.breadwallet.wallet.wallets.ela.request.CreateTx;
import com.breadwallet.wallet.wallets.ela.request.Outputs;
import com.breadwallet.wallet.wallets.ela.response.create.ElaTransactionRes;
import com.breadwallet.wallet.wallets.ela.response.create.ElaUTXOInput;
import com.breadwallet.wallet.wallets.ela.response.create.Meno;
import com.breadwallet.wallet.wallets.ela.response.history.History;
import com.breadwallet.wallet.wallets.ela.response.history.TxHistory;
import com.breadwallet.wallet.wallets.ioex.BRIoexTransaction;
import com.breadwallet.wallet.wallets.ioex.WalletIoexManager;
import com.google.gson.Gson;
import com.platform.APIClient;

import org.elastos.sdk.keypair.ElastosKeypairSign;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
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

    private static final String IOEX_NODE = "api-ioex.elaphant.app";

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
        return new StringBuilder("https://").append(IOEX_NODE).append("/").append(api).toString();
    }

    @WorkerThread
    public String getBalance(String address){
        if(StringUtil.isNullOrEmpty(address)) return null;
        String balance = null;
        try {
            String url = getUrl("api/1/ioex/balance/"+address);
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

    HistoryTransactionEntity historyTransactionEntity = new HistoryTransactionEntity();
    public synchronized BRIoexTransaction createTx(final String inputAddress, final String outputsAddress, final long amount, String memo){
        if(StringUtil.isNullOrEmpty(inputAddress) || StringUtil.isNullOrEmpty(outputsAddress)) return null;
        BRIoexTransaction brIoexTransaction = null;
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

            List<ElaUTXOInput> inputs = res.Transactions.get(0).UTXOInputs;
            for(int i=0; i<inputs.size(); i++){
                ElaUTXOInput utxoInputs = inputs.get(i);
                utxoInputs.privateKey  = WalletIoexManager.getInstance(mContext).getPrivateKey();
            }

            String transactionJson =new Gson().toJson(res);

            brIoexTransaction = new BRIoexTransaction();
            brIoexTransaction.setTx(transactionJson);
            brIoexTransaction.setTxId(inputs.get(0).txid);

            historyTransactionEntity.txReversed = inputs.get(0).txid;
            historyTransactionEntity.fromAddress = inputAddress;
            historyTransactionEntity.toAddress = outputsAddress;
            historyTransactionEntity.isReceived = false;
            historyTransactionEntity.fee = new BigDecimal("100").longValue();
            historyTransactionEntity.blockHeight = 0;
            historyTransactionEntity.hash = new byte[1];
            historyTransactionEntity.txSize = 0;
            historyTransactionEntity.amount = new BigDecimal(amount).longValue();
            historyTransactionEntity.balanceAfterTx = 0;
            historyTransactionEntity.timeStamp = System.currentTimeMillis()/1000;
            historyTransactionEntity.isValid = true;
            historyTransactionEntity.memo = memo;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return brIoexTransaction;
    }

    public synchronized String sendIoexRawTx(final String transaction){
        String result = null;
        try {
            String url = getUrl("api/1/ioex/sendRawTx");
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
            historyTransactionEntity.txReversed = result;
            cacheSingleTx(historyTransactionEntity);
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
    };

    public void getHistory(String address){
        if(StringUtil.isNullOrEmpty(address)) return;
        try {
            String url = getUrl("api/1/ioex/history/"+address /*+"?pageNum=1&pageSize=10"*/);
            Log.i(TAG, "history url:"+url);
            String result = urlGET(url);
            JSONObject jsonObject = new JSONObject(result);
            String json = jsonObject.getString("result");
            TxHistory txHistory = new Gson().fromJson(json, TxHistory.class);

            List<HistoryTransactionEntity> elaTransactionEntities = new ArrayList<>();
            elaTransactionEntities.clear();
            List<History> transactions = txHistory.History;
            for(History history : transactions){
                HistoryTransactionEntity historyTransactionEntity = new HistoryTransactionEntity();
                historyTransactionEntity.txReversed = history.Txid;
                historyTransactionEntity.isReceived = isReceived(history.Type);
                historyTransactionEntity.fromAddress = isReceived(history.Type) ? history.Inputs.get(0) : history.Outputs.get(0);
                historyTransactionEntity.toAddress = isReceived(history.Type) ? history.Inputs.get(0) : history.Outputs.get(0);
                historyTransactionEntity.fee = new BigDecimal(history.Fee).longValue();
                historyTransactionEntity.blockHeight = history.Height;
                historyTransactionEntity.hash = history.Txid.getBytes();
                historyTransactionEntity.txSize = 0;
                historyTransactionEntity.amount = isReceived(history.Type) ? new BigDecimal(history.Value).longValue() : new BigDecimal(history.Value).subtract(new BigDecimal(history.Fee)).longValue();
                historyTransactionEntity.balanceAfterTx = 0;
                historyTransactionEntity.isValid = true;
                historyTransactionEntity.timeStamp = new BigDecimal(history.CreateTime).longValue();
                historyTransactionEntity.memo = getMeno(history.Memo);
                elaTransactionEntities.add(historyTransactionEntity);
            }
            cacheMultTx(elaTransactionEntities);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cacheSingleTx(HistoryTransactionEntity entity){
        List<HistoryTransactionEntity> entities = new ArrayList<>();
        entities.clear();
        entities.add(entity);
        cacheMultTx(entities);
    }

    public synchronized void cacheMultTx(List<HistoryTransactionEntity> ioexTransactionEntities){
        if(ioexTransactionEntities == null) return;
//        Cursor cursor = null;
        try {
            database = openDatabase();
            database.beginTransaction();

            for(HistoryTransactionEntity entity : ioexTransactionEntities){

                ContentValues value = new ContentValues();
                value.put(BRSQLiteHelper.IOEX_COLUMN_ISRECEIVED, entity.isReceived? 1:0);
                value.put(BRSQLiteHelper.IOEX_COLUMN_TIMESTAMP, entity.timeStamp);
                value.put(BRSQLiteHelper.IOEX_COLUMN_BLOCKHEIGHT, entity.blockHeight);
                value.put(BRSQLiteHelper.IOEX_COLUMN_HASH, entity.hash);
                value.put(BRSQLiteHelper.IOEX_COLUMN_TXREVERSED, entity.txReversed);
                value.put(BRSQLiteHelper.IOEX_COLUMN_FEE, entity.fee);
                value.put(BRSQLiteHelper.IOEX_COLUMN_TO, entity.toAddress);
                value.put(BRSQLiteHelper.IOEX_COLUMN_FROM, entity.fromAddress);
                value.put(BRSQLiteHelper.IOEX_COLUMN_BALANCEAFTERTX, entity.balanceAfterTx);
                value.put(BRSQLiteHelper.IOEX_COLUMN_TXSIZE, entity.txSize);
                value.put(BRSQLiteHelper.IOEX_COLUMN_AMOUNT, entity.amount);
                value.put(BRSQLiteHelper.IOEX_COLUMN_MENO, entity.memo);
                value.put(BRSQLiteHelper.IOEX_COLUMN_ISVALID, entity.isValid?1:0);

                long l = database.insertWithOnConflict(BRSQLiteHelper.IOEX_TX_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
                Log.i(TAG, "l:"+l);
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            closeDatabase();
            e.printStackTrace();
        } finally {
//            cursor.close();
            database.endTransaction();
            closeDatabase();
        }

    }

    //true is receive
    private boolean isReceived(String type){
        if(StringUtil.isNullOrEmpty(type)) return false;
        if(type.equals("spend")) return false;
        if(type.equals("income")) return true;

        return true;
    }

    private String getMeno(String value){
        if(value==null || !value.contains("msg") || !value.contains("type") || !value.contains(",")) return "";
        if(value.contains("msg:")){
            String[] msg = value.split("msg:");
            if(msg!=null && msg.length==2){
                return msg[1];
            }
        }
        return "";
    }


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
                cursor.getInt(12)==1);
    }

    @Override
    public SQLiteDatabase openDatabase() {
        if (database == null || !database.isOpen())
            database = dbHelper.getWritableDatabase();
        dbHelper.setWriteAheadLoggingEnabled(BRConstants.WRITE_AHEAD_LOGGING);
        return database;
    }

    @Override
    public void closeDatabase() {

    }

    public void deleteAllTransactions() {
        try {
            database = openDatabase();
            database.delete(BRSQLiteHelper.IOEX_TX_TABLE_NAME, null, null);
        } finally {
            closeDatabase();
        }
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
