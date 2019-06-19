package com.breadwallet.presenter.entities;

public class RegisterChainData extends BaseChainData {

    public Value value;

    public static class Value {
        public String url;
        public String hash;
    }
}
