package com.elastos.jni;

import android.net.Uri;

import com.elastos.jni.utils.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class UriFactory {



    public static final String SCHEME_KEY = "scheme_key";
    public static final String TYPE_KEY = "type_key";

    public String getRequestType() {
        return result.get(TYPE_KEY);
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

    public String getReturnUrl(){
        return getValue("ReturnUrl".toLowerCase());
    }

    public String getRequestInfo() {return getValue("RequestInfo".toLowerCase());}

    public String getCandidatePublicKeys() {
        String candidate = getValue("CandidatePublicKeys".toLowerCase());
        if(StringUtils.isNullOrEmpty(candidate)) return null;
        return candidate.trim();
    }

    public String getOrderID() {return getValue("OrderID".toLowerCase());}

    public String getReceivingAddress() {return getValue("ReceivingAddress".toLowerCase());}

    public String getTarget() {return getValue("Target".toLowerCase());}

    public String getRequestedContent(){
        return getValue("RequestedContent".toLowerCase());
    }

    public String getUseStatement(){
        return getValue("UseStatement".toLowerCase());
    }

    private String getValue(String key){
        String tmp = Uri.decode(result.get(key));
        return tmp;
    }

    private Map<String, String> result = new HashMap();

    public void parse(String url){
        result.clear();

        Uri uri = Uri.parse(url);
        Set names = uri.getQueryParameterNames();
        Iterator<String> it = names.iterator();

        String scheme = uri.getScheme();
        result.put(SCHEME_KEY, scheme);

        while (it.hasNext()) {
            String key = it.next();
            String value = uri.getQueryParameter(key);
            result.put(key.toLowerCase(), value);
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


    public String create(String type, Map<String, String> params){
        if(StringUtils.isNullOrEmpty(type) || params.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("elaphant://").append(type).append("?");

        for(Map.Entry<String, String> entry : params.entrySet()){
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }

        return sb.deleteCharAt(sb.length()-1).toString();
    }

}

