package com.breadwallet.presenter.entities;

public class ContactEntity extends BaseIndexPinyinBean {
    private String contact;
    private boolean isTop;

    public ContactEntity() {

    }

    public ContactEntity(String contact) {
        this.contact = contact;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public boolean isTop() {
        return isTop;
    }

    @Override
    public String getTarget() {
        return contact;
    }

    public ContactEntity setTop(boolean top) {
        isTop = top;
        return this;
    }

    @Override
    public boolean isNeedToPinyin() {
        return !isTop;
    }


    @Override
    public boolean isShowSuspension() {
        return !isTop;
    }
}
