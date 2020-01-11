package org.chat.lib.entity;

import java.util.List;

public class MessageCacheBean {
    public String MessageType;
    public String MessageHumncode;
    public long MessageTimestamp;
    public boolean MessageHasRead; //0 false, 1 true
    public String MessageContent ;
    public String MessageNickname;
    public String MessageIconPath;
    public int MessageOrientation;
    public List<String> MessageFriendCodes;
    public String MessageFriendIconPath;
}
