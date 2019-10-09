package com.elastos.jni.utils;

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
}
