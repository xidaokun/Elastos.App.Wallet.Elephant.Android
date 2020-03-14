package org.chat.lib.push;

import android.util.Base64;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PushRequest {
    private static final String TAG = PushRequest.class.getName();
    private static final String HOST_URL = "https://cloudpush.aliyuncs.com/";
    private static final String ACTION = "Push";
    private static final String METHOD = "GET";
    private final static String SEPARATOR = "&";

    private String mAccessKeyId;
    private String mAccessSecret;

    private HashMap<String, String> mQueryParameters = new HashMap<>();
    OkHttpClient mClient = new OkHttpClient();

    public PushRequest(String accessKeyId, String accessSecret) {
        mAccessKeyId = accessKeyId;
        mAccessSecret = accessSecret;
        mQueryParameters.put("Action", ACTION);
    }

    public void setDeviceType(String type) {
        if (Util.isNullOrEmpty(type)) return;
        mQueryParameters.put("DeviceType", type);
    }

    public String getDeviceType() {
        return mQueryParameters.get("DeviceType");
    }

    public void setAppKey(String appKey) {
        if (Util.isNullOrEmpty(appKey)) return;
        mQueryParameters.put("AppKey", appKey);
    }

    public String getAppKey() {
        return mQueryParameters.get("AppKey");
    }

    public void setTarget(String target) {
        if (Util.isNullOrEmpty(target)) return;
        mQueryParameters.put("Target", target);
    }

    public String getTarget() {
        return mQueryParameters.get("Target");
    }

    public void setTargetValue(String value) {
        if (Util.isNullOrEmpty(value)) return;
        mQueryParameters.put("TargetValue", value);
    }

    public String getTargetValue() {
        return mQueryParameters.get("TargetValue");
    }


    public void setPushType(String type) {
        if (Util.isNullOrEmpty(type)) return;
        mQueryParameters.put("PushType", type);
    }

    public String getPushType() {
        return mQueryParameters.get("PushType");
    }


    public void setBody(String body) {
        if (Util.isNullOrEmpty(body)) return;
        mQueryParameters.put("Body", body);
    }

    public String getBody() {
        return mQueryParameters.get("Body");
    }

    public void setTitle(String title) {
        if (Util.isNullOrEmpty(title)) return;
        mQueryParameters.put("Title", title);
    }

    public String getTitle() {
        return mQueryParameters.get("Title");
    }


    //iOS config
    public void setIOSMusic(String music) {
        if (Util.isNullOrEmpty(music)) return;
        mQueryParameters.put("iOSMusic", music);
    }

    public String getIOSMusic() {
        return mQueryParameters.get("iOSMusic");
    }

    public void setIOSBadge(Integer data) {
        if (data == null) return;
        mQueryParameters.put("iOSBadge", data.toString());
    }

    public Integer getIOSBadge() {
        String badge = mQueryParameters.get("iOSBadge");
        if (badge == null) return null;
        return Integer.parseInt(badge);
    }

    public void setIOSBadgeAutoIncrement(Boolean data) {
        if (data == null) return;
        mQueryParameters.put("iOSBadgeAutoIncrement", data.toString());
    }

    public Boolean getIOSBadgeAutoIncrement() {
        String data = mQueryParameters.get("iOSBadgeAutoIncrement");
        if (data == null) return null;
        return Boolean.parseBoolean(data);
    }

    public void setIOSSilentNotification(Boolean data) {
        if (data == null) return;
        mQueryParameters.put("iOSSilentNotification", data.toString());
    }

    public Boolean getIOSSilentNotification() {
        String data = mQueryParameters.get("iOSSilentNotification");
        if (data == null) return null;
        return Boolean.parseBoolean(data);
    }

    public void setIOSSubtitle(String data) {
        if (Util.isNullOrEmpty(data)) return;
        mQueryParameters.put("iOSSubtitle", data);
    }

    public String getIOSSubtitle() {
        return mQueryParameters.get("iOSSubtitle");
    }

    public void setIOSNotificationCategory(String music) {
        if (Util.isNullOrEmpty(music)) return;
        mQueryParameters.put("iOSNotificationCategory", music);
    }

    public String getIOSNotificationCategory() {
        return mQueryParameters.get("iOSNotificationCategory");
    }

    public void setIOSMutableContent(Boolean data) {
        if (data == null) return;
        mQueryParameters.put("iOSMutableContent", data.toString());
    }

    public void setiOSExtParameters(String data) {
        if (data == null) return;
        mQueryParameters.put("iOSExtParameters", data);
    }

    public Boolean getIOSMutableContent() {
        String data = mQueryParameters.get("iOSMutableContent");
        if (data == null) return null;
        return Boolean.parseBoolean(data);
    }


    public void setIOSApnsEnv(String data) {
        if (Util.isNullOrEmpty(data)) return;
        mQueryParameters.put("iOSApnsEnv", data);
    }

    public String getIOSApnsEnv() {
        return mQueryParameters.get("iOSApnsEnv");
    }


    public void setIOSRemind(Boolean data) {
        if (data == null) return;
        mQueryParameters.put("iOSRemind", data.toString());
    }

    public Boolean getIOSRemind() {
        String data = mQueryParameters.get("iOSRemind");
        if (data == null) return null;
        return Boolean.parseBoolean(data);
    }

    public void setIOSRemindBody(String data) {
        if (Util.isNullOrEmpty(data)) return;
        mQueryParameters.put("iOSRemindBody", data);
    }

    public String getIOSRemindBody() {
        return mQueryParameters.get("iOSRemindBody");
    }


    // Android config
    public void setAndroidOpenType(String data) {
        if (Util.isNullOrEmpty(data)) return;
        mQueryParameters.put("AndroidOpenType", data);
    }

    public void setAndroidExtParameters(String data) {
        if (Util.isNullOrEmpty(data)) return;
        mQueryParameters.put("AndroidExtParameters", data);
    }

    public String getAndroidOpenType() {
        return mQueryParameters.get("AndroidOpenType");
    }

    public void setAndroidNotifyType(String data) {
        if (Util.isNullOrEmpty(data)) return;
        mQueryParameters.put("AndroidNotifyType", data);
    }

    public String getAndroidNotifyType() {
        return mQueryParameters.get("AndroidNotifyType");
    }

    public void setAndroidActivity(String data) {
        if (Util.isNullOrEmpty(data)) return;
        mQueryParameters.put("AndroidActivity", data);
    }

    public String getAndroidActivity() {
        return mQueryParameters.get("AndroidActivity");
    }

    public void setAndroidOpenUrl(String data) {
        if (Util.isNullOrEmpty(data)) return;
        mQueryParameters.put("AndroidOpenUrl", data);
    }

    public String getAndroidOpenUrl() {
        return mQueryParameters.get("AndroidOpenUrl");
    }

    public void setAndroidNotificationBarType(Integer data) {
        if (data == null) return;
        mQueryParameters.put("AndroidNotificationBarType", data.toString());
    }

    public Integer getAndroidNotificationBarType() {
        String badge = mQueryParameters.get("AndroidNotificationBarType");
        if (badge == null) return null;
        return Integer.parseInt(badge);
    }

    public void setAndroidNotificationBarPriority(Integer data) {
        if (data == null) return;
        mQueryParameters.put("AndroidNotificationBarPriority", data.toString());
    }

    public Integer getAndroidNotificationBarPriority() {
        String badge = mQueryParameters.get("AndroidNotificationBarPriority");
        if (badge == null) return null;
        return Integer.parseInt(badge);
    }

    public void setAndroidNotificationChannel(String data) {
        if (Util.isNullOrEmpty(data)) return;
        mQueryParameters.put("AndroidNotificationChannel", data);
    }

    public String getAndroidNotificationChannel() {
        return mQueryParameters.get("AndroidNotificationChannel");
    }

    public void setAndroidRemind(Boolean data) {
        if (data == null) return;
        mQueryParameters.put("AndroidRemind", data.toString());
    }

    public Boolean getAndroidRemind() {
        String data = mQueryParameters.get("AndroidRemind");
        if (data == null) return null;
        return Boolean.parseBoolean(data);
    }

    public void setAndroidPopupActivity(String data) {
        if (Util.isNullOrEmpty(data)) return;
        mQueryParameters.put("AndroidPopupActivity", data);
    }

    public String getAndroidPopupActivity() {
        return mQueryParameters.get("AndroidPopupActivity");
    }

    public void setAndroidPopupTitle(String data) {
        if (Util.isNullOrEmpty(data)) return;
        mQueryParameters.put("AndroidPopupTitle", data);
    }

    public String getAndroidPopupTitle() {
        return mQueryParameters.get("AndroidPopupTitle");
    }

    public void setAndroidPopupBody(String data) {
        if (Util.isNullOrEmpty(data)) return;
        mQueryParameters.put("AndroidPopupBody", data);
    }

    public String getAndroidPopupBody() {
        return mQueryParameters.get("AndroidPopupBody");
    }

    // push control
    public void setPushTime(String data) {
        if (Util.isNullOrEmpty(data)) return;
        mQueryParameters.put("PushTime", data);
    }

    public String getPushTime() {
        return mQueryParameters.get("PushTime");
    }

    public void setStoreOffline(Boolean data) {
        if (data == null) return;
        mQueryParameters.put("StoreOffline", data.toString());
    }

    public Boolean getStoreOffline() {
        String data = mQueryParameters.get("StoreOffline");
        if (data == null) return null;
        return Boolean.parseBoolean(data);
    }

    public void setExpireTime(String data) {
        if (Util.isNullOrEmpty(data)) return;
        mQueryParameters.put("ExpireTime", data);
    }

    public String getExpireTime() {
        return mQueryParameters.get("ExpireTime");
    }

    private Map<String, String> refreshSignParameters() {
        Map<String, String> immutableMap = new HashMap<String, String>(mQueryParameters);
        immutableMap.put("Timestamp", Util.getISO8601Time(new Date()));
        immutableMap.put("SignatureMethod", "HMAC-SHA1");
        immutableMap.put("SignatureVersion", "1.0");
        immutableMap.put("SignatureNonce", Util.getUniqueNonce());
        immutableMap.put("AccessKeyId", mAccessKeyId);
        immutableMap.put("Format", "JSON");
        immutableMap.put("Version", "2016-08-01");
        immutableMap.put("RegionId", "cn-hangzhou");
        return immutableMap;
    }

    private String composeStringToSign(Map<String, String> queries) {
        String[] sortedKeys = queries.keySet().toArray(new String[]{});
        Arrays.sort(sortedKeys);
        StringBuilder canonicalizedQueryString = new StringBuilder();
        try {
            for (String key : sortedKeys) {
                canonicalizedQueryString.append("&")
                        .append(Util.percentEncode(key)).append("=")
                        .append(Util.percentEncode(queries.get(key)));
            }

            StringBuilder stringToSign = new StringBuilder();
            stringToSign.append(METHOD);
            stringToSign.append(SEPARATOR);
            stringToSign.append(Util.percentEncode("/"));
            stringToSign.append(SEPARATOR);
            stringToSign.append(Util.percentEncode(
                    canonicalizedQueryString.toString().substring(1)));

            return stringToSign.toString();
        } catch (UnsupportedEncodingException exp) {
            throw new RuntimeException("UTF-8 encoding is not supported.");
        }

    }

    private String signRequest() {
        Map<String, String> queries = refreshSignParameters();
        String strToSign = composeStringToSign(queries);
        Log.d(TAG, strToSign);
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKey key = new SecretKeySpec((mAccessSecret+"&").getBytes(), mac.getAlgorithm());
            mac.init(key);
            byte[] bytes = mac.doFinal(strToSign.getBytes());
            String signature = Base64.encodeToString(bytes, Base64.NO_WRAP);
            queries.put("Signature", signature);
            return composeUrl(queries);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String composeUrl(Map<String, String> queries) throws UnsupportedEncodingException {
        StringBuilder urlBuilder = new StringBuilder(HOST_URL);
        urlBuilder.append("?");
        String query = concatQueryString(queries);
        return urlBuilder.append(query).toString();
    }

    private static String concatQueryString(Map<String, String> parameters) throws UnsupportedEncodingException {
        if (null == parameters) {
            return null;
        }

        StringBuilder urlBuilder = new StringBuilder("");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            urlBuilder.append(URLEncoder.encode(key, "UTF-8"));
            if (val != null) {
                urlBuilder.append("=").append(URLEncoder.encode(val, "UTF-8"));
            }
            urlBuilder.append(SEPARATOR);
        }

        int strIndex = urlBuilder.length();
        if (parameters.size() > 0) {
            urlBuilder.deleteCharAt(strIndex - 1);
        }

        return urlBuilder.toString();
    }

    public PushResponse execute() throws IOException {
        String url = signRequest();
        if (Util.isNullOrEmpty(url)) return null;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Call call = mClient.newCall(request);
        Response response = call.execute();
        return PushResponse.getInstance(response);
    }

    public void asyncExecute(final PushCallback callback) {
        String url = signRequest();
        Log.e(TAG, url);
        if (Util.isNullOrEmpty(url)) return;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Call call = mClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.onFailure(PushRequest.this, e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                callback.onResponse(PushRequest.this, PushResponse.getInstance(response));
            }
        });
    }

    public interface PushCallback {
        void onFailure(PushRequest request, IOException e);
        void onResponse(PushRequest request, PushResponse response);
    }
}
