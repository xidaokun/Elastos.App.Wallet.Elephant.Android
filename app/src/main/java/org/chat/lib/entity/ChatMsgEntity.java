package org.chat.lib.entity;

import java.util.List;

public class ChatMsgEntity {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    private String name;
    private String message;
    private int count;
    private long timeStamp;
    private String iconUrl;

    public List<String> getFriendCodes() {
        return friendCodes;
    }

    public void setFriendCodes(List<String> friendCodes) {
        this.friendCodes = friendCodes;
    }

    private List<String> friendCodes;
}
