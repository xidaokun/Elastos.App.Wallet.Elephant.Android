package com.breadwallet.presenter.entities;

import java.io.Serializable;

/**
 * Created by byfieldj on 1/31/18.
 */

public class BaseWallet implements Serializable {

    private String mWalletName;
    private String mWalletBalanceUSD;
    private String mWalletBalanceCurrency;
    private String mWalletCurrency;
    private String mTradeValue;
    private String mWalletType;

    public static class WalletType{

      public static final String BTC_WALLET = "btc";
      public static final String BCH_WALLET = "bch";
    }

    public String getWalletBalanceCurrency() {
        return mWalletBalanceCurrency;
    }

    public void setWalletBalanceCurrency(String walletBalanceCurrency) {
        this.mWalletBalanceCurrency = walletBalanceCurrency;
    }

    public String getWalletCurrency() {
        return mWalletCurrency;
    }

    public void setWalletCurrency(String walletCurrency) {
        this.mWalletCurrency = walletCurrency;
    }

    public String getTradeValue() {
        return mTradeValue;
    }

    public void setTradeValue(String tradeValue) {
        this.mTradeValue = tradeValue;
    }


    public void setWalletName(String name) {

        this.mWalletName = name;
    }

    public String getWalletName() {
        return this.mWalletName;
    }


    public void setWalletBalanceUSD(String balance) {
        this.mWalletBalanceUSD = balance;
    }


    public String getWalletBalanceUSD() {
        return this.mWalletBalanceUSD;
    }

    public void setWalletType(String type){
        this.mWalletType = type;
    }

    public String getWalletType(){
        return this.mWalletType;
    }


}