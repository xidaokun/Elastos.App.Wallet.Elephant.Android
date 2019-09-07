package com.elastos.jni;

import android.content.Context;

import org.elastos.sdk.keypair.ElastosKeypair;
import org.elastos.sdk.keypair.ElastosKeypairDID;

public class Utility {
    private static Utility mInstance;
    private static Context mContext;

    private Utility(Context context){
        this.mContext = context;
    }

    public static Utility getInstance(Context context){
        if(mInstance == null){
            mInstance = new Utility(context);
        }
        return mInstance;
    }

    public String getSinglePrivateKey(String mnemonic){
        ElastosKeypair.Data seed = new ElastosKeypair.Data();
        int ret = ElastosKeypair.getSeedFromMnemonic(seed, mnemonic, "");
        if(ret <= 0) return null;

        return ElastosKeypair.getSinglePrivateKey(seed, ret);

    }

    public String getSinglePublicKey(String mnemonic){

        ElastosKeypair.Data seed = new ElastosKeypair.Data();
        int length = ElastosKeypair.getSeedFromMnemonic(seed, mnemonic, "");
        if(length <= 0) return null;

        return ElastosKeypair.getSinglePublicKey(seed, length);
    }

    public String getAddress(String jpublickey){
        return ElastosKeypair.getAddress(jpublickey);
    }

    public byte[] sign(String privateKey, byte[] data){
        ElastosKeypair.Data signData = new ElastosKeypair.Data();
        signData.buf = data;
        ElastosKeypair.Data signedData = new ElastosKeypair.Data();
        int length = ElastosKeypair.sign(privateKey, signData, signData.buf.length, signedData);

        if(length <= 0) return null;
        return signedData.buf;
    }

    public boolean verify(String publicKey, byte[] data, byte[] signed){
        ElastosKeypair.Data signData = new ElastosKeypair.Data();
        ElastosKeypair.Data signedData = new ElastosKeypair.Data();
        signData.buf = data;
        signedData.buf = signed;
        return ElastosKeypair.verify(publicKey, signData, signData.buf.length, signedData, signedData.buf.length);
    }

    public String getDid(String publicKey){
        return ElastosKeypairDID.getDid(publicKey);
    }
}
