package com.breadwallet.presenter.entities;

public abstract class BaseIndexPinyinBean extends BaseIndexBean {
    private String baseIndexPinyin;

    public String getBaseIndexPinyin() {
        return baseIndexPinyin;
    }

    public BaseIndexPinyinBean setBaseIndexPinyin(String baseIndexPinyin) {
        this.baseIndexPinyin = baseIndexPinyin;
        return this;
    }

    public boolean isNeedToPinyin() {
        return true;
    }

    public abstract String getTarget();
}