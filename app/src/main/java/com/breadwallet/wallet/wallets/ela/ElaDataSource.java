package com.breadwallet.wallet.wallets.ela;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.widget.Toast;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.vote.PayLoadEntity;
import com.breadwallet.vote.ProducerEntity;
import com.breadwallet.vote.ProducersEntity;
import com.breadwallet.wallet.wallets.ela.data.HistoryTransactionEntity;
import com.breadwallet.wallet.wallets.ela.data.MultiTxProducerEntity;
import com.breadwallet.wallet.wallets.ela.data.TxProducerEntity;
import com.breadwallet.wallet.wallets.ela.data.TxProducersEntity;
import com.breadwallet.wallet.wallets.ela.request.CreateTx;
import com.breadwallet.wallet.wallets.ela.request.Outputs;
import com.breadwallet.wallet.wallets.ela.response.create.ElaOutput;
import com.breadwallet.wallet.wallets.ela.response.create.ElaTransaction;
import com.breadwallet.wallet.wallets.ela.response.create.ElaTransactionRes;
import com.breadwallet.wallet.wallets.ela.response.create.ElaUTXOInput;
import com.breadwallet.wallet.wallets.ela.response.create.Meno;
import com.breadwallet.wallet.wallets.ela.response.create.Payload;
import com.breadwallet.wallet.wallets.ela.response.history.History;
import com.breadwallet.wallet.wallets.ela.response.history.TxHistory;
import com.elastos.jni.Utility;
import com.elastos.jni.utils.HexUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.platform.APIClient;

import org.elastos.sdk.keypair.ElastosKeypair;
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
public class ElaDataSource implements BRDataSourceInterface {

    private static final String TAG = ElaDataSource.class.getSimpleName();

    public static final String ELA_NODE_KEY = "elaNodeKey";

    private static final int ONE_PAGE_SIZE = 10;
//    https://api-wallet-ela.elastos.org
//    https://api-wallet-did.elastos.org
//    hw-ela-api-test.elastos.org
//    https://api-wallet-ela-testnet.elastos.org/api/1/currHeight
//    https://api-wallet-did-testnet.elastos.org/api/1/currHeight
    public static final String ELA_NODE =  "node1.elaphant.app" /*"dev.elapp.org"*/;

    private static ElaDataSource mInstance;

    private final BRSQLiteHelper dbHelper;

    private SQLiteDatabase database;

    private static Context mContext;

    private static Activity mActivity;

    private ElaDataSource(Context context){
        mContext = context;
        if(context instanceof Activity) mActivity = findActivity(context);
        dbHelper = BRSQLiteHelper.getInstance(context);
    }


    private static Activity findActivity(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        return null;
    }

    public static ElaDataSource getInstance(Context context){
        if(mInstance == null){
            mInstance = new ElaDataSource(context);
        }

        return mInstance;
    }

    public String getUrlByVersion(String api, String version) {
        String node = BRSharedPrefs.getElaNode(mContext, ELA_NODE_KEY);
        if(StringUtil.isNullOrEmpty(node)) node = ELA_NODE;
        return new StringBuilder("https://").append(node).append("/api/").append(version).append("/").append(api).toString();
    }

    public String getUrl(String api){
        String node = BRSharedPrefs.getElaNode(mContext, ELA_NODE_KEY);
        if(StringUtil.isNullOrEmpty(node)) node = ELA_NODE;
        return new StringBuilder("https://").append(node).append("/api/1/").append(api).toString();
    }

    private final String[] allColumns = {
            BRSQLiteHelper.ELA_COLUMN_ISRECEIVED,
            BRSQLiteHelper.ELA_COLUMN_TIMESTAMP,
            BRSQLiteHelper.ELA_COLUMN_BLOCKHEIGHT,
            BRSQLiteHelper.ELA_COLUMN_HASH,
            BRSQLiteHelper.ELA_COLUMN_TXREVERSED,
            BRSQLiteHelper.ELA_COLUMN_FEE,
            BRSQLiteHelper.ELA_COLUMN_TO,
            BRSQLiteHelper.ELA_COLUMN_FROM,
            BRSQLiteHelper.ELA_COLUMN_BALANCEAFTERTX,
            BRSQLiteHelper.ELA_COLUMN_TXSIZE,
            BRSQLiteHelper.ELA_COLUMN_AMOUNT,
            BRSQLiteHelper.ELA_COLUMN_MENO,
            BRSQLiteHelper.ELA_COLUMN_ISVALID,
            BRSQLiteHelper.ELA_COLUMN_ISVOTE,
            BRSQLiteHelper.ELA_COLUMN_PAGENUMBER,
            BRSQLiteHelper.ELA_COLUMN_STATUS,
            BRSQLiteHelper.ELA_COLUMN_TPYE,
            BRSQLiteHelper.ELA_COLUMN_TXTPYE
    };


    public void deleteAllTransactions() {
        try {
            database = openDatabase();
            database.delete(BRSQLiteHelper.ELA_TX_TABLE_NAME, null, null);
        } finally {
            closeDatabase();
        }
    }

    public synchronized void cacheMultTx(List<HistoryTransactionEntity> elaTransactionEntities){
        if(elaTransactionEntities == null) return;
        try {
            database = openDatabase();
            database.beginTransaction();

            for(HistoryTransactionEntity entity : elaTransactionEntities){

                ContentValues value = new ContentValues();
                value.put(BRSQLiteHelper.ELA_COLUMN_ISRECEIVED, entity.isReceived? 1:0);
                value.put(BRSQLiteHelper.ELA_COLUMN_TIMESTAMP, entity.status.equalsIgnoreCase("pending")?System.currentTimeMillis()/1000:entity.timeStamp);
                value.put(BRSQLiteHelper.ELA_COLUMN_BLOCKHEIGHT, entity.blockHeight);
                value.put(BRSQLiteHelper.ELA_COLUMN_HASH, entity.hash);
                value.put(BRSQLiteHelper.ELA_COLUMN_TXREVERSED, entity.txReversed);
                value.put(BRSQLiteHelper.ELA_COLUMN_FEE, entity.fee);
                value.put(BRSQLiteHelper.ELA_COLUMN_TO, entity.toAddress);
                value.put(BRSQLiteHelper.ELA_COLUMN_FROM, entity.fromAddress);
                value.put(BRSQLiteHelper.ELA_COLUMN_BALANCEAFTERTX, entity.balanceAfterTx);
                value.put(BRSQLiteHelper.ELA_COLUMN_TXSIZE, entity.txSize);
                value.put(BRSQLiteHelper.ELA_COLUMN_AMOUNT, entity.amount);
                value.put(BRSQLiteHelper.ELA_COLUMN_MENO, entity.memo);
                value.put(BRSQLiteHelper.ELA_COLUMN_ISVALID, entity.isValid?1:0);
                value.put(BRSQLiteHelper.ELA_COLUMN_ISVOTE, entity.isVote?1:0);
                value.put(BRSQLiteHelper.ELA_COLUMN_PAGENUMBER, entity.pageNumber);
                value.put(BRSQLiteHelper.ELA_COLUMN_STATUS, entity.status);
                value.put(BRSQLiteHelper.ELA_COLUMN_TPYE, entity.type);
                value.put(BRSQLiteHelper.ELA_COLUMN_TXTPYE, entity.txType);

                long l = database.insertWithOnConflict(BRSQLiteHelper.ELA_TX_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
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

    private TxProducerEntity cursorToTxProducerEntity(Cursor cursor){
        return new TxProducerEntity(cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3));
    }

    private ProducerEntity cursorToProducerEntity(Cursor cursor) {
        return new ProducerEntity(cursor.getString(0),
                cursor.getString(1),
                cursor.getInt(2),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getString(5));
    }

    public List<HistoryTransactionEntity> getHistoryTransactions(){
        List<HistoryTransactionEntity> currencies = new ArrayList<>();
        Cursor cursor = null;

        int pageNumber = BRSharedPrefs.getCurrentHistoryPageNumber(mContext);

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.ELA_TX_TABLE_NAME, allColumns, null, null, null, null, "timeStamp desc");
//            cursor = database.query(BRSQLiteHelper.ELA_TX_TABLE_NAME, allColumns, BRSQLiteHelper.ELA_COLUMN_PAGENUMBER + " = ? ", new String[]{Integer.toString(pageNumber)}, null, null, "timeStamp desc");
//            cursor = database.query(BRSQLiteHelper.ELA_TX_TABLE_NAME, allColumns,
//                    BRSQLiteHelper.ELA_COLUMN_PAGENUMBER+" = ? OR " + BRSQLiteHelper.ELA_COLUMN_PAGENUMBER+" = ? OR " + BRSQLiteHelper.ELA_COLUMN_PAGENUMBER + " = ? ",
//                    new String[]{Integer.toString(lastPageNumber), Integer.toString(nextPageNumber), Integer.toString(pageNumber)}, null, null, "timeStamp desc");

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

    public List<HistoryTransactionEntity> getHistoryTransactionsByPage(int pageNumber) {
        List<HistoryTransactionEntity> currencies = new ArrayList<>();
        Cursor cursor = null;

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.ELA_TX_TABLE_NAME, allColumns, BRSQLiteHelper.ELA_COLUMN_PAGENUMBER + " = ? ", new String[]{Integer.toString(pageNumber)}, null, null, "timeStamp desc");

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
                cursor.getInt(13)==1,
                cursor.getInt(14),
                cursor.getString(15),
                cursor.getString(16),
                cursor.getString(17));
    }

    private void toast(final String message){
        Log.i("ElaDataApi", "message:"+message);
        if(mActivity !=null)
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                }
            });
    }

    public String getRewardAddress(){
        String rewardAddress = null;
        try {
            String url = getUrlByVersion("node/reward/address", "v1");
            String result = urlGET(url);
            JSONObject object = new JSONObject(result);
            rewardAddress = object.getString("result");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rewardAddress;
    }

    @WorkerThread
    public String getElaBalance(String address){
        if(address==null || address.isEmpty()) return null;
        String balance = null;
        try {
            String url = getUrlByVersion("balance/"+address, "1");
            String result = urlGET(url);
            JSONObject object = new JSONObject(result);
            balance = object.getString("result");
            int status = object.getInt("status");
            if(result==null || !result.contains("result") || !result.contains("status") || balance==null || status!=200) {
                toast("balance crash result:");
                throw new Exception("address:"+ address + "\n" + "result:" +result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return balance;
    }

    static class ProducerTxid {
        public List<String> txid;
    }

    public void getProducerByTxid(){
        if(mVoteTxid.size() <= 0) return;
        MultiTxProducerEntity multiTxProducerEntity = null;
        try {
            ProducerTxid producerTxid = new ProducerTxid();
            producerTxid.txid = mVoteTxid;
            String json = new Gson().toJson(producerTxid);
            String url = getUrlByVersion("dpos/transaction/producer", "1");
            String result = urlPost(url, json);
            multiTxProducerEntity = new Gson().fromJson(result, MultiTxProducerEntity.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(multiTxProducerEntity==null || multiTxProducerEntity.result==null) return;
        cacheMultiTxProducer(multiTxProducerEntity.result);
    }

    public List<String> mVoteTxid = new ArrayList<>();
    public void refreshHistory(String address){
        if(StringUtil.isNullOrEmpty(address)) return;
        mVoteTxid.clear();
        try {
            String url = getUrlByVersion("history/"+address +"?pageNum=1&pageSize="+ONE_PAGE_SIZE+"&order=desc", "v3");
            Log.i(TAG, "history url:"+url);
            String result = urlGET(url);
            JSONObject jsonObject = new JSONObject(result);
            String json = jsonObject.getString("result");
            TxHistory txHistory = new Gson().fromJson(json, TxHistory.class);
            BRSharedPrefs.putTotalPageNumber(mContext, txHistory.TotalNum);

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
                historyTransactionEntity.isVote = !isReceived(history.Type) && isVote(history.TxType);
                historyTransactionEntity.pageNumber = 1;
                historyTransactionEntity.timeStamp = new BigDecimal(history.CreateTime).longValue();
                historyTransactionEntity.memo = getMeno(history.Memo);
                historyTransactionEntity.status=  history.Status;
                historyTransactionEntity.type = history.Type;
                historyTransactionEntity.txType = history.TxType;
                elaTransactionEntities.add(historyTransactionEntity);
                if(historyTransactionEntity.isVote) mVoteTxid.add(history.Txid);
            }
            if(elaTransactionEntities.size() <= 0) return;
            cacheMultTx(elaTransactionEntities);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getHistoryByPage(String address, int pageNumber) {
        List<HistoryTransactionEntity> transactionEntities = getHistoryTransactionsByPage(pageNumber);
        if(null!=transactionEntities || transactionEntities.size()<=0){
            if(StringUtil.isNullOrEmpty(address)) return;
            mVoteTxid.clear();
            try {
                String url = getUrlByVersion("history/"+address +"?pageNum="+pageNumber+"&pageSize="+ONE_PAGE_SIZE+"&order=desc", "v3");
                Log.i(TAG, "history url:"+url);
                String result = urlGET(url);
                JSONObject jsonObject = new JSONObject(result);
                String json = jsonObject.getString("result");
                TxHistory txHistory = new Gson().fromJson(json, TxHistory.class);
                BRSharedPrefs.putTotalPageNumber(mContext, txHistory.TotalNum);

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
                    historyTransactionEntity.isVote = !isReceived(history.Type) && isVote(history.TxType);
                    historyTransactionEntity.pageNumber = 1;
                    historyTransactionEntity.timeStamp = new BigDecimal(history.CreateTime).longValue();
                    historyTransactionEntity.memo = getMeno(history.Memo);
                    historyTransactionEntity.status=  history.Status;
                    elaTransactionEntities.add(historyTransactionEntity);
                    if(historyTransactionEntity.isVote) mVoteTxid.add(history.Txid);
                }
                if(elaTransactionEntities.size() <= 0) return;
                cacheMultTx(elaTransactionEntities);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void getHistory(String address){
        if(StringUtil.isNullOrEmpty(address)) return;
        mVoteTxid.clear();
        try {
            int currentPageNumber = BRSharedPrefs.getCurrentHistoryPageNumber(mContext);
            int range = BRSharedPrefs.getHistoryRange(mContext);
            int pageNumber = currentPageNumber+range;
            String url = getUrlByVersion("history/"+address +"?pageNum="+pageNumber+"&pageSize="+ONE_PAGE_SIZE+"&order=desc", "v3");
            Log.i(TAG, "history url:"+url);
            String result = urlGET(url);
            JSONObject jsonObject = new JSONObject(result);
            String json = jsonObject.getString("result");
            TxHistory txHistory = new Gson().fromJson(json, TxHistory.class);
            BRSharedPrefs.putTotalPageNumber(mContext, txHistory.TotalNum);

//            BRSharedPrefs.putTotalPageNumber(mContext, txHistory.TotalNum/ONE_PAGE_SIZE+1);
//            if(currentPageNumber >= 999999){
//                Log.d("loadData", "currentPageNumber:"+currentPageNumber);
//                BRSharedPrefs.putCurrentHistoryPageNumber(mContext, txHistory.TotalNum/ONE_PAGE_SIZE+1);
//                getHistory(address);
//                return;
//            }

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
                historyTransactionEntity.isVote = !isReceived(history.Type) && isVote(history.TxType);
                historyTransactionEntity.pageNumber = pageNumber;
                historyTransactionEntity.timeStamp = new BigDecimal(history.CreateTime).longValue();
                historyTransactionEntity.memo = getMeno(history.Memo);
                historyTransactionEntity.status=  history.Status;
                elaTransactionEntities.add(historyTransactionEntity);
                if(historyTransactionEntity.isVote) mVoteTxid.add(history.Txid);
            }
            if(elaTransactionEntities.size() <= 0) {
                BRSharedPrefs.putCurrentHistoryPageNumber(mContext, currentPageNumber);
                BRSharedPrefs.putHistoryRange(mContext, 0);
                return;
            }
            BRSharedPrefs.putCurrentHistoryPageNumber(mContext, pageNumber);
            BRSharedPrefs.putHistoryRange(mContext, 0);
            cacheMultTx(elaTransactionEntities);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    //true is receive
    private boolean isReceived(String type){
        if(StringUtil.isNullOrEmpty(type)) return false;
        if(type.equals("spend")) return false;
        if(type.equals("income")) return true;

        return true;
    }

    private boolean isVote(String type){
        if(!StringUtil.isNullOrEmpty(type)){
            if(type.equalsIgnoreCase("Vote")) return true;
        }
        return false;
    }

    public synchronized List<BRElaTransaction> createElaTx(final String inputAddress,
                                                           final String outputsAddress,
                                                           final long amount, String memo){
        return createElaTx(inputAddress, outputsAddress, amount, memo, null, null, null);
    }

    public synchronized List<BRElaTransaction> createElaTx(final String inputAddress,
                                                           final String outputsAddress,
                                                           final long amount,
                                                           String memo,
                                                           List<PayLoadEntity> publickeys){
        return createElaTx(inputAddress, outputsAddress, amount, memo, publickeys, null, null);
    }

    List<HistoryTransactionEntity> multiHistoryTransactionEntity = new ArrayList<>();
    List<BRElaTransaction> multiElaTransaction = new ArrayList<>();
    public synchronized List<BRElaTransaction> createElaTx(final String inputAddress,
                                                           final String outputsAddress,
                                                           final long amount,
                                                           String memo,
                                                           List<PayLoadEntity> publickeys,
                                                           List<PayLoadEntity> candidateCrcs,
                                                           CreateTxCallBack callBack){
        if(StringUtil.isNullOrEmpty(inputAddress) || StringUtil.isNullOrEmpty(outputsAddress)) return null;

        if(mActivity!=null) toast(mActivity.getResources().getString(R.string.SendTransacton_sending));
        try {
            String url = getUrlByVersion("createVoteTx", "1");
            Log.i(TAG, "create tx url:"+url);
            CreateTx tx = new CreateTx();
            tx.inputs.add(inputAddress);

            Outputs reqOutputs = new Outputs();
            reqOutputs.addr = outputsAddress;
            reqOutputs.amt = amount;

            tx.outputs.add(reqOutputs);

            String json = new Gson().toJson(tx);
            Log.d("posvote", "request json:"+json);
            String result = urlPost(url, json)/*getCreateTx()*/;

            JSONObject jsonObject = new JSONObject(result);
            String tranactions = jsonObject.getString("result");
            ElaTransactionRes res = new Gson().fromJson(tranactions, ElaTransactionRes.class);

            if(!checkTx(inputAddress, outputsAddress, amount, res.Transactions)) return null;
            if(!StringUtil.isNullOrEmpty(memo)) res.Transactions.get(0).Memo = new Meno("text", memo).toString();
//            if(!StringUtil.isNullOrEmpty(memo)) {
//                String memoStr = null;
//                String outPutPublickey = getPublicKeyByAddress(outputsAddress);
//                if(!StringUtil.isNullOrEmpty(outPutPublickey)) {
//                    memo = ElastosKeypairCrypto.eciesEncrypt(outPutPublickey, memo);
//                    memoStr = new Meno("ciphertext", memo).toString();
//                } else {
//                    memoStr = new Meno("text", memo).toString();
//                }
//
//                res.Transactions.get(0).Memo = memoStr;
//            }

            if(res.Transactions.size() > 1) {
                if(mActivity!=null) toast(mActivity.getResources().getString(R.string.utxo_too_much_hint));
            }

            multiHistoryTransactionEntity.clear();
            multiElaTransaction.clear();
            for(int i=0; i< res.Transactions.size(); i++) {
                if(!StringUtil.isNullOrEmpty(memo)) res.Transactions.get(i).Memo = new Meno("text", memo).toString();

                List<ElaUTXOInput> inputs = res.Transactions.get(i).UTXOInputs;
                List<ElaOutput> outputs = res.Transactions.get(i).Outputs;

                for(int j=0; j<inputs.size(); j++){
                    ElaUTXOInput utxoInputs = inputs.get(j);
                    utxoInputs.privateKey  = WalletElaManager.getInstance(mContext).getPrivateKey();
                }

                if((null!=publickeys && publickeys.size()>0) || (null!=candidateCrcs && candidateCrcs.size()>0)){
                    List<ElaOutput> outputsR = res.Transactions.get(i).Outputs;
                    String lastOutPutAddress = outputsR.get(outputsR.size()-1).address;
                    if(!StringUtil.isNullOrEmpty(lastOutPutAddress)) {
                        ElaOutput output = outputsR.get(outputsR.size()-1);
                        Payload tmp = new Payload();
                        tmp.candidatePublicKeys = publickeys;
                        if(null!=candidateCrcs && candidateCrcs.size()>0 && callBack!=null)
                            callBack.modifyCrcAmount(output, candidateCrcs);
                        tmp.candidateCrcs = candidateCrcs;
                        output.payload = tmp;
                    }
                }

                String transactionJson =new Gson().toJson(res.Transactions.get(i));
                BRElaTransaction brElaTransaction = new BRElaTransaction();
                brElaTransaction.setTx(transactionJson);
                brElaTransaction.setTxId(inputs.get(0).txid);

                HistoryTransactionEntity historyTransactionEntity = new HistoryTransactionEntity();
                historyTransactionEntity.fromAddress = inputAddress;
                historyTransactionEntity.toAddress = outputsAddress;
                historyTransactionEntity.isReceived = false;
                historyTransactionEntity.fee = WalletElaManager.getInstance(mContext).SALA_FEE.longValue();
                historyTransactionEntity.blockHeight = 0;
                historyTransactionEntity.hash = new byte[1];
                historyTransactionEntity.txSize = 0;
                historyTransactionEntity.balanceAfterTx = 0;
                historyTransactionEntity.timeStamp = System.currentTimeMillis()/1000;
                historyTransactionEntity.isValid = true;
                historyTransactionEntity.isVote = (publickeys!=null && publickeys.size()>0);
                historyTransactionEntity.memo = memo;
                historyTransactionEntity.status=  "pending";

                ElaOutput output = outputs.get(0);
                String address = output.address;
                if(!StringUtil.isNullOrEmpty(address) && address.equals(outputsAddress)){
                    historyTransactionEntity.amount = new BigDecimal(output.amount).longValue();
                }

                multiElaTransaction.add(brElaTransaction);
                multiHistoryTransactionEntity.add(historyTransactionEntity);
            }
        } catch (Exception e) {
            if(mActivity!=null) toast(mActivity.getResources().getString(R.string.SendTransacton_failed));
            e.printStackTrace();
        }

        return multiElaTransaction;
    }

    public interface CreateTxCallBack {
        void modifyCrcAmount(ElaOutput outputs, List<PayLoadEntity> payLoadEntities);
    }

    private boolean checkSignature(ElaTransaction tx){
        if(tx == null) return false;

        String pub = tx.Postmark.pub;
        String signature = tx.Postmark.signature;
        if(!StringUtil.isNullOrEmpty(pub) && !StringUtil.isNullOrEmpty(signature)) {
            StringBuilder sourceBuilder = new StringBuilder();
            for(ElaUTXOInput input : tx.UTXOInputs) {
                sourceBuilder.append(input.txid)
                        .append("-")
                        .append(input.index)
                        .append(";");
            }
            sourceBuilder.append("&");

            for(ElaOutput output : tx.Outputs) {
                sourceBuilder.append(output.address)
                        .append("-")
                        .append(output.amount);
            }
            sourceBuilder.append("&");

            sourceBuilder.append(tx.Fee);

            String source = sourceBuilder.toString();
            boolean isValid = Utility.getInstance(mContext).verify(pub, source.getBytes(), HexUtils.hexToByteArray(signature));
            return isValid;
        }

        return false;
    }



    public boolean checkTx(String inputAddress, String outputAddress, long amount, List<ElaTransaction> elaTransactions) {
        if(StringUtil.isNullOrEmpty(inputAddress) ||
                StringUtil.isNullOrEmpty(outputAddress) ||
                amount<0 ||
                elaTransactions == null) return false;

        String nodeAddress = null;
        long sumToAmount = 0;
        boolean isSendToOther = false;

        for(ElaTransaction elaTransaction : elaTransactions){
            if(checkSignature(elaTransaction)) return false;

            if(elaTransaction.Postmark != null) {
                nodeAddress = ElastosKeypair.getAddress(elaTransaction.Postmark.pub);
                String rewardAddress = getRewardAddress();
                if(!StringUtil.isNullOrEmpty(nodeAddress) && !nodeAddress.equals(rewardAddress)) {
                    return false;
                }
            }

            boolean hasToAddress = false;
            boolean hasNodeAddress = false;
            for(ElaOutput output : elaTransaction.Outputs){

                if(outputAddress.equals(inputAddress)){

                    if(output.address.equals(nodeAddress)){

                        if(output.amount+elaTransaction.Fee != elaTransaction.Total_Node_Fee) {
                            return false;
                        }

                        if(hasNodeAddress){
                            return false;
                        }
                        hasNodeAddress = true;
                    } else {
                        if(!inputAddress.equals(output.address)){
                            return false;
                        }
                    }
                } else {
                    isSendToOther = true;
                   if(!outputAddress.equals(nodeAddress)){
                       if(output.address.equals(nodeAddress)){
                           if(output.amount+elaTransaction.Fee != elaTransaction.Total_Node_Fee){
                               return false;
                           }
                           if(hasNodeAddress){
                               return false;
                           }
                           hasNodeAddress = false;
                       } else if(output.address.equals(outputAddress)){
                            sumToAmount += output.amount;
                            if(hasToAddress) {
                                return false;
                            }
                            hasToAddress = true;
                       } else if(!output.address.equals(inputAddress)){
                            return false;
                       }
                   } else {
                       if(output.address.equals(nodeAddress)){
                           if(output.amount+elaTransaction.Fee == elaTransaction.Total_Node_Fee) {
                               if(hasNodeAddress) {
                                   return false;
                               }
                               hasNodeAddress = true;
                           } else {
                               sumToAmount += output.amount;
                               if(hasToAddress) {
                                   return false;
                               }
                               hasToAddress = true;
                           }
                       } else if(!output.address.equals(inputAddress) ){ //找零地址
                            return false;
                       }
                   }
                }
            }
        }

        if(sumToAmount!=amount && isSendToOther) {
            return false;
        }

        return true;
    }


    public synchronized String sendElaRawTx(final List<BRElaTransaction> transactions){
        if(transactions==null || transactions.size()<=0) return null;
        String result = null;
        try {
            String url = getUrlByVersion("sendRawTx", "1");
            Log.i(TAG, "send raw url:"+url);
            List<String> rawTransactions = new ArrayList<>();
            for(int i=0; i<transactions.size(); i++) {
                ElaTransaction elaTransaction = new Gson().fromJson(transactions.get(i).getTx(), ElaTransaction.class);
                List<ElaTransaction> elaTransactions = new ArrayList<>();
                elaTransactions.add(elaTransaction);
                ElaTransactionRes elaTransactionRes = new ElaTransactionRes();
                elaTransactionRes.Transactions = elaTransactions;
                String txs = new Gson().toJson(elaTransactionRes);
                String rawTransaction = ElastosKeypairSign.generateRawTransaction(txs, BRConstants.ELA_ASSET_ID);
                rawTransactions.add(rawTransaction);
            }

            String json = "{\"data\":" + new Gson().toJson(rawTransactions) + "}";

            String tmp = urlPost(url, json) /*"{\"result\":[\"8cf2df7d3205b286a1531d28225b7cb5d8c7834a282ba70499759eba31479024\"],\"status\":200}"*/;
            JSONObject jsonObject = new JSONObject(tmp);
            result = jsonObject.getString("result");
            if(result==null || result.contains("ERROR") || result.contains(" ")) {
                Thread.sleep(3000);
                if(mActivity!=null) toast(/*mActivity.getString(R.string.double_spend)*/"send transaction error");
//                toast(result);
                return null;
            }
            List<String> historyTransactions = new Gson().fromJson(result, new TypeToken<List<String>>() {
            }.getType());
//            if(multiHistoryTransactionEntity.size() != historyTransactions.size()) return null;
            for(int i=0; i<historyTransactions.size(); i++) {
                multiHistoryTransactionEntity.get(i).txReversed = historyTransactions.get(i);
            }
            cacheMultTx(multiHistoryTransactionEntity);
        } catch (Exception e) {
            if(mActivity!=null) toast(mActivity.getResources().getString(R.string.SendTransacton_failed));
            e.printStackTrace();
        }

        return result;
    }

    public synchronized String sendSerializedRawTx(final String rawTransaction) {
        String result = null;
        try {
            String url = getUrlByVersion("sendRawTx", "1");
            Log.i(TAG, "send raw url:"+url);
            String json = "{"+"\"data\"" + ":" + "\"" + rawTransaction + "\"" +"}";
            Log.i(TAG, "rawTransaction:"+rawTransaction);
            String tmp = urlPost(url, json);
            JSONObject jsonObject = new JSONObject(tmp);
            result = jsonObject.getString("result");
            Log.d(TAG, "send raw tx result: " + tmp);
            if(result==null || result.contains("ERROR") || result.contains(" ")) {
                Thread.sleep(3000);
                if(mActivity!=null) toast(mActivity.getString(R.string.double_spend));
                return null;
            }
            Log.d("post multi-sig", "txId:"+result);
        } catch (Exception e) {
            if(mActivity!=null) toast(mActivity.getResources().getString(R.string.SendTransacton_failed));
            e.printStackTrace();
        }

        return result;
    }

    public long getNodeFee() {
        long result = 0;
        try {
            String url = getUrlByVersion("fee", "1");
            String tmp = urlGET(url);
            JSONObject jsonObject = new JSONObject(tmp);
            result = jsonObject.getLong("result");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public void getProducers(){
        try {
            String jsonRes = urlGET(getUrlByVersion("dpos/rank/height/9999999999999999", "1"));
            if(!StringUtil.isNullOrEmpty(jsonRes) && jsonRes.contains("result")) {
                ProducersEntity producersEntity = new Gson().fromJson(jsonRes, ProducersEntity.class);
                List list = producersEntity.result;
                if(list==null || list.size()<=0) return;
                cacheProducer(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<TxProducerEntity> getTxProducerByTxid(String txid){
        if(StringUtil.isNullOrEmpty(txid)) return null;
        List<TxProducerEntity> entities = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(BRSQLiteHelper.HISTORY_PRODUCER_TABLE_NAME,
                    null, BRSQLiteHelper.HISTORY_PRODUCER_TXID + " = ?", new String[]{txid},
                    null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                TxProducerEntity producerEntity = cursorToTxProducerEntity(cursor);
                entities.add(producerEntity);
                cursor.moveToNext();
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return entities;
    }

    public List<ProducerEntity> getProducersByPK(List<String> publicKeys){
        if(publicKeys==null || publicKeys.size()<=0) return null;
        List<ProducerEntity> entities = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();

            for(String publickey : publicKeys){
                cursor = database.query(BRSQLiteHelper.ELA_PRODUCER_TABLE_NAME,
                        null, BRSQLiteHelper.PEODUCER_PUBLIC_KEY + " = ?", new String[]{publickey},
                        null, null, null);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    ProducerEntity producerEntity = cursorToProducerEntity(cursor);
                    entities.add(producerEntity);
                    cursor.moveToNext();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return entities;
    }

    public void cacheMultiTxProducer(List<TxProducersEntity> entities){
        if(entities==null || entities.size()<=0) return;
        try {
            database = openDatabase();
            database.beginTransaction();
            for(TxProducersEntity txProducersEntity : entities){
                if(null==txProducersEntity.Producer || StringUtil.isNullOrEmpty(txProducersEntity.Txid)) break;
                for(TxProducerEntity txProducerEntity : txProducersEntity.Producer){
                    ContentValues value = new ContentValues();
                    value.put(BRSQLiteHelper.HISTORY_PRODUCER_TXID, txProducersEntity.Txid);
                    value.put(BRSQLiteHelper.HISTORY_PRODUCER_OWN_PUBLICKEY, txProducerEntity.Ownerpublickey);
                    value.put(BRSQLiteHelper.HISTORY_PRODUCER_NOD_PUBLICKEY, txProducerEntity.Nodepublickey);
                    value.put(BRSQLiteHelper.HISTORY_PRODUCER_NICKNAME, txProducerEntity.Nickname);
                    long l = database.insertWithOnConflict(BRSQLiteHelper.HISTORY_PRODUCER_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            closeDatabase();
            e.printStackTrace();
        } finally {
            database.endTransaction();
            closeDatabase();
        }
    }

    public synchronized void cacheProducer(List<ProducerEntity> values) {
        if (values == null || values.size() <= 0) return;
        try {
            database = openDatabase();
            database.beginTransaction();

            for (ProducerEntity entity : values) {
                ContentValues value = new ContentValues();
                value.put(BRSQLiteHelper.PEODUCER_PUBLIC_KEY, entity.Producer_public_key);
                value.put(BRSQLiteHelper.PEODUCER_VALUE, entity.Value);
                value.put(BRSQLiteHelper.PEODUCER_RANK, entity.Rank);
                value.put(BRSQLiteHelper.PEODUCER_ADDRESS, entity.Address);
                value.put(BRSQLiteHelper.PEODUCER_NICKNAME, entity.Nickname);
                value.put(BRSQLiteHelper.PEODUCER_VOTES, entity.Votes);
                long l = database.insertWithOnConflict(BRSQLiteHelper.ELA_PRODUCER_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            closeDatabase();
            e.printStackTrace();
        } finally {
            database.endTransaction();
            closeDatabase();
        }
    }

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public String urlPost(String url, String json) throws Exception {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = APIClient.elaClient.newCall(request).execute();
        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new Exception("Unexpected code " + response);
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
}
