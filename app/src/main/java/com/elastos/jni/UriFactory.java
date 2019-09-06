package com.elastos.jni;

import android.net.Uri;

import com.elastos.jni.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

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
        if(StringUtils.isNullOrEmpty(candidate.trim())) return null;
        return candidate;
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

    private String RequestType;
    public UriFactory setRequestType(String type){
        this.RequestType = type;
        return this;
    }

    private String AppID;
    public UriFactory setAppID(String appID){
        this.AppID = appID;
        return this;
    }

    private String SerialNumber;
    public UriFactory setSerialNumber(String serialNumber){
        this.SerialNumber = serialNumber;
        return this;
    }

    private String AppName;
    public UriFactory setAppName(String appName){
        this.AppName = appName;
        return this;
    }

    private String DID;
    public UriFactory setDID(String DID){
        this.DID = DID;
        return this;
    }

    private String PublicKey;
    public UriFactory setPublicKey(String publicKey){
        this.PublicKey = publicKey;
        return this;
    }

    private String Signature;
    public UriFactory setSignature(String signature){
        this.Signature = signature;
        return this;
    }

    private String Description;
    public UriFactory setDescription(String description){
        this.Description = description;
        return this;
    }

    private String RandomNumber;
    public UriFactory setRandomNumber(String RandomNumber){
        this.RandomNumber = RandomNumber;
        return this;
    }

    private String CallbackUrl;
    public UriFactory setCallbackUrl(String callbackUrl){
        this.CallbackUrl = callbackUrl;
        return this;
    }

    private String OrderID;
    public UriFactory setOrderID(String orderID){
        this.OrderID = orderID;
        return this;
    }

    private String ReceivingAddress;
    public UriFactory setReceivingAddress(String receivingAddress){
        this.ReceivingAddress = receivingAddress;
        return this;
    }

    private String Target;
    public UriFactory setTarget(String target){
        this.Target = target;
        return this;
    }

    public String buildLoginUri(){
        result.clear();
//        result.put(RequestType.getClass().getSimpleName(), RequestType);
        result.put("AppID".toLowerCase(), AppID);
        result.put("SerialNumber".toLowerCase(), SerialNumber);
        result.put("AppName".toLowerCase(), AppName);
        result.put("DID".toLowerCase(), DID);
        result.put("PublicKey".toLowerCase(), PublicKey);
        result.put("Signature".toLowerCase(), Signature);
        result.put("Description".toLowerCase(), Description);
        result.put("RandomNumber".toLowerCase(), RandomNumber);
        result.put("CallbackUrl".toLowerCase(), CallbackUrl);

        return create(RequestType, result);
    }

    private String Amount;
    public UriFactory setAmount(String amount){
        this.Amount = amount;
        return this;
    }

    private String CoinName;
    public UriFactory setCoinName(String CoinName){
        this.CoinName = CoinName;
        return this;
    }

    public String buildPayUri(){
        result.clear();
        result.put("AppID".toLowerCase(), AppID);
        result.put("SerialNumber".toLowerCase(), SerialNumber);
        result.put("AppName".toLowerCase(), AppName);
        result.put("DID".toLowerCase(), DID);
        result.put("PublicKey".toLowerCase(), PublicKey);
        result.put("Signature".toLowerCase(), Signature);
        result.put("Description".toLowerCase(), Description);
        result.put("CallbackUrl".toLowerCase(), CallbackUrl);
        result.put("ReceivingAddress".toLowerCase(), ReceivingAddress);
        result.put("Amount".toLowerCase(), Amount);
        result.put("CoinName".toLowerCase(), CoinName);

        return create(RequestType, result);
    }

    private Map<String, String> result = new HashMap();
//    public void parse(String uri){
//        result.clear();
//        String[] schemeArr = null;
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
//    }

    public void parse(String uri){
        result.clear();
        String[] schemeArr = null;
        if(uri.contains("elastos://")){
            schemeArr = uri.split("elastos://");
            result.put(SCHEME_KEY, "elastos");
        } else if(uri.contains("elaphant")){
            schemeArr = uri.split("elaphant://");
            result.put(SCHEME_KEY, "elaphant");
        }
        if(schemeArr!=null && schemeArr.length>1) {
            String[] typeArr = schemeArr[1].split("\\?");
            if(typeArr!=null && typeArr.length>1){
                result.put(TYPE_KEY, typeArr[0]);

                String[] andArr = typeArr[1].split("&");
                if(andArr==null || andArr.length<=0) return;
                for(String and : andArr){
                    String[] params = and.split("=");
                    if(params!=null && params.length>1) {
                        result.put(params[0].toLowerCase(), params[1]);
                    }
                }

            }
        }
    }

    public void parseUrl(String uri){
        
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
