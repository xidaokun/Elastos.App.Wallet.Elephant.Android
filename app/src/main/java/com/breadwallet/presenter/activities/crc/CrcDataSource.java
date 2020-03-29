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
import com.breadwallet.vote.CrcsEntity;
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


    private final String[] crcMemberColumn = {
            BRSQLiteHelper.CRC_VOTE_DID,
            BRSQLiteHelper.CRC_VOTE_RANK,
            BRSQLiteHelper.CRC_VOTE_NICKNAME,
            BRSQLiteHelper.CRC_VOTE_LOCATION,
            BRSQLiteHelper.CRC_VOTE_VOTES,
            BRSQLiteHelper.CRC_VOTE_VALUE,
    };

    private CrcEntity cursorToMemberEntity(Cursor cursor) {
        return new CrcEntity(cursor.getString(0),
                cursor.getInt(1),
                cursor.getString(2),
                cursor.getInt(3),
                cursor.getString(4),
                cursor.getString(5));
    }

    public synchronized void cacheCrcs(List<CrcEntity> crcEntities){
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

    public List<CrcEntity> queryCrcsByRank() {
        return queryCrcsByRank("desc");
    }

    public List<CrcEntity> queryCrcsByRank(String orderBy) {
        List<CrcEntity> currencies = new ArrayList<>();
        Cursor cursor = null;

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.CRC_VOTE_TABLE_NAME, crcMemberColumn, null, null, null, null, "rank " + orderBy);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                CrcEntity curEntity = cursorToMemberEntity(cursor);
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

    public List<CrcEntity> queryCrcsByIds(List<String> ids) {
        List<CrcEntity> memebers = new ArrayList<>();
        List<CrcEntity> tmp = queryCrcsByRank("desc");
        for(CrcEntity entity : tmp) {
            for(String id : ids) {
                if(id.equals(entity.Did)) {
                    memebers.add(entity);
                }
            }
        }

        return memebers;
    }

    public void getCrcPayload(String txid) {
        try {
            //https://node3.elaphant.app/api/v1/transaction/9b570bd355ca7a3d237f1eb3635f3838042054741b53df16b46f1547966e714f
            String url = getUrlByVersion("transaction/"+txid, "v1");
            String result = APIClient.urlGET(mContext, url);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void getAndCacheCrcs() {
        try {
            String url = getUrlByVersion("crc/rank/height/241762000?state=active", "v1");
            String result = APIClient.urlGET(mContext, url);
            cacheCrcs(new Gson().fromJson(result, CrcsEntity.class).result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
