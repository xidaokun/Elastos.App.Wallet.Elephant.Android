package org.chat.lib.source;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.util.BRConstants;
import com.google.gson.Gson;

import org.chat.lib.entity.MessageCacheBean;
import org.chat.lib.entity.MessageItemBean;
import org.chat.lib.utils.Constants;

import java.util.ArrayList;
import java.util.List;

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
            cursor = database.query(BRSQLiteHelper.CHAT_MESSAGE_ITEM_TABLE_NAME, itemColumns, null, null, null, null, "chatMessageItemTimestamp asc");
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

    public void cacheMessgeInfo() {
        MessageCacheBean messageCacheBean = new MessageCacheBean();
        messageCacheBean.MessageType = mType;
        messageCacheBean.MessageContentType = mContentType;
        messageCacheBean.MessageContent = mContent;
        messageCacheBean.MessageHasRead = mIsRead;
        messageCacheBean.MessageTimestamp = mTimestamp;
        messageCacheBean.MessageOrientation = mOrientation;
        messageCacheBean.MessageFriendCode = mFriendCode;

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


    private final String[] allColumns = {
            BRSQLiteHelper.CHAT_MESSAGE_TYPE,
            BRSQLiteHelper.CHAT_MESSAGE_HUMANCODE,
            BRSQLiteHelper.CHAT_MESSAGE_TIMESTAMP,
            BRSQLiteHelper.CHAT_MESSAGE_HAS_READ,
            BRSQLiteHelper.CHAT_MESSAGE_CONTENT,
            BRSQLiteHelper.CHAT_MESSAGE_NICKNAME,
            BRSQLiteHelper.CHAT_MESSAGE_ICON_PATH,
            BRSQLiteHelper.CHAT_MESSAGE_ORIENTATION,
            BRSQLiteHelper.CHAT_MESSAGE_FRIENDCODE,
            BRSQLiteHelper.CHAT_MESSAGE_FRIEND_ICON_PATH
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

        return messageCacheBean;
    }

    public List<MessageCacheBean> getMessage(String selection, String[] selectionArgs) {
        List<MessageCacheBean> messageInfos = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.CHAT_MESSAGE_TABLE_NAME, allColumns, selection, selectionArgs, null, null, "chatMessageTimestamp asc");
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
