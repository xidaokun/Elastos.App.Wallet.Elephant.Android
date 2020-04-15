package com.elastos.jni.utils;

import android.net.Uri;

import com.breadwallet.tools.util.StringUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SchemeStringUtils {

    public static boolean isNullOrEmpty(String value) {
        if (value == null || value.isEmpty()) return true;
        return false;
    }


    public static boolean isHttpPrefix(String url) {
        if (isNullOrEmpty(url)) {
            return false;
        }
//        String regEx = "^(http|https)\\://.+\\.capsule$";
        String regEx = "^(http|https)\\://.+";
        Pattern p = Pattern.compile(regEx);
        Matcher matcher = p.matcher(url.trim());
        boolean ret = matcher.matches();
        return ret;
    }

    public static boolean isElaphantPrefix(String url){
        if (isNullOrEmpty(url)) return false;
//        String regEx = "^(elaphant|elastos|elapp:http|elapp:https)\\://.+\\.capsule$";
        String regEx = "^(elaphant|elastos|elapp:http|elapp:https)\\://.+";
        Pattern p = Pattern.compile(regEx);
        Matcher matcher = p.matcher(url);
        boolean ret = matcher.matches();
        return ret;
    }

    public static String replaceElsProtocol(String url, String protocol){
        if(StringUtil.isNullOrEmpty(url) || SchemeStringUtils.isNullOrEmpty(protocol)) return null;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        return url.replace(scheme+"://", protocol+"://");
    }



    public static List<String> asList(String value) {
        try {
            if(isNullOrEmpty(value)) return null;
            return new Gson().fromJson(value, new TypeToken<List<String>>() {
            }.getType());
        } catch (Exception e) {

        }

        return null;
    }
}
