package com.breadwallet.presenter.activities.crc;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.vote.CityEntity;
import com.breadwallet.vote.CrcRankEntity;
import com.breadwallet.vote.CrcsRankEntity;
import com.google.gson.Gson;
import com.platform.APIClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.Request;
import okhttp3.Response;

public class CrcDataSource implements BRDataSourceInterface {


    private final String[] crcMemberColumn = {
            BRSQLiteHelper.CRC_VOTE_DID,
            BRSQLiteHelper.CRC_VOTE_RANK,
            BRSQLiteHelper.CRC_VOTE_NICKNAME,
            BRSQLiteHelper.CRC_VOTE_LOCATION,
            BRSQLiteHelper.CRC_VOTE_AREA,
            BRSQLiteHelper.CRC_VOTE_VOTES,
            BRSQLiteHelper.CRC_VOTE_VALUE,
    };


    public synchronized void cacheMultMembers(List<CrcRankEntity> crcRankEntities){
        if(crcRankEntities == null) return;
        try {
            database = openDatabase();
            database.beginTransaction();

            for(CrcRankEntity entity : crcRankEntities){

                ContentValues value = new ContentValues();
                value.put(BRSQLiteHelper.CRC_VOTE_DID, entity.Did);
                value.put(BRSQLiteHelper.CRC_VOTE_RANK, entity.Rank);
                value.put(BRSQLiteHelper.CRC_VOTE_NICKNAME, entity.Nickname);
                value.put(BRSQLiteHelper.CRC_VOTE_LOCATION, entity.Location);
                value.put(BRSQLiteHelper.CRC_VOTE_AREA, entity.Area);
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

    public List<CrcRankEntity> getMembersByRank() {
        return getMembersByRank("desc");
    }

    public List<CrcRankEntity> getMembersByRank(String orderBy) {
        List<CrcRankEntity> currencies = new ArrayList<>();
        Cursor cursor = null;

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.ELA_TX_TABLE_NAME, crcMemberColumn, null, null, null, null, "crcVoteRank " + orderBy);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                CrcRankEntity curEntity = cursorToMemberEntity(cursor);
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

    public List<CrcRankEntity> getMembersByIds(List<String> ids) {
        List<CrcRankEntity> currencies = new ArrayList<>();
        List<CrcRankEntity> tmp = getMembersByRank("desc");
        for(CrcRankEntity entity : tmp) {
            for(String id : ids) {
                if(id.equals(entity.Did)) {
                    currencies.add(entity);
                }
            }
        }

        return currencies;
    }

    public void updateMessage(List<CityEntity> cityEntities) {
        try {
            database = openDatabase();

            for (CityEntity entity : cityEntities) {
                ContentValues args = new ContentValues();
                args.put(BRSQLiteHelper.CRC_VOTE_AREA, entity.location);

                int r = database.update(BRSQLiteHelper.CHAT_MESSAGE_TABLE_NAME, args, BRSQLiteHelper.CRC_VOTE_LOCATION + " = ? ", new String[]{String.valueOf(entity.code)});

            }
        } finally {
            closeDatabase();
        }
    }


    private CrcRankEntity cursorToMemberEntity(Cursor cursor) {
        return new CrcRankEntity(cursor.getString(0),
                cursor.getInt(1),
                cursor.getString(2),
                cursor.getInt(3),
                cursor.getString(4),
                cursor.getString(5),
                cursor.getString(6));
    }

    public void getCrcWithRank() {
        try {
            String url = getUrlByVersion("crc/rank/height/241762000?state=active", "v1");
            String result = urlGET(url);
            cacheMultMembers(new Gson().fromJson(result, CrcsRankEntity.class).result);
        } catch (Exception e) {
            e.printStackTrace();
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
