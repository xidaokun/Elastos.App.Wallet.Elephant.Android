package org.chat.lib.entity;

public class MessageCacheBean {
    public String MessageType; //single, group
    public String MessageHumncode;
    public long MessageTimestamp;
    public boolean MessageHasRead; //0 false, 1 true
    public String MessageContentType; //text, img, voice
    public String MessageContent;
    public String MessageNickname;
    public String MessageIconPath;
    public int MessageOrientation;
    public String MessageFriendCode;
    public String MessageFriendIconPath;
    public int MessageSendState;
}
