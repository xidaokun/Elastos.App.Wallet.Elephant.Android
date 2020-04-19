package com.breadwallet.presenter.activities.crc;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.vote.CityEntity;
import com.breadwallet.vote.CrcEntity;
import com.breadwallet.vote.CrcTxEntity;
import com.breadwallet.vote.CrcsEntity;
import com.breadwallet.wallet.wallets.ela.ElaDataUtils;
import com.google.gson.Gson;
import com.platform.APIClient;

import java.util.ArrayList;
import java.util.List;

public class CrcDataSource implements BRDataSourceInterface {


    private final String[] crcCityColumn = {
            BRSQLiteHelper.CRC_CITY_CODE,
            BRSQLiteHelper.CRC_CITY_EN,
            BRSQLiteHelper.CRC_CITY_ZH
    };

    public void cacheCrcCity(List<CityEntity> cityEntities) {
        if(null == cityEntities) return;
        try {
            database = openDatabase();
            database.beginTransaction();

            for(CityEntity entity : cityEntities){

                ContentValues value = new ContentValues();
                value.put(BRSQLiteHelper.CRC_CITY_CODE, entity.code);
                value.put(BRSQLiteHelper.CRC_CITY_EN, entity.en);
                value.put(BRSQLiteHelper.CRC_CITY_ZH, entity.zh);

                long l = database.insertWithOnConflict(BRSQLiteHelper.CRC_CIRY_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
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

    public void updateCrcsArea(List<CrcEntity> crcEntities) {
        try {
            database = openDatabase();

            for (CrcEntity entity : crcEntities) {
                ContentValues args = new ContentValues();
                args.put(BRSQLiteHelper.CRC_CITY_CODE, entity.Location);

                Cursor cursor = database.query(BRSQLiteHelper.CRC_CIRY_TABLE_NAME, crcCityColumn, BRSQLiteHelper.CRC_CITY_CODE + " = ? ", new String[]{String.valueOf(entity.Location)}, null, null, null);

                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    entity.AreaEn = cursor.getString(1);
                    entity.AreaZh = cursor.getString(2);
                    cursor.moveToNext();
                }
            }
        } finally {
            closeDatabase();
        }
    }

    public synchronized void cacheActiveCrcs(List<CrcEntity> crcEntities){
        if(crcEntities == null) return;
        try {
            database = openDatabase();
            database.beginTransaction();

            for(CrcEntity entity : crcEntities){

                ContentValues value = new ContentValues();
                value.put(BRSQLiteHelper.CRC_VOTE_DID, entity.Did);
                value.put(BRSQLiteHelper.CRC_VOTE_RANK, entity.Rank);
                value.put(BRSQLiteHelper.CRC_VOTE_NICKNAME, entity.Nickname);
                value.put(BRSQLiteHelper.CRC_VOTE_LOCATION, entity.Location);
                value.put(BRSQLiteHelper.CRC_VOTE_VOTES, entity.Votes);
                value.put(BRSQLiteHelper.CRC_VOTE_VALUE, entity.Value);

                long l = database.insertWithOnConflict(BRSQLiteHelper.CRC_VOTE_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
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

    public List<CrcEntity> queryCrcsByIds(List<String> ids, List<String> votes) {
        if(ids == null) return null;

        List<CrcEntity> result = new ArrayList<>();
        result.clear();
        try {
            database = openDatabase();

            for (int i=0; i<ids.size(); i++) {
                Cursor cursor = database.query(BRSQLiteHelper.CRC_VOTE_TABLE_NAME, ElaDataUtils.crcMemberColumn, BRSQLiteHelper.CRC_VOTE_DID + " = ? ", new String[]{ids.get(i)}, null, null, null);

                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    CrcEntity entity = new CrcEntity();
                    if(null!=votes) entity.Vote = votes.get(i);
                    ElaDataUtils.cursorToMemberEntity(cursor, entity);
                    result.add(entity);

                    cursor.moveToNext();
                }
            }
        } finally {
            closeDatabase();
        }

        return result;
    }


    public List<CrcEntity> queryCrcsByIds(List<String> ids) {
        return queryCrcsByIds(ids, null);
    }

    //TODO 代替crc/producer接口
    public void getCrcPayload(String txid) {
        try {
            List<CrcTxEntity.Candidates> candidates = queryCrcPayload(txid);
            if(candidates!=null && candidates.size()>0) return;
            //https://node3.elaphant.app/api/v1/transaction/9b570bd355ca7a3d237f1eb3635f3838042054741b53df16b46f1547966e714f
            String url = getUrlByVersion("transaction/"+txid, "v1");
            String result = APIClient.urlGET(mContext, url);

            CrcTxEntity crcTxEntity = new Gson().fromJson(result, CrcTxEntity.class);
            CrcTxEntity.Vout lastVout = crcTxEntity.Result.vout.get(crcTxEntity.Result.vout.size()-1);
            List<CrcTxEntity.Contents> contents = lastVout.payload.contents;
            for(CrcTxEntity.Contents content : contents) {
                int votetype = content.votetype;
                if(1 == votetype) {
                    candidates = content.candidates;
                    break;
                }
            }
            cacheCrcPayload(txid, candidates);
            Log.d("test", "test");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<CrcTxEntity.Candidates> queryCrcPayload(String txid) {
        if(StringUtil.isNullOrEmpty(txid)) return null;
        List<CrcTxEntity.Candidates> candidates = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.CRC_HISTORY_TABLE_NAME,
                    ElaDataUtils.crcPayloadColumn, BRSQLiteHelper.CRC_HISTORY_TXID + " = ?", new String[]{txid},
                    null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                CrcTxEntity.Candidates candidate = ElaDataUtils.cursorToCrcPayload(cursor);
                candidates.add(candidate);
                cursor.moveToNext();
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return candidates;
    }

    public void cacheCrcPayload(String txid, List<CrcTxEntity.Candidates> candidates) {
        if(candidates==null || candidates.size()<=0) return;
        try {
            database = openDatabase();
            database.beginTransaction();
            for(CrcTxEntity.Candidates candidate : candidates){
                ContentValues value = new ContentValues();
                value.put(BRSQLiteHelper.CRC_HISTORY_TXID, txid);
                value.put(BRSQLiteHelper.CRC_HISTORY_DID, candidate.candidate);
                value.put(BRSQLiteHelper.CRC_HISTORY_VOTE, candidate.votes);
                long l = database.insertWithOnConflict(BRSQLiteHelper.CRC_HISTORY_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
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

//    public void getCrcProducer(List<String> txids) {
//        if(txids==null || txids.size() <= 0) return;
//        CrcProducerResult crcProducerResult = null;
//        try {
//            ProducerTxid producerTxid = new ProducerTxid();
//            producerTxid.txid = txids;
//            String json = new Gson().toJson(producerTxid);
//            String url = ElaDataUtils.getUrlByVersion(mContext,"crc/transaction/producer", "1");
//            String result = APIClient.urlPost(url, json);
//            crcProducerResult = new Gson().fromJson(result, CrcProducerResult.class);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        if(crcProducerResult ==null || crcProducerResult.result==null) return;
//        cacheCrcProducer(crcProducerResult.result);
//    }


    public void getAndCacheActiveCrcs() {
        try {
            String url = getUrlByVersion("crc/rank/height/241762000?state=active", "v1");
            String result = APIClient.urlGET(mContext, url);
            cacheActiveCrcs(new Gson().fromJson(result, CrcsEntity.class).result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public void getAndCacheActiveDpos() {
//        try {
//            String url = getUrlByVersion("dpos/rank/height/241762000?state=active", "v1");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }

//    public void cacheCrcProducer(List<CrcProducerResult.CrcProducers> entities) {
//        if(entities==null || entities.size()<=0) return;
//        try {
//            database = openDatabase();
//            database.beginTransaction();
//            for(CrcProducerResult.CrcProducers crcProducers : entities){
//                if(null== crcProducers.Producer || StringUtil.isNullOrEmpty(crcProducers.Txid)) break;
//                for(CrcProducerResult.CrcProducer crcProducer : crcProducers.Producer){
//                    ContentValues value = new ContentValues();
//                    value.put(BRSQLiteHelper.CRC_PRODUCER_TXID, crcProducers.Txid);
//                    value.put(BRSQLiteHelper.CRC_PRODUCER_DID, crcProducer.Did);
//                    value.put(BRSQLiteHelper.CRC_PRODUCER_LOCATION, crcProducer.Location);
//                    value.put(BRSQLiteHelper.CRC_PRODUCER_STATE, crcProducer.State);
//                    long l = database.insertWithOnConflict(BRSQLiteHelper.CRC_PRODUCER_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
//                }
//            }
//            database.setTransactionSuccessful();
//        } catch (Exception e) {
//            closeDatabase();
//            e.printStackTrace();
//        } finally {
//            database.endTransaction();
//            closeDatabase();
//        }
//    }

//    public List<String> queryCrcProducerByTx(String txid){
//        if(StringUtil.isNullOrEmpty(txid)) return null;
//        List<String> entities = new ArrayList<>();
//        Cursor cursor = null;
//        try {
//            database = openDatabase();
//            cursor = database.query(BRSQLiteHelper.CRC_PRODUCER_TABLE_NAME,
//                    ElaDataUtils.crcProducerColumn, BRSQLiteHelper.CRC_PRODUCER_TXID + " = ?", new String[]{txid},
//                    null, null, null);
//            cursor.moveToFirst();
//            while (!cursor.isAfterLast()) {
//                CrcProducerResult.CrcProducer producerEntity = ElaDataUtils.cursorToCrcProducer(cursor);
//                entities.add(producerEntity.Did);
//                cursor.moveToNext();
//            }
//        } catch (Exception e){
//            e.printStackTrace();
//        } finally {
//            if (cursor != null)
//                cursor.close();
//            closeDatabase();
//        }
//
//        return entities;
//    }

    public List<CrcProducerResult.CrcProducer> queryCrcProducerByDid(List<String> dids) {
        if(dids == null) return null;

        List<CrcProducerResult.CrcProducer> result = new ArrayList<>();
        result.clear();
        try {
            database = openDatabase();

            for (int i=0; i<dids.size(); i++) {
                Cursor cursor = database.query(BRSQLiteHelper.CRC_PRODUCER_TABLE_NAME, ElaDataUtils.crcProducerColumn, BRSQLiteHelper.CRC_PRODUCER_DID + " = ? ", new String[]{dids.get(i)}, null, null, null);

                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    CrcProducerResult.CrcProducer producerEntity = ElaDataUtils.cursorToCrcProducer(cursor);
                    result.add(producerEntity);
                    cursor.moveToNext();
                }
            }
        } finally {
            closeDatabase();
        }

        return result;
    }

//    static class ProducerTxid {
//        public List<String> txid;
//    }

    public String getUrlByVersion(String api, String version) {
        String node = BRSharedPrefs.getElaNode(mContext, ELA_NODE_KEY);
        if(StringUtil.isNullOrEmpty(node)) node = ELA_NODE;
        return new StringBuilder("https://").append(node).append("/api/").append(version).append("/").append(api).toString();
    }

    private static final String TAG = CrcDataSource.class.getSimpleName();

    public static final String ELA_NODE_KEY = "elaNodeKey";

    public static final String ELA_NODE =  "node1.elaphant.app";

    private final BRSQLiteHelper dbHelper;

    private SQLiteDatabase database;

    private static Context mContext;

    private static CrcDataSource mInstance;

    private CrcDataSource(Context context){
        mContext = context;
        dbHelper = BRSQLiteHelper.getInstance(context);
    }

    public static CrcDataSource getInstance(Context context){
        if(mInstance == null){
            mInstance = new CrcDataSource(context);
        }

        return mInstance;
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
