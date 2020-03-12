package org.chat.lib.source;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.google.gson.Gson;

import org.chat.lib.entity.MessageCacheBean;
import org.chat.lib.entity.MessageItemBean;
import org.chat.lib.entity.NewFriendBean;
import org.chat.lib.utils.Constants;
import org.elastos.sdk.elephantwallet.contact.Contact;
import org.elastos.sdk.elephantwallet.contact.internal.ContactInterface;
import org.node.CarrierPeerNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatDataSource implements BRDataSourceInterface {

    private static final String TAG = ChatDataSource.class.getSimpleName();

    public static final String TYPE_MESSAGE_TEXT = "text";
    public static final String TYPE_MESSAGE_IMG = "img";
    public static final String TYPE_MESSAGE_VIDEO = "video";
    public static final String TYPE_MESSAGE_VOICE = "voice";

    private static ChatDataSource mInstance;

    private final BRSQLiteHelper dbHelper;

    private SQLiteDatabase database;

    private Context mContext;

    private ChatDataSource(Context context) {
        mContext = context;
        dbHelper = BRSQLiteHelper.getInstance(context);
    }

    public static ChatDataSource getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new ChatDataSource(context);
        }

        return mInstance;
    }

    private final String[] waitAcceptColumns = {
            BRSQLiteHelper.WAIT_ACCEPT_DID,
            BRSQLiteHelper.WAIT_ACCEPT_NICKNAME,
            BRSQLiteHelper.WAIT_ACCEPT_TIMESTAMP,
            BRSQLiteHelper.WAIT_ACCEPT_STATUS,
            BRSQLiteHelper.WAIT_ACCEPT_CAEEIERADDR
    };

    private NewFriendBean cursorToWaitAcceptBean(Cursor cursor) {
        NewFriendBean waitAcceptBean = new NewFriendBean();
        waitAcceptBean.did = cursor.getString(0);
        waitAcceptBean.nickName = cursor.getString(1);
        waitAcceptBean.timeStamp = cursor.getLong(2);
        waitAcceptBean.acceptStatus = cursor.getInt(3);
        waitAcceptBean.carrierAddr = cursor.getString(4);

        return waitAcceptBean;
    }

    public List<NewFriendBean> getAllNewFriends() {
        List<NewFriendBean> waitAcceptBeans = new ArrayList<>();

        Cursor cursor = null;
        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.WAIT_ACCEPT_TABLE_NAME, waitAcceptColumns, null, null, null, null, "waitAcceptTimestamp asc");
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                NewFriendBean waitAcceptBean = cursorToWaitAcceptBean(cursor);
                waitAcceptBeans.add(waitAcceptBean);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return waitAcceptBeans;
    }

    public Map<String,String> getAllFriendName() {
        Map<String, String> hashMap = new HashMap();


        Cursor cursor = null;
        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.WAIT_ACCEPT_TABLE_NAME, waitAcceptColumns, null, null, null, null, "waitAcceptTimestamp asc");
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                hashMap.put(cursor.getString(0), cursor.getString(1));
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return hashMap;
    }

    public List<NewFriendBean> getNotAcceptFriends() {
        List<NewFriendBean> waitAcceptBeans = new ArrayList<>();
        Cursor cursor = null;

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.WAIT_ACCEPT_TABLE_NAME, waitAcceptColumns, BRSQLiteHelper.WAIT_ACCEPT_STATUS+" = ? OR " + BRSQLiteHelper.WAIT_ACCEPT_STATUS+" = ? ", new String[]{String.valueOf(BRConstants.RECEIVE_ACCEPT), String.valueOf(BRConstants.REQUEST_ACCEPT)}, null, null, "waitAcceptTimestamp asc");
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                NewFriendBean waitAcceptBean = cursorToWaitAcceptBean(cursor);
                waitAcceptBeans.add(waitAcceptBean);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return waitAcceptBeans;
    }

    public NewFriendBean getFriendByCode(String friendCode) {
        NewFriendBean newFriendBean = null;
        Cursor cursor = null;

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.WAIT_ACCEPT_TABLE_NAME, waitAcceptColumns,BRSQLiteHelper.WAIT_ACCEPT_DID +" = ? OR " + BRSQLiteHelper.WAIT_ACCEPT_CAEEIERADDR+" = ? ", new String[]{friendCode, friendCode}, null, null, "waitAcceptTimestamp asc");
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                newFriendBean = cursorToWaitAcceptBean(cursor);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return newFriendBean;
    }

    public void cacheWaitAcceptFriend(NewFriendBean waitAcceptBean) {
        try {
            database = openDatabase();
            database.beginTransaction();

            ContentValues value = new ContentValues();
            value.put(BRSQLiteHelper.WAIT_ACCEPT_DID, waitAcceptBean.did);
            value.put(BRSQLiteHelper.WAIT_ACCEPT_NICKNAME, waitAcceptBean.nickName);
            value.put(BRSQLiteHelper.WAIT_ACCEPT_TIMESTAMP, waitAcceptBean.timeStamp);
            value.put(BRSQLiteHelper.WAIT_ACCEPT_STATUS, waitAcceptBean.acceptStatus);
            value.put(BRSQLiteHelper.WAIT_ACCEPT_CAEEIERADDR, waitAcceptBean.carrierAddr);

            long l = database.insertWithOnConflict(BRSQLiteHelper.WAIT_ACCEPT_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
            database.setTransactionSuccessful();
        } catch (Exception e) {
            closeDatabase();
            e.printStackTrace();
        } finally {
            database.endTransaction();
            closeDatabase();
        }
    }


    public int updateAcceptState(String friendCode, int acceptStatus) {
        int r = 0;
        try {
            database = openDatabase();

            ContentValues args = new ContentValues();
            args.put(BRSQLiteHelper.WAIT_ACCEPT_STATUS, acceptStatus);

            r = database.update(BRSQLiteHelper.WAIT_ACCEPT_TABLE_NAME, args, BRSQLiteHelper.WAIT_ACCEPT_DID +" = ? OR " + BRSQLiteHelper.WAIT_ACCEPT_CAEEIERADDR+" = ? ", new String[]{friendCode, friendCode});
            Log.d("xidaokun", "ChatDataSource#updateAcceptState#ret:"+ r);
        } finally {
            closeDatabase();
        }

        return r;
    }

    public boolean isFriendExit(String friendCode) {
        Cursor cursor = null;

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.WAIT_ACCEPT_TABLE_NAME, waitAcceptColumns, BRSQLiteHelper.WAIT_ACCEPT_DID + " = ? ", new String[]{friendCode}, null, null, null);
            return cursor.getCount()>0;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return false;
    }

    public void updateFriendName(String friendCode, String nickname) {
        try {
            database = openDatabase();

            ContentValues args = new ContentValues();
            args.put(BRSQLiteHelper.WAIT_ACCEPT_NICKNAME, nickname);

            int r = database.update(BRSQLiteHelper.WAIT_ACCEPT_TABLE_NAME, args, BRSQLiteHelper.WAIT_ACCEPT_DID + " = ? ", new String[]{friendCode});
            Log.d("xidaokun", "ChatDataSource#updateMessageItem#ret:"+ r);
        } finally {
            closeDatabase();
        }
    }

    private final String[] itemColumns = {
            BRSQLiteHelper.CHAT_MESSAGE_ITEM_FRIENDCODE,
            BRSQLiteHelper.HCAT_MESSAGE_ITEM_TIMESTAMP
    };

    private MessageItemBean cursorToItemBean(Cursor cursor) {
        MessageItemBean messageItemBean = new MessageItemBean();
        messageItemBean.friendCode = cursor.getString(0);
        messageItemBean.timeStamp = cursor.getLong(1);

        return messageItemBean;
    }

    public List<MessageItemBean> getMessageItemInfos() {
        List<MessageItemBean> messageItemBeans = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.CHAT_MESSAGE_ITEM_TABLE_NAME, itemColumns, null, null, null, null, "chatMessageItemTimestamp desc");
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                MessageItemBean messageInfo = cursorToItemBean(cursor);
                messageItemBeans.add(messageInfo);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return messageItemBeans;
    }

    public void cacheMessageItemInfos(List<MessageItemBean> messageItemBeans) {
        if (messageItemBeans == null) return;
        try {
            database = openDatabase();
            database.beginTransaction();

            for (MessageItemBean entity : messageItemBeans) {

                ContentValues value = new ContentValues();
                value.put(BRSQLiteHelper.CHAT_MESSAGE_ITEM_FRIENDCODE, entity.friendCode);
                value.put(BRSQLiteHelper.CHAT_MESSAGE_ITEM_TYPE, entity.type);
                value.put(BRSQLiteHelper.HCAT_MESSAGE_ITEM_TIMESTAMP, entity.timeStamp);

                long l = database.insertWithOnConflict(BRSQLiteHelper.CHAT_MESSAGE_ITEM_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
                Log.d(TAG, "l:" + l);
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            closeDatabase();
            e.printStackTrace();
        } finally {
            database.endTransaction();
            closeDatabase();
        }
    }

    public void deleteMessageItemInfo(String friendCode) {
        try {
            database = openDatabase();
            database.delete(BRSQLiteHelper.CHAT_MESSAGE_ITEM_TABLE_NAME, BRSQLiteHelper.CHAT_MESSAGE_FRIENDCODE + " = ?", new String[]{friendCode});
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            closeDatabase();
        }
    }

    public void updateMessageItem(String friendCode, String key, String value) {
        try {
            database = openDatabase();

            ContentValues args = new ContentValues();
            args.put(key, value);

            int r = database.update(BRSQLiteHelper.CHAT_MESSAGE_ITEM_TABLE_NAME, args, BRSQLiteHelper.CHAT_MESSAGE_ITEM_FRIENDCODE + " = ? ", new String[]{friendCode});
            Log.d("xidaokun", "ChatDataSource#updateMessageItem#ret:"+ r);
        } finally {
            closeDatabase();
        }
    }

    public String mType;
    public ChatDataSource setType(String type) {
        this.mType = type;
        return this;
    }

    public String mContentType = ChatDataSource.TYPE_MESSAGE_TEXT;
    public ChatDataSource setContentType(String contentType) {
        this.mContentType = contentType;
        return this;
    }

    public String mContent;
    public ChatDataSource setContent(String content) {
        this.mContent = content;
        return this;
    }

    public boolean mIsRead;
    public ChatDataSource hasRead(boolean isRead) {
        this.mIsRead = isRead;
        return this;
    }

    public long mTimestamp;
    public ChatDataSource setTimestamp(long timestamp) {
        this.mTimestamp = timestamp;
        return this;
    }

    public int mOrientation = Constants.CHAT_ITEM_TYPE_RIGHT;
    public ChatDataSource setOrientation(int orientation) {
        this.mOrientation = orientation;
        return this;
    }

    public String mFriendCode;
    public ChatDataSource setFriendCode(String friendCode) {
        this.mFriendCode = friendCode;
        return this;
    }

    public String mNickname;
    public ChatDataSource setNickname(String nickname) {
        this.mNickname = nickname;
        return this;
    }

    public int mSendState;
    public ChatDataSource setSendState(int state) {
        this.mSendState = state;
        return this;
    }

    public void cacheMessgeInfo() {
        MessageCacheBean messageCacheBean = new MessageCacheBean();
        messageCacheBean.MessageType = mType;
        messageCacheBean.MessageContentType = mContentType;
        messageCacheBean.MessageContent = mContent;
        messageCacheBean.MessageNickname = mNickname;
        messageCacheBean.MessageHasRead = mIsRead;
        messageCacheBean.MessageTimestamp = mTimestamp;
        messageCacheBean.MessageOrientation = mOrientation;
        messageCacheBean.MessageFriendCode = mFriendCode;
        messageCacheBean.MessageSendState = mSendState;

        List<MessageCacheBean> messageCacheBeans = new ArrayList<>();
        messageCacheBeans.add(messageCacheBean);
        Log.d("xidaokun", "ChatDataSource#cacheMessge#\ncacheMessage:"+ new Gson().toJson(messageCacheBeans));
        cacheMessage(messageCacheBeans);

        MessageItemBean messageItemBean = new MessageItemBean();
        messageItemBean.friendCode = mFriendCode;
        messageItemBean.type = mType;
        messageItemBean.contentType = mContentType;
        messageItemBean.timeStamp = mTimestamp;
        List<MessageItemBean> messageItemBeans = new ArrayList<>();
        messageItemBeans.add(messageItemBean);
        Log.d("xidaokun", "ChatDataSource#cacheMessge#\ncacheMessageItemInfos:"+ new Gson().toJson(messageItemBeans));
        cacheMessageItemInfos(messageItemBeans);
    }


    private final String[] messageColumns = {
            BRSQLiteHelper.CHAT_MESSAGE_TYPE,
            BRSQLiteHelper.CHAT_MESSAGE_HUMANCODE,
            BRSQLiteHelper.CHAT_MESSAGE_TIMESTAMP,
            BRSQLiteHelper.CHAT_MESSAGE_HAS_READ,
            BRSQLiteHelper.CHAT_MESSAGE_CONTENT,
            BRSQLiteHelper.CHAT_MESSAGE_NICKNAME,
            BRSQLiteHelper.CHAT_MESSAGE_ICON_PATH,
            BRSQLiteHelper.CHAT_MESSAGE_ORIENTATION,
            BRSQLiteHelper.CHAT_MESSAGE_FRIENDCODE,
            BRSQLiteHelper.CHAT_MESSAGE_FRIEND_ICON_PATH,
            BRSQLiteHelper.CHAT_MESSAGE_SEND_STATE
    };

    private MessageCacheBean cursorToMessageInfo(Cursor cursor) {
        MessageCacheBean messageCacheBean = new MessageCacheBean();
        messageCacheBean.MessageType = cursor.getString(0);
        messageCacheBean.MessageHumncode = cursor.getString(1);
        messageCacheBean.MessageTimestamp = cursor.getLong(2);
        messageCacheBean.MessageHasRead = cursor.getInt(3) == 1;
        messageCacheBean.MessageContent = cursor.getString(4);
        messageCacheBean.MessageNickname = cursor.getString(5);
        messageCacheBean.MessageIconPath = cursor.getString(6);
        messageCacheBean.MessageOrientation = cursor.getInt(7);
        messageCacheBean.MessageFriendCode = cursor.getString(8);
        messageCacheBean.MessageFriendIconPath = cursor.getString(9);
        messageCacheBean.MessageSendState = cursor.getInt(10);

        return messageCacheBean;
    }

    public String getNickname(String friendCode) {
        Map<String, String> friendsNickname = ChatDataSource.getInstance(mContext).getAllFriendName();
        ContactInterface.FriendInfo friendInfo = CarrierPeerNode.getInstance(mContext).getFriendInfo(friendCode);
        if(null == friendInfo) return null;
        String nickname = friendsNickname.get(friendInfo.humanCode);
        if(!StringUtil.isNullOrEmpty(nickname)) return nickname;
        for(Contact.HumanInfo.CarrierInfo carrierInfo : friendInfo.boundCarrierArray) {
            nickname = friendsNickname.get(carrierInfo.usrAddr);
            if(!StringUtil.isNullOrEmpty(nickname)) return nickname;
        }

        return null;
    }

    public List<MessageCacheBean> getMessage(String selection, String[] selectionArgs) {
        List<MessageCacheBean> messageInfos = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.CHAT_MESSAGE_TABLE_NAME, messageColumns, selection, selectionArgs, null, null, "chatMessageTimestamp asc");
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                MessageCacheBean messageInfo = cursorToMessageInfo(cursor);
                messageInfos.add(messageInfo);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return messageInfos;
    }

    public List<MessageCacheBean> getFailedMessage() {
        List<MessageCacheBean> messageCacheBeans = new ArrayList<>();
        Cursor cursor = null;

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.CHAT_MESSAGE_TABLE_NAME, messageColumns, BRSQLiteHelper.CHAT_MESSAGE_SEND_STATE+" = ? ", new String[]{String.valueOf(Constants.CHAT_ITEM_SENDING)}, null, null, "chatMessageTimestamp asc");
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                MessageCacheBean messageCacheBean = cursorToMessageInfo(cursor);
                messageCacheBeans.add(messageCacheBean);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return messageCacheBeans;
    }

    public void deleteMessage(String friendCode) {
        try {
            database = openDatabase();
            database.delete(BRSQLiteHelper.CHAT_MESSAGE_TABLE_NAME, BRSQLiteHelper.CHAT_MESSAGE_FRIENDCODE + " = ?", new String[]{friendCode});
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            closeDatabase();
        }
    }

    public void updateMessage(List<MessageCacheBean> messageCacheBeans, boolean hasRead) {
        try {
            database = openDatabase();

            for (MessageCacheBean entity : messageCacheBeans) {
                ContentValues args = new ContentValues();
                args.put(BRSQLiteHelper.CHAT_MESSAGE_HAS_READ, hasRead ? 1 : 0);

                int r = database.update(BRSQLiteHelper.CHAT_MESSAGE_TABLE_NAME, args, BRSQLiteHelper.CHAT_MESSAGE_FRIENDCODE + " = ? ", new String[]{entity.MessageFriendCode});

            }
        } finally {
            closeDatabase();
        }
    }

    public void updateHasRead(String friendCode, boolean hasRead) {
        try {
            database = openDatabase();

            ContentValues args = new ContentValues();
            args.put(BRSQLiteHelper.CHAT_MESSAGE_HAS_READ, hasRead ? 1 : 0);
            int r = database.update(BRSQLiteHelper.CHAT_MESSAGE_TABLE_NAME, args, BRSQLiteHelper.CHAT_MESSAGE_FRIENDCODE + " = ? ", new String[]{friendCode});
        } finally {
            closeDatabase();
        }
    }

    public void updateMessage(String friendCode, String key, String value) {
        try {
            database = openDatabase();

            ContentValues args = new ContentValues();
            args.put(key, value);

            int r = database.update(BRSQLiteHelper.CHAT_MESSAGE_TABLE_NAME, args, BRSQLiteHelper.CHAT_MESSAGE_FRIENDCODE + " = ? ", new String[]{friendCode});
            Log.d("xidaokun", "ChatDataSource#updateMessage#ret:"+ r);
        } finally {
            closeDatabase();
        }
    }

    public void cacheMessage(List<MessageCacheBean> messageCacheBeans) {
        if (messageCacheBeans == null) return;
//        Cursor cursor = null;
        try {
            database = openDatabase();
            database.beginTransaction();

            for (MessageCacheBean entity : messageCacheBeans) {

                ContentValues value = new ContentValues();
                value.put(BRSQLiteHelper.CHAT_MESSAGE_HUMANCODE, entity.MessageHumncode);
                value.put(BRSQLiteHelper.CHAT_MESSAGE_NICKNAME, entity.MessageNickname);
                value.put(BRSQLiteHelper.CHAT_MESSAGE_CONTENT, entity.MessageContent);
                value.put(BRSQLiteHelper.CHAT_MESSAGE_FRIEND_ICON_PATH, entity.MessageFriendIconPath);
                value.put(BRSQLiteHelper.CHAT_MESSAGE_FRIENDCODE, entity.MessageFriendCode);
                value.put(BRSQLiteHelper.CHAT_MESSAGE_HAS_READ, entity.MessageHasRead ? 1 : 0);
                value.put(BRSQLiteHelper.CHAT_MESSAGE_ICON_PATH, entity.MessageIconPath);
                value.put(BRSQLiteHelper.CHAT_MESSAGE_ORIENTATION, entity.MessageOrientation);
                value.put(BRSQLiteHelper.CHAT_MESSAGE_TIMESTAMP, entity.MessageTimestamp);
                value.put(BRSQLiteHelper.CHAT_MESSAGE_TYPE, entity.MessageType);
                value.put(BRSQLiteHelper.CHAT_MESSAGE_SEND_STATE, entity.MessageSendState);

                long l = database.insertWithOnConflict(BRSQLiteHelper.CHAT_MESSAGE_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
                Log.d(TAG, "l:" + l);
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            closeDatabase();
            e.printStackTrace();
        } finally {
//            cursor.close();
            database.endTransaction();
            closeDatabase();
        }

    }

    @Override
    public SQLiteDatabase openDatabase() {
        if (database == null || !database.isOpen())
            database = dbHelper.getWritableDatabase();
        dbHelper.setWriteAheadLoggingEnabled(BRConstants.WRITE_AHEAD_LOGGING);
        return database;
    }

    @Override
    public void closeDatabase() {

    }
}
