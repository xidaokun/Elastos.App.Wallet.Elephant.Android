package org.chat.lib.push;

import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Response;

public class PushResponse {
    private static final String TAG = PushResponse.class.getName();

    private String RequestId;

    private String MessageId;

    public String getRequestId() {
        return this.RequestId;
    }

    public void setRequestId(String requestId) {
        this.RequestId = requestId;
    }

    public String getMessageId() {
        return this.MessageId;
    }

    public void setMessageId(String messageId) {
        this.MessageId = messageId;
    }

    public static PushResponse getInstance(Response response) {
        try {
            String body = response.body().string();
            Log.d(TAG, body);
            Gson json = new Gson();
            return json.fromJson(body, PushResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
