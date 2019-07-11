package com.breadwallet.tools.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.breadwallet.presenter.entities.SignHistoryItem;
import com.breadwallet.tools.util.BRConstants;

import java.util.ArrayList;
import java.util.List;

public class EsignDataSource implements BRDataSourceInterface {

    private static final String TAG = EsignDataSource.class.getSimpleName();

    private final BRSQLiteHelper dbHelper;

    private SQLiteDatabase database;

    private static Context mContext;

    private static EsignDataSource mInstance;

    private EsignDataSource(Context context){
        mContext = context;
        dbHelper = BRSQLiteHelper.getInstance(context);
    }

    public static EsignDataSource getInstance(Context context){
        if(mInstance == null){
            mInstance = new EsignDataSource(context);
        }

        return mInstance;
    }

    public synchronized void putSignData(SignHistoryItem item){
        if(null == item) return;
        try {
            database = openDatabase();
            database.beginTransaction();
            ContentValues value = new ContentValues();
            value.put(BRSQLiteHelper.ESIGN_SIGN_DATA, item.signData);
            value.put(BRSQLiteHelper.ESIGN_SIGNED_DATA, item.signedData);
            value.put(BRSQLiteHelper.ESIGN_SIGN_TIME, item.time);

            long l = database.insertWithOnConflict(BRSQLiteHelper.ESIGN_HISTORY_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
            database.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            database.endTransaction();
            closeDatabase();
        }
    }

    private final String[] allColumns = {
            BRSQLiteHelper.ESIGN_SIGN_TIME,
            BRSQLiteHelper.ESIGN_SIGN_DATA,
            BRSQLiteHelper.ESIGN_SIGNED_DATA
    };

    public synchronized List<SignHistoryItem> getSignData(){
        List<SignHistoryItem> infos = new ArrayList<>();
        Cursor cursor = null;

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.ESIGN_HISTORY_TABLE_NAME, allColumns, null, null, null, null, "timeStamp desc");

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                SignHistoryItem curEntity = cursorToInfo(cursor);
                infos.add(curEntity);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return infos;
    }

    private SignHistoryItem cursorToInfo(Cursor cursor){
        SignHistoryItem item = new SignHistoryItem();
        item.time = cursor.getLong(0);
        item.signData = cursor.getString(1);
        item.signedData = cursor.getString(2);

        return item;
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
