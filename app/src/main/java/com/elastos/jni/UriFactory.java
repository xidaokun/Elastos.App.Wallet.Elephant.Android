package com.elastos.jni;

import android.net.Uri;

import com.breadwallet.tools.util.StringUtil;
import com.elastos.jni.utils.SchemeStringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class UriFactory {


    public static final String SCHEME_KEY = "scheme_key";
    public static final String HOST_KEY = "host_key";

    public String getScheme() {
        return result.get(SCHEME_KEY);
    }

    public String getHost() {
        return result.get(HOST_KEY);
    }

    public String getAppID() {
        return getValue("AppID".toLowerCase());
    }

    public String getSerialNumber() {
        return getValue("SerialNumber".toLowerCase());
    }

    public String getAppName() {
        return getValue("AppName".toLowerCase());
    }

    public String getDID() {
        return getValue("DID".toLowerCase());
    }

    public String getPublicKey() {
        return getValue("PublicKey".toLowerCase());
    }

    public String getSignature() {
        return getValue("Signature".toLowerCase());
    }

    public String getDescription() {
        return getValue("Description".toLowerCase());
    }

    public String getRandomNumber() {
        return getValue("RandomNumber".toLowerCase());
    }

    public String getCallbackUrl() {
        return getValue("CallbackUrl".toLowerCase());
    }

    public String getPaymentAddress() {
        return getValue("PaymentAddress".toLowerCase());
    }

    public String getAmount() {
        return getValue("Amount".toLowerCase());
    }

    public String getCoinName() {
        return getValue("CoinName".toLowerCase());
    }

    public String getReturnUrl() {
        return getValue("ReturnUrl".toLowerCase());
    }

    public String getRequestInfo() {
        return getValue("RequestInfo".toLowerCase());
    }

    public String getCandidates() {
        return getValue("Candidates".toLowerCase());
    }

    public String getVotes() {
        return getValue("Votes".toLowerCase());
    }

    public String getCandidatePublicKeys() {
        String candidate = getValue("CandidatePublicKeys".toLowerCase());
        if (SchemeStringUtils.isNullOrEmpty(candidate)) return null;
        return candidate.trim();
    }

    public String getOrderID() {
        return getValue("OrderID".toLowerCase());
    }

    public String getReceivingAddress() {
        return getValue("ReceivingAddress".toLowerCase());
    }

    public String getTarget() {
        return getValue("Target".toLowerCase());
    }

    public String getRequestedContent() {
        return getValue("RequestedContent".toLowerCase());
    }

    public String getUseStatement() {
        return getValue("UseStatement".toLowerCase());
    }

    private String getValue(String key) {
        String tmp = Uri.decode(result.get(key));
        return tmp;
    }

    private Map<String, String> result = new HashMap();

    public UriFactory() {

    }

    public UriFactory(String url) {
        parse(url);
    }

    private String mUrl;
    public String getUrl() {
        return mUrl;
    }

    public void parse(String url) {
        mUrl = url;
        try {
            if (StringUtil.isNullOrEmpty(url)) return;
            if (url.toUpperCase().contains("ELAPHANT%3A%2F%2F") || url.toUpperCase().contains("ELASTOS%3A%2F%2F")) {
                url = Uri.decode(url);
            }

            if(url.contains("elastos://")){
                url = "elastos://" + url.split("elastos://")[1];
                result.put(SCHEME_KEY, "elastos");
            } else if(url.contains("elaphant")){
                url = "elaphant://" + url.split("elaphant://")[1];
                result.put(SCHEME_KEY, "elaphant");
            }

            result.clear();
            Uri uri = Uri.parse(url);
            Set names = uri.getQueryParameterNames();
            Iterator<String> it = names.iterator();

            String scheme = uri.getScheme();
            result.put(SCHEME_KEY, scheme);

            String host = uri.getHost();
            result.put(HOST_KEY, host);

            while (it.hasNext()) {
                String key = it.next();
                String value = uri.getQueryParameter(key);
                result.put(key.toLowerCase(), value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

//        String[] schemeArr = null;
//        if(StringUtil.isNullOrEmpty(uri)) return;
//        if(uri.toUpperCase().contains("ELAPHANT%3A%2F%2F") || uri.toUpperCase().contains("ELASTOS%3A%2F%2F")) {
//           uri = Uri.decode(uri);
//        }
//        if(uri.contains("elastos://")){
//            schemeArr = uri.split("elastos://");
//            result.put(SCHEME_KEY, "elastos");
//        } else if(uri.contains("elaphant")){
//            schemeArr = uri.split("elaphant://");
//            result.put(SCHEME_KEY, "elaphant");
//        }
//        if(schemeArr!=null && schemeArr.length>1) {
//            String[] typeArr = schemeArr[1].split("\\?");
//            if(typeArr!=null && typeArr.length>1){
//                result.put(TYPE_KEY, typeArr[0]);
//
//                String[] andArr = typeArr[1].split("&");
//                if(andArr==null || andArr.length<=0) return;
//                for(String and : andArr){
//                    String[] params = and.split("=");
//                    if(params!=null && params.length>1) {
//                        result.put(params[0].toLowerCase(), params[1]);
//                    }
//                }
//
//            }
//        }
    }

}

