package com.breadwallet.wallet.wallets.side;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.util.JsonRpcHelper;
import com.platform.APIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ElaSideEthDataSource implements BRDataSourceInterface {

    private static ElaSideEthDataSource mInstance;
    private Context mContenxt;

    public static ElaSideEthDataSource getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new ElaSideEthDataSource(context);
        }
        return mInstance;
    }

    private ElaSideEthDataSource(Context context) {
        this.mContenxt = context;
    }



    @Override
    public SQLiteDatabase openDatabase() {
        return null;
    }

    @Override
    public void closeDatabase() {

    }

    public String getBalance(String address, int rid, JsonRpcHelper.JsonRpcRequestListener listener) {
        String url = "https://api-wallet-eth.elastos.org/api/1/eth/wrap";
        JSONObject payload = new JSONObject();
        JSONArray params = new JSONArray();

        try {
            payload.put(JsonRpcHelper.JSONRPC, "2.0");
            payload.put(JsonRpcHelper.METHOD, JsonRpcHelper.ETH_BALANCE);
            params.put(address);
            params.put(JsonRpcHelper.LATEST);
            payload.put(JsonRpcHelper.PARAMS, params);
            payload.put(JsonRpcHelper.ID, rid);

            String result = urlPost(url, payload.toString());

            if (listener != null) {
                listener.onRpcRequestCompleted(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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
}
