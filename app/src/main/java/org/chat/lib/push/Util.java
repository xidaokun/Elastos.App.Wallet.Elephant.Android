package org.chat.lib.push;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.UUID;

public class Util {
    public static String percentEncode(String data) throws UnsupportedEncodingException {
        return data != null ? URLEncoder.encode(data, "UTF-8").replace("+", "%20")
                .replace("*", "%2A").replace("%7E", "~") : null;
}

    public static String getISO8601Time(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return df.format(date);
    }

    public static String getUniqueNonce() {
        StringBuffer uniqueNonce = new StringBuffer();
        UUID uuid = UUID.randomUUID();
        uniqueNonce.append(uuid.toString());
        uniqueNonce.append(System.currentTimeMillis());
        uniqueNonce.append(Thread.currentThread().getId());
        return uniqueNonce.toString();
    }

    public static boolean isNullOrEmpty(String data) {
        return data == null || data.isEmpty();
    }
}
