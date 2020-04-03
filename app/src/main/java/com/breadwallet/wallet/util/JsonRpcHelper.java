package com.breadwallet.wallet.util;

import android.content.Context;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.BuildConfig;
import com.breadwallet.tools.util.Utils;
import com.platform.APIClient;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 6/5/18.
 * Copyright (c) 2018 breadwallet LLC
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
public class JsonRpcHelper {
    private static final String BRD_ETH_RPC_ENDPOINT = BuildConfig.BITCOIN_TESTNET ? "/ethq/ropsten/proxy" : "/ethq/mainnet/proxy";
    private static final String BRD_ETH_TX_ENDPOINT = BuildConfig.BITCOIN_TESTNET ? "/ethq/ropsten/" : "/ethq/mainnet/";
    private static final String PROTOCOL = "https";
    public static final String METHOD = "method";
    public static final String JSONRPC = "jsonrpc";
    public static final String ETH_BALANCE = "eth_getBalance";
    public static final String LATEST = "latest";
    public static final String PARAMS = "params";
    public static final String ID = "id";
    public static final String RESULT = "result";
    public static final String ACCOUNT = "account";
    public static final String ETH_GAS_PRICE = "eth_gasPrice";
    public static final String ETH_ESTIMATE_GAS = "eth_estimateGas";
    public static final String ETH_SEND_RAW_TRANSACTION = "eth_sendRawTransaction";
    public static final String ERROR = "error";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";
    public static final String HASH = "hash";
    public static final String TO = "to";
    public static final String FROM = "from";
    public static final String CONTRACT_ADDRESS = "contractAddress";
    public static final String ADDRESS = "address";
    public static final String VALUE = "value";
    public static final String GAS = "gas";
    public static final String GAS_PRICE = "gasPrice";
    public static final String NONCE = "nonce";
    public static final String GAS_USED = "gasUsed";
    public static final String BLOCK_NUMBER = "blockNumber";
    public static final String ETH_BLOCK_NUMBER = "eth_blockNumber";
    public static final String ETH_TRANSACTION_COUNT = "eth_getTransactionCount";
    public static final String BLOCK_HASH = "blockHash";
    public static final String LOG_INDEX = "logIndex";
    public static final String INPUT = "input";
    public static final String CONFIRMATIONS = "confirmations";
    public static final String TRANSACTION_INDEX = "transactionIndex";
    public static final String TIMESTAMP = "timeStamp";
    public static final String IS_ERROR = "isError";
    public static final String TOPICS = "topics";
    public static final String DATA = "data";
    public static final String TRANSACTION_HASH = "transactionHash";

    private JsonRpcHelper() {
    }

    public interface JsonRpcRequestListener {

        void onRpcRequestCompleted(String jsonResult);
    }

    public static String getEthereumRpcUrl() {
        return PROTOCOL + "://" + BreadApp.HOST + JsonRpcHelper.BRD_ETH_RPC_ENDPOINT;
    }

    public static String createTokenTransactionsUrl(String address, String contractAddress) {
        return PROTOCOL + "://" + BreadApp.HOST + BRD_ETH_TX_ENDPOINT + "query?" + "module=account&action=tokenbalance"
                + "&address=" + address + "&contractaddress=" + contractAddress;
    }

    public static String createEthereumTransactionsUrl(String address) {
        return PROTOCOL + "://" + BreadApp.HOST + BRD_ETH_TX_ENDPOINT
                + "query?module=account&action=txlist&address=" + address;
    }

    public static String createLogsUrl(String address, String contract, String event) {

        //https://api-eth.elaphant.app/api/1/eth/getLogs?fromBlock=0&toBlock=latest&topic0=0xdac17f958d2ee523a2206206994597c13d831ec7&topic1=0x0000000000000000000000003D411b1632bFeC70d2aA83D1296849fF73246e75&topic1_2_opr=or&topic2=0x0000000000000000000000003D411b1632bFeC70d2aA83D1296849fF73246e75

        return "https://api-eth.elaphant.app/api/1/eth/getLogs?"
                + "&fromBlock=0&toBlock=latest"
                + (null == contract ? "" : ("&address=" + contract))
                + "&topic0=" + event
                + "&topic1=" + address
                + "&topic1_2_opr=or"
                + "&topic2=" + address;
    }

    ///api/1/eth/getLogs?fromBlock=0&toBlock=latest&topic0=0x89d24a6b4ccb1b6faa2625fe562bdd9a23260359&topic1=0x000000000000000000000000e418a0e203f36cb843079f6ebf0b367e48774ac1&topic1_2_opr=or&topic2=0x000000000000000000000000829bd824b016326a401d083b33d092293333a830
    public static String createElaEthLogsUrl(String host, String url, String event, String address) {
        return "https://"
                + host
                + url
                + "?fromBlock=0&toBlock=latest&"
                + "&topic0=" + event
                + "&topic1=" + address
                + "&topic1_2_opr=or"
                + "&topic2=" + address;
    }

    @WorkerThread
    public static void makeRpcRequest(Context app, String url, JSONObject payload, JsonRpcRequestListener listener) {
        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

        RequestBody requestBody = RequestBody.create(JSON, payload.toString());

        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .post(requestBody).build();


        APIClient.BRResponse resp = APIClient.getInstance(app).sendRequest(request, true);
        String responseString = resp.getBodyText();

        if (listener != null) {
            listener.onRpcRequestCompleted(responseString);
        }

    }

    @WorkerThread
    public static void makeRpcRequest(Context app, String url, JsonRpcRequestListener listener) {

        //https://api-eth.elaphant.app/api/1/eth/getLogs?fromBlock=0
        // &toBlock=latest
        // &topic0=0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef
        // &topic1=0x000000000000000000000000544976511F2B6237b0b4Fe8fbd271B08164dD1b6
        // &topic1_2_opr=or
        // &topic2=0x000000000000000000000000544976511F2B6237b0b4Fe8fbd271B08164dD1b6

        //https://api-eth.elaphant.app/api/1/eth/getLogs?fromBlock=0
        // &toBlock=latest
        // &topic0=0xdac17f958d2ee523a2206206994597c13d831ec7
        // &topic1=0x000000000000000000000000544976511F2B6237b0b4Fe8fbd271B08164dD1b6
        // &topic1_2_opr=or
        // &topic2=0x000000000000000000000000544976511F2B6237b0b4Fe8fbd271B08164dD1b6
        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .build();


        APIClient.BRResponse resp = APIClient.getInstance(app).sendRequest(request, true);
        String responseString = resp.getBodyText();

        if (listener != null) {
            listener.onRpcRequestCompleted(responseString);
        }

//        try {
//            String responseString = urlPost(url, payload.toString());
//            if (listener != null) {
//                listener.onRpcRequestCompleted(responseString);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        try {
//            String tmp = urlGET(app, "https://api-eth.elaphant.app/api/1/eth/token/balance?address=0x289B44672d8499A51130d65d2087A151c4e45966&contractaddress=0xa8cac329f783edac931815c5466e283d48c9d7f7");
//            Log.d("test", "test");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    //TODO test
    @WorkerThread
    public static void makeRpcRequest2(Context app, String url, JSONObject payload, JsonRpcRequestListener listener) {
        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

        RequestBody requestBody = RequestBody.create(JSON, payload.toString());

        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .post(requestBody).build();


        APIClient.BRResponse resp = APIClient.getInstance(app).sendRequest(request, true);
        String responseString = resp.getBodyText();

        if (listener != null) {
            listener.onRpcRequestCompleted(responseString);
        }

//        try {
//            String responseString = urlPost(url, payload.toString());
//            if (listener != null) {
//                listener.onRpcRequestCompleted(responseString);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        try {
//            String tmp = urlGET(app, "https://api-eth.elaphant.app/api/1/eth/token/balance?address=0x289B44672d8499A51130d65d2087A151c4e45966&contractaddress=0xa8cac329f783edac931815c5466e283d48c9d7f7");
//            Log.d("test", "test");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }


    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static String urlPost(String url, String json) throws IOException {
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

    public static String urlGET(Context app, String myURL) throws IOException {
        Map<String, String> headers = BreadApp.getBreadHeaders();

        Request.Builder builder = new Request.Builder()
                .url(myURL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-agent", Utils.getAgentString(app, "android/HttpURLConnection"))
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
