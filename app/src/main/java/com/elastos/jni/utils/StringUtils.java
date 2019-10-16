package com.elastos.jni.utils;

import android.net.Uri;

import com.breadwallet.tools.util.StringUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    public static boolean isNullOrEmpty(String value) {
        if (value == null || value.isEmpty()) return true;
        return false;
    }


    public static boolean isUrl(String url) {
        if (isNullOrEmpty(url)) {
            return false;
        }

        String regEx = "^(http|https)\\://.+\\.capsule$";
        Pattern p = Pattern.compile(regEx);
        Matcher matcher = p.matcher(url);
        return matcher.matches();
    }

    public static boolean isElsProtocol(String url){
        if (isNullOrEmpty(url)) return false;
        String regEx = "^(elsphant)\\://.+\\.capsule$";
        Pattern p = Pattern.compile(regEx);
        Matcher matcher = p.matcher(url.toLowerCase());
        return matcher.matches();
    }

    public static String replaceElsProtocol(String url, String protocol){
        if(StringUtil.isNullOrEmpty(url) || StringUtils.isNullOrEmpty(protocol)) return null;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        return url.replace(scheme, protocol);
    }
}
