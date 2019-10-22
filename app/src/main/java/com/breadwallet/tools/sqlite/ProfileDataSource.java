package com.breadwallet.tools.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.presenter.entities.MyAppItem;
import com.breadwallet.presenter.entities.RegisterChainData;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.google.gson.Gson;
import com.platform.APIClient;

import org.apache.shiro.crypto.hash.SimpleHash;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileDataSource implements BRDataSourceInterface {

    private Context mContext;

    //    final String DID_URL = "https://api-wallet-did-testnet.elastos.org/";
    public static final String DID_URL = "https://api-wallet-did.elastos.org/";
//    final String elaTestUrl = "https://api-wallet-ela-testnet.elastos.org/";

    private static ProfileDataSource instance;

    private final BRSQLiteHelper dbHelper;

    private SQLiteDatabase database;

    public static ProfileDataSource getInstance(Context context) {
        if (instance == null) {
            instance = new ProfileDataSource(context);
        }
        return instance;
    }

    public ProfileDataSource(Context context) {
        this.mContext = context;
        dbHelper = BRSQLiteHelper.getInstance(context);
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

    public static class ProfileResponse {
        public String result;
        public int status;
    }

    private final String[] allColumns = {
            BRSQLiteHelper.ADD_APPS_NAME,
            BRSQLiteHelper.ADD_APPS_NAME_EN,
            BRSQLiteHelper.ADD_APPS_NAME_ZH_CN,
            BRSQLiteHelper.ADD_APPS_APP_ID,
            BRSQLiteHelper.ADD_APPS_DID,
            BRSQLiteHelper.ADD_APPS_PUBLICKEY,
            BRSQLiteHelper.ADD_APPS_ICON,
            BRSQLiteHelper.ADD_APPS_SHORTDESC,
            BRSQLiteHelper.ADD_APPS_SHORTDESC_EN,
            BRSQLiteHelper.ADD_APPS_SHORTDESC_ZH_CN,
            BRSQLiteHelper.ADD_APPS_LONGDESC_EN,
            BRSQLiteHelper.ADD_APPS_LONGDESC_ZH_CN,
            BRSQLiteHelper.ADD_APPS_DEVELOPER,
            BRSQLiteHelper.ADD_APPS_URL,
            BRSQLiteHelper.ADD_APPS_PATH,
            BRSQLiteHelper.ADD_APPS_HASH,
            BRSQLiteHelper.ADD_APPS_CATEGORY,
            BRSQLiteHelper.ADD_APPS_PLATFORM,
            BRSQLiteHelper.ADD_APPS_VERSION,
            BRSQLiteHelper.ADD_APPS_INDEX
    };

    private MyAppItem cursorToInfo(Cursor cursor) {
        MyAppItem item = new MyAppItem();
        item.name = cursor.getString(0);
        item.name_en = cursor.getString(1);
        item.name_zh_CN = cursor.getString(2);
        item.appId = cursor.getString(3);
        item.did = cursor.getString(4);
        item.publicKey = cursor.getString(5);
        item.icon = cursor.getString(6);
        item.shortDesc = cursor.getString(7);
        item.shortDesc_en = cursor.getString(8);
        item.shortDesc_zh_CN = cursor.getString(9);
        item.longDesc_en = cursor.getString(10);
        item.longDesc_zh_CN = cursor.getString(11);
        item.developer = cursor.getString(12);
        item.url = cursor.getString(13);
        item.path = cursor.getString(14);
        item.hash = cursor.getString(15);
        item.category = cursor.getString(16);
        item.platform = cursor.getString(17);
        item.version = cursor.getString(18);

        return item;
    }

    public MyAppItem getAppInfoById(String appId) {
        Cursor cursor = null;
        MyAppItem result = null;
        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.ADD_APPS_TABLE_NAME,
                    null, BRSQLiteHelper.ADD_APPS_APP_ID + " = ?", new String[]{appId}, null, null, null);

            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                result = cursorToInfo(cursor);
            }

            return result;
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }
    }

    public void deleteAppItem(String appId) {
        if (StringUtil.isNullOrEmpty(appId)) return;
        try {
            database = openDatabase();
            long l = database.delete(BRSQLiteHelper.ADD_APPS_TABLE_NAME, BRSQLiteHelper.ADD_APPS_APP_ID + " = ?", new String[]{appId});
            Log.d("test", "test");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDatabase();
        }
    }

    public void updateMyAppItem(List<MyAppItem> entities) {
        if (entities == null || entities.size() <= 0) return;
        try {
            database = openDatabase();
            database.beginTransaction();
            for (int i = 0; i < entities.size(); i++) {
                MyAppItem item = entities.get(i);
                if (item == null) return;
                ContentValues value = new ContentValues();
                value.put(BRSQLiteHelper.ADD_APPS_NAME, item.name);
                value.put(BRSQLiteHelper.ADD_APPS_NAME_EN, item.name_en);
                value.put(BRSQLiteHelper.ADD_APPS_NAME_ZH_CN, item.name_zh_CN);
                value.put(BRSQLiteHelper.ADD_APPS_APP_ID, item.appId);
                value.put(BRSQLiteHelper.ADD_APPS_DID, item.did);
                value.put(BRSQLiteHelper.ADD_APPS_PUBLICKEY, item.publicKey);
                value.put(BRSQLiteHelper.ADD_APPS_ICON, item.icon);
                value.put(BRSQLiteHelper.ADD_APPS_SHORTDESC, item.shortDesc);
                value.put(BRSQLiteHelper.ADD_APPS_SHORTDESC_EN, item.shortDesc_en);
                value.put(BRSQLiteHelper.ADD_APPS_SHORTDESC_ZH_CN, item.shortDesc_zh_CN);
                value.put(BRSQLiteHelper.ADD_APPS_LONGDESC_EN, item.longDesc_en);
                value.put(BRSQLiteHelper.ADD_APPS_LONGDESC_ZH_CN, item.longDesc_zh_CN);
                value.put(BRSQLiteHelper.ADD_APPS_DEVELOPER, item.developer);
                value.put(BRSQLiteHelper.ADD_APPS_URL, item.url);
                value.put(BRSQLiteHelper.ADD_APPS_PATH, item.path);
                value.put(BRSQLiteHelper.ADD_APPS_HASH, item.hash);
                value.put(BRSQLiteHelper.ADD_APPS_CATEGORY, item.category);
                value.put(BRSQLiteHelper.ADD_APPS_PLATFORM, item.platform);
                value.put(BRSQLiteHelper.ADD_APPS_VERSION, item.version);
                value.put(BRSQLiteHelper.ADD_APPS_INDEX, i);

                long l = database.insertWithOnConflict(BRSQLiteHelper.ADD_APPS_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            database.endTransaction();
            closeDatabase();
        }
    }

//    public void putMyAppItem(MyAppItem item){
//        if(null == item) return;
//        try {
//            database = openDatabase();
//            database.beginTransaction();
//            ContentValues value = new ContentValues();
//            value.put(BRSQLiteHelper.ADD_APPS_NAME_EN, item.name_en);
//            value.put(BRSQLiteHelper.ADD_APPS_NAME_ZH_CN, item.name_zh_CN);
//            value.put(BRSQLiteHelper.ADD_APPS_APP_ID, item.appId);
//            value.put(BRSQLiteHelper.ADD_APPS_DID, item.did);
//            value.put(BRSQLiteHelper.ADD_APPS_PUBLICKEY, item.publicKey);
//            value.put(BRSQLiteHelper.ADD_APPS_ICON_XXHDPI, item.banner_en_xxhdpi);
//            value.put(BRSQLiteHelper.ADD_APPS_SHORTDESC_EN, item.shortDesc_en);
//            value.put(BRSQLiteHelper.ADD_APPS_SHORTDESC_ZH_CN, item.shortDesc_zh_CN);
//            value.put(BRSQLiteHelper.ADD_APPS_LONGDESC_EN, item.longDesc_en);
//            value.put(BRSQLiteHelper.ADD_APPS_LONGDESC_ZH_CN, item.longDesc_zh_CN);
//            value.put(BRSQLiteHelper.ADD_APPS_DEVELOPER, item.developer);
//            value.put(BRSQLiteHelper.ADD_APPS_URL, item.url);
//            value.put(BRSQLiteHelper.ADD_APPS_PATH, item.path);
//            value.put(BRSQLiteHelper.ADD_APPS_HASH, item.hash);
//
//            long l = database.insertWithOnConflict(BRSQLiteHelper.ADD_APPS_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
//            database.setTransactionSuccessful();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            database.endTransaction();
//            closeDatabase();
//        }
//    }

    public List<MyAppItem> getMyAppItems() {
        List<MyAppItem> infos = new ArrayList<>();
        Cursor cursor = null;

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.ADD_APPS_TABLE_NAME, allColumns, null, null, null, null, "appIndex asc");

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                MyAppItem curEntity = cursorToInfo(cursor);
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

    public RegisterChainData getMiniAppSetting(String miniAppDid, String key) {
        try {
//            String url = "https://api-wallet-did.elastos.org/api/1/didexplorer/did/iiJRtAn6wyHaMSDQPS9Kkft3iiNjH5tTmi/status/normal?detailed=true";
//            String url = "https://api-wallet-did.elastos.org/api/1/didexplorer/did/iiJRtAn6wyHaMSDQPS9Kkft3iiNjH5tTmi/property_history?key=Dev/dopsvote.h5.app/Release/Web/1.0.0";
            String url = "https://api-wallet-did.elastos.org/api/1/didexplorer/did/"+miniAppDid+"/property_history?key="+key;
            String ret = urlGET(url);

            if (!StringUtil.isNullOrEmpty(ret)) {
                ProfileResponse profileResponse = new Gson().fromJson(ret, ProfileResponse.class);
                if (profileResponse.status == 200) {
                    String result = profileResponse.result;
                    JSONArray jsonArray = new JSONArray(result);
                    Log.d("test", "test");
                    if (jsonArray.length() > 0) {
                        JSONObject jsonObject = (JSONObject) jsonArray.get(0);
                        String registerKey = jsonObject.getString("key");
                        String registerValue = jsonObject.getString("value");

                        JSONObject jsonObjectValue = new JSONObject(registerValue);
                        String registerUrl = jsonObjectValue.getString("url");
                        String registerHash = jsonObjectValue.getString("hash");

                        RegisterChainData registerChainData = new RegisterChainData();
                        registerChainData.key = registerKey;
                        registerChainData.url = registerUrl;
                        registerChainData.hash = registerHash;

                        return registerChainData;
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void upchainSync(final String data) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i("ProfileFunction", "upchain data:" + data);
                    ProfileResponse result = urlPost(DID_URL + "api/1/blockagent/upchain/data", data);
                    Log.i("ProfileFunction", "result:" + result);
                } catch (IOException e) {
                    Log.i("ProfileFunction", "upchain exception");
                    e.printStackTrace();
                }
            }
        });
    }

    public String upchain(String data) {
        try {
            Log.i("ProfileFunction", "upchain data:" + data);
            ProfileResponse result = urlPost(DID_URL + "api/1/blockagent/upchain/data", data);
            Log.i("ProfileFunction", "result:" + result);
            if (200 == result.status) return result.result;
        } catch (IOException e) {
            Log.i("ProfileFunction", "upchain exception");
            e.printStackTrace();
        }

        return null;
    }

    static class Transaction {
        public String txid;
        public int confirmations;
        public int payloadversion;
        public int type;
    }

    public boolean isTxExit(String txid) {
        Transaction transaction = getTransaction(txid);
        boolean is = !(transaction == null || StringUtil.isNullOrEmpty(transaction.txid));
        Log.i("ProfileFunction", "isTxExit:" + is);
        return is;
    }

    public String getProfileValue(String did, String key) {
        String url = DID_URL + "/api/1/did/" + did + "/" + key;

        try {
            String result = urlGET(url);
            if (!StringUtil.isNullOrEmpty(result) && result.contains("200")) {
                JSONObject jsonObject = new JSONObject(result);
                result = jsonObject.getString("result");
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    private Transaction getTransaction(String txid) {
        Log.i("ProfileFunction", "getTransaction:" + txid);
        String url = DID_URL + "api/1/tx/" + txid;
        String result = null;
        try {
            result = urlGET(url);
            if (!StringUtil.isNullOrEmpty(result) && result.contains("200")) {
                JSONObject jsonObject = new JSONObject(result);
                result = jsonObject.getString("result");
                return new Gson().fromJson(result, Transaction.class);
            }
        } catch (Exception e) {
            Log.i("ProfileFunction", "getTransaction IOException");
            e.printStackTrace();
        }
        return null;
    }

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public ProfileResponse urlPost(String url, String json) throws IOException {
        String author = createHeaderAuthor();
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .header("X-Elastos-Agent-Auth", author)
                .post(body)
                .build();
        Response response = APIClient.elaClient.newCall(request).execute();
        if (response.isSuccessful()) {
            return new Gson().fromJson(response.body().string(), ProfileResponse.class);
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

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


    static class Author {
        public String id;
        public String time;
        public String auth;
    }

    private String createHeaderAuthor() {
        String acc_id = "unCZRceA8o7dbny";
        String acc_secret = "qtvb4PlRVGLYYYQxyLIo3OgyKI7kUL";

        long time = new Date().getTime();
        String strTime = String.valueOf(time);

        SimpleHash hash = new SimpleHash("md5", acc_secret, strTime);
        String auth = hash.toHex();

        Author author = new Author();
        author.id = acc_id;
        author.auth = auth;
        author.time = strTime;

        return new Gson().toJson(author);
    }
}
