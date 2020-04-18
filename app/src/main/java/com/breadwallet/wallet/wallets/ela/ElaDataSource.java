package com.breadwallet.wallet.wallets.ela;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.vote.PayLoadEntity;
import com.breadwallet.vote.ProducerEntity;
import com.breadwallet.vote.ProducersEntity;
import com.breadwallet.wallet.wallets.ela.data.DposProducer;
import com.breadwallet.wallet.wallets.ela.data.DposProducerResult;
import com.breadwallet.wallet.wallets.ela.data.DposProducers;
import com.breadwallet.wallet.wallets.ela.data.HistoryTransactionEntity;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.platform.APIClient;

import org.elastos.sdk.keypair.ElastosKeypairSign;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ElaDataSource implements BRDataSourceInterface {

    private static final String TAG = ElaDataSource.class.getSimpleName();

    private static final int ONE_PAGE_SIZE = 10;
//    https://api-wallet-ela.elastos.org
//    https://api-wallet-did.elastos.org
//    hw-ela-api-test.elastos.org
//    https://api-wallet-ela-testnet.elastos.org/api/1/currHeight
//    https://api-wallet-did-testnet.elastos.org/api/1/currHeight

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

                ContentValues value = ElaDataUtils.createHistoryValues(entity);

                long l = database.insertWithOnConflict(BRSQLiteHelper.ELA_TX_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
                Log.i(TAG, "l:"+l);
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

    public List<HistoryTransactionEntity> queryHistoryTransactions(){
        List<HistoryTransactionEntity> currencies = new ArrayList<>();
        Cursor cursor = null;

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.ELA_TX_TABLE_NAME, ElaDataUtils.elaHistoryColumn, null, null, null, null, "timeStamp desc");

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                HistoryTransactionEntity curEntity = ElaDataUtils.cursorToTxEntity(cursor);
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
            String url = ElaDataUtils.getUrlByVersion(mContext, "node/reward/address", "v1");
            String result = APIClient.urlGET(mContext, url);
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
            String url = ElaDataUtils.getUrlByVersion(mContext,"balance/"+address, "1");
            String result = APIClient.urlGET(mContext, url);
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

    public List<String> mDposVoteTxid = new ArrayList<>();
    public void getDposProducer(List<String> txids){
        if(mDposVoteTxid.size() <= 0) return;
        DposProducerResult dposProducerResult = null;
        try {
            ProducerTxid producerTxid = new ProducerTxid();
            producerTxid.txid = txids;
            String json = new Gson().toJson(producerTxid);
            String url = ElaDataUtils.getUrlByVersion(mContext,"dpos/transaction/producer", "1");
            String result = APIClient.urlPost(url, json);
            dposProducerResult = new Gson().fromJson(result, DposProducerResult.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(dposProducerResult ==null || dposProducerResult.result==null) return;
        cacheDposProducer(dposProducerResult.result);
    }

//    public List<String> mCrcVoteTxid = new ArrayList<>();

    public void refreshHistory(String address){
        if(StringUtil.isNullOrEmpty(address)) return;
        mDposVoteTxid.clear();
//        mCrcVoteTxid.clear();
        try {
            String url = ElaDataUtils.getUrlByVersion(mContext,"history/"+address +"?pageNum=1&pageSize="+ONE_PAGE_SIZE+"&order=desc", "v4");
            Log.i(TAG, "history url:"+url);
            String result = APIClient.urlGET(mContext, url);
            JSONObject jsonObject = new JSONObject(result);
            String json = jsonObject.getString("result");
            TxHistory txHistory = new Gson().fromJson(json, TxHistory.class);
            BRSharedPrefs.putTotalPageNumber(mContext, txHistory.TotalNum);

            List<HistoryTransactionEntity> elaTransactionEntities = new ArrayList<>();
            elaTransactionEntities.clear();
            List<History> transactions = txHistory.History;
            for(History history : transactions){
                HistoryTransactionEntity historyTransactionEntity = ElaDataUtils.setHistoryEntity(history, 1);
                elaTransactionEntities.add(historyTransactionEntity);
                int dposType = ElaDataUtils.getVoteType(historyTransactionEntity.type, historyTransactionEntity.txType);
//                int crcType = ElaDataUtils.getVoteType(historyTransactionEntity.type, historyTransactionEntity.txType);
                if(dposType==1 || dposType==3) mDposVoteTxid.add(history.Txid);
//                if(crcType==2 || crcType==3) mCrcVoteTxid.add(history.Txid);
            }
            if(elaTransactionEntities.size() <= 0) return;
            cacheMultTx(elaTransactionEntities);
            getDposProducer(mDposVoteTxid);
//            CrcDataSource.getInstance(mContext).getCrcProducer(mCrcVoteTxid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getHistory(String address){
        if(StringUtil.isNullOrEmpty(address)) return;
        mDposVoteTxid.clear();
//        mCrcVoteTxid.clear();
        try {
            int currentPageNumber = BRSharedPrefs.getCurrentHistoryPageNumber(mContext);
            int pageNumber = currentPageNumber+1;
            String url = ElaDataUtils.getUrlByVersion(mContext,"history/"+address +"?pageNum="+pageNumber+"&pageSize="+ONE_PAGE_SIZE+"&order=desc", "v4");
            Log.i(TAG, "history url:"+url);
            String result = APIClient.urlGET(mContext, url);
            JSONObject jsonObject = new JSONObject(result);
            String json = jsonObject.getString("result");
            TxHistory txHistory = new Gson().fromJson(json, TxHistory.class);
            BRSharedPrefs.putTotalPageNumber(mContext, txHistory.TotalNum);

            List<HistoryTransactionEntity> elaTransactionEntities = new ArrayList<>();
            elaTransactionEntities.clear();
            List<History> transactions = txHistory.History;
            for(History history : transactions){
                HistoryTransactionEntity historyTransactionEntity = ElaDataUtils.setHistoryEntity(history, pageNumber);
                elaTransactionEntities.add(historyTransactionEntity);
                int dposType = ElaDataUtils.getVoteType(historyTransactionEntity.type, historyTransactionEntity.txType);
//                int crcType = ElaDataUtils.getVoteType(historyTransactionEntity.type, historyTransactionEntity.txType);
                if(dposType==1 || dposType==3) mDposVoteTxid.add(history.Txid);
//                if(crcType==2 || crcType==3) mCrcVoteTxid.add(history.Txid);
            }
            if(elaTransactionEntities.size() <= 0) {
                BRSharedPrefs.putCurrentHistoryPageNumber(mContext, currentPageNumber);
                return;
            }
            BRSharedPrefs.putCurrentHistoryPageNumber(mContext, pageNumber);
            cacheMultTx(elaTransactionEntities);
            getDposProducer(mDposVoteTxid);
//            CrcDataSource.getInstance(mContext).getCrcProducer(mCrcVoteTxid);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            String url = ElaDataUtils.getUrlByVersion(mContext,"createVoteTx", "1");
            Log.i(TAG, "create tx url:"+url);
            CreateTx tx = new CreateTx();
            tx.inputs.add(inputAddress);

            Outputs reqOutputs = new Outputs();
            reqOutputs.addr = outputsAddress;
            reqOutputs.amt = amount;

            tx.outputs.add(reqOutputs);

            String json = new Gson().toJson(tx);
            Log.d("posvote", "request json:"+json);
            String result = APIClient.urlPost(url, json)/*getCreateTx()*/;

            JSONObject jsonObject = new JSONObject(result);
            String tranactions = jsonObject.getString("result");
            ElaTransactionRes res = new Gson().fromJson(tranactions, ElaTransactionRes.class);

            if(!ElaDataUtils.checkTx(mContext, inputAddress, outputsAddress, amount, res.Transactions)) return null;
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
                        if(callBack!=null) callBack.modifyCrcAmount(output, candidateCrcs, publickeys);
                        tmp.candidatePublicKeys = publickeys;
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
        void modifyCrcAmount(ElaOutput outputs, List<PayLoadEntity> crcs, List<PayLoadEntity> dposs);
    }


    public synchronized String sendElaRawTx(final List<BRElaTransaction> transactions){
        if(transactions==null || transactions.size()<=0) return null;
        String result = null;
        try {
            String url = ElaDataUtils.getUrlByVersion(mContext, "sendRawTx", "1");
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

            String tmp = APIClient.urlPost(url, json) /*"{\"result\":[\"8cf2df7d3205b286a1531d28225b7cb5d8c7834a282ba70499759eba31479024\"],\"status\":200}"*/;
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
            String url = ElaDataUtils.getUrlByVersion(mContext, "sendRawTx", "1");
            Log.i(TAG, "send raw url:"+url);
            String json = "{"+"\"data\"" + ":" + "\"" + rawTransaction + "\"" +"}";
            Log.i(TAG, "rawTransaction:"+rawTransaction);
            String tmp = APIClient.urlPost(url, json);
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
            String url = ElaDataUtils.getUrlByVersion(mContext,"fee", "1");
            String tmp = APIClient.urlGET(mContext, url);
            JSONObject jsonObject = new JSONObject(tmp);
            result = jsonObject.getLong("result");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public void getAllDposProducers(){
        try {
            String jsonRes = APIClient.urlGET(mContext, ElaDataUtils.getUrlByVersion(mContext,"dpos/rank/height/9999999999999999", "1"));
            if(!StringUtil.isNullOrEmpty(jsonRes) && jsonRes.contains("result")) {
                ProducersEntity producersEntity = new Gson().fromJson(jsonRes, ProducersEntity.class);
                List list = producersEntity.result;
                if(list==null || list.size()<=0) return;
                cacheAllDposProducer(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<DposProducer> queryDposProducer(String txid){
        if(StringUtil.isNullOrEmpty(txid)) return null;
        List<DposProducer> entities = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(BRSQLiteHelper.DPOS_PRODUCER_TABLE_NAME,
                    null, BRSQLiteHelper.DPOS_PRODUCER_TXID + " = ?", new String[]{txid},
                    null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                DposProducer producerEntity = ElaDataUtils.cursorToTxProducerEntity(cursor);
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

    public List<ProducerEntity> queryDposProducers(List<String> publicKeys){
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
                    ProducerEntity producerEntity = ElaDataUtils.cursorToProducerEntity(cursor);
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

    public void cacheDposProducer(List<DposProducers> entities){
        if(entities==null || entities.size()<=0) return;
        try {
            database = openDatabase();
            database.beginTransaction();
            for(DposProducers dposProducers : entities){
                if(null== dposProducers.Producer || StringUtil.isNullOrEmpty(dposProducers.Txid)) break;
                for(DposProducer dposProducer : dposProducers.Producer){
                    ContentValues value = new ContentValues();
                    value.put(BRSQLiteHelper.DPOS_PRODUCER_TXID, dposProducers.Txid);
                    value.put(BRSQLiteHelper.DPOS_PRODUCER_OWN_PUBLICKEY, dposProducer.Ownerpublickey);
                    value.put(BRSQLiteHelper.DPOS_PRODUCER_NOD_PUBLICKEY, dposProducer.Nodepublickey);
                    value.put(BRSQLiteHelper.DPOS_PRODUCER_NICKNAME, dposProducer.Nickname);
                    long l = database.insertWithOnConflict(BRSQLiteHelper.DPOS_PRODUCER_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
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

    public synchronized void cacheAllDposProducer(List<ProducerEntity> values) {
        if (values == null || values.size() <= 0) return;
        try {
            database = openDatabase();
            database.beginTransaction();

            for (ProducerEntity entity : values) {
                ContentValues value = ElaDataUtils.createProducerValues(entity);
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
}
