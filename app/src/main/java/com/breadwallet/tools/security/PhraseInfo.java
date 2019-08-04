package com.breadwallet.tools.security;

public class PhraseInfo {
    public byte[] phrase;
    public byte[] authKey;
    public byte[] pubKey;
    public long creationTime;
    public String alias;
    public boolean selected = false;
}
