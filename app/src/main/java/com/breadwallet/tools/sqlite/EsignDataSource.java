package com.breadwallet.tools.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.breadwallet.presenter.entities.SignHistoryItem;
import com.breadwallet.tools.util.BRConstants;

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

    public synchronized void cacheSignData(SignHistoryItem item){
        if(null == item) return;
        try {
            ContentValues value = new ContentValues();
            value.put(BRSQLiteHelper.ESIGN_SIGN_DATA, item.signData);
            value.put(BRSQLiteHelper.ESIGN_SIGNED_DATA, item.signedData);

            long l = database.insertWithOnConflict(BRSQLiteHelper.ESIGN_HISTORY_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
            database.setTransactionSuccessful();
        } catch (Exception e) {
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
}
