package org.node;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.wallet.wallets.ela.WalletElaManager;

import org.chat.lib.entity.MessageInfo;
import org.chat.lib.push.PushClient;
import org.chat.lib.utils.Constants;
import org.elastos.sdk.elephantwallet.contact.Contact;
import org.elastos.sdk.elephantwallet.contact.Utils;
import org.elastos.sdk.elephantwallet.contact.internal.ContactInterface;
import org.elastos.sdk.keypair.ElastosKeypair;
import org.greenrobot.eventbus.EventBus;

import java.util.List;

import app.elaphant.sdk.peernode.Connector;
import app.elaphant.sdk.peernode.PeerNode;
import app.elaphant.sdk.peernode.PeerNodeListener;

public class CarrierPeerNode {

    private static CarrierPeerNode mInstance;
    private static PeerNode mPeerNode;
    private static Connector mConnector = null;
    private static Connector mGroupConnector = null;

    private String mPrivateKey;
    private String mPublicKey;

    private int mStartRet;

    private Context mContext;

    private CarrierPeerNode(Context context) {
        if (null == mInstance) {
            init(context);
        }
    }

    public static CarrierPeerNode getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new CarrierPeerNode(context);
        }
        return mInstance;
    }

    private void init(final Context context) {
        mContext = context;
        mPrivateKey = WalletElaManager.getInstance(context).getPrivateKey();
        mPublicKey = WalletElaManager.getInstance(context).getPublicKey();
        mPeerNode = PeerNode.getInstance(context.getFilesDir().getAbsolutePath(),
                Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                mPeerNode.setListener(new PeerNodeListener.Listener() {

                    @Override
                    public byte[] onAcquire(org.elastos.sdk.elephantwallet.contact.Contact.Listener.AcquireArgs request) {
                        byte[] response = null;
                        switch (request.type) {
                            case PublicKey:
                                response = (null==mPublicKey)?null:mPublicKey.getBytes();
                                Log.d("xidaokun", "CarrierPeerNode#setListener#PublicKey#\nmPublicKey:"+ mPublicKey);
                                break;
                            case EncryptData:
                                response = request.data;
                                Log.d("xidaokun", "CarrierPeerNode#setListener#EncryptData#\nrequest.data:"+ request.data);
                                break;
                            case DecryptData:
                                response = request.data;
                                Log.d("xidaokun", "CarrierPeerNode#setListener#DecryptData#\nrequest.data:"+ request.data);
                                break;
                            case DidPropAppId:
                                response = BRConstants.ELAPHANT_APP_ID.getBytes();
                                break;
                            case DidAgentAuthHeader:
                                response = getAgentAuthHeader();
                                break;
                            case SignData:
                                response = signData(request.data);
                                Log.d("xidaokun", "CarrierPeerNode#setListener#SignData#\nrequest.data:"+ "Unprocessed request");
                                break;
                            default:
                                Log.d("xidaokun", "CarrierPeerNode#setListener#default:"+ request.data);
                                throw new RuntimeException("Unprocessed request: " + request);
                        }
                        return response;
                    }

                    @Override
                    public void onError(int errCode, String errStr, String ext) {
                        Log.d("xidaokun", "CarrierPeerNode#setListener#onError#errStr:"+ errStr + "\next:" + ext);
                    }
                });

                mStartRet = mPeerNode.start();
                createConnector("chat");
                createGroupConnector("ChatGroupService");
                ContactInterface.UserInfo userInfo = getUserInfo();
                if(null != userInfo) {
                    String carrierAddr = userInfo.getCurrDevCarrierAddr();
                    if(StringUtil.isNullOrEmpty(carrierAddr)) return;
                    BRSharedPrefs.cacheCarrierId(mContext, carrierAddr);
                    Log.d("xidaokun_push", "bind carrierAddr:"+carrierAddr);
                    PushClient.getInstance().bindAlias(carrierAddr, null);
                }

            }
        });
    }

    public void start() {
        Log.d("xidaokun", "CarrierPeerNode#start#mStartRet:"+ mStartRet);
        if (mStartRet != 0) throw new RuntimeException("carrier start failed :" + mStartRet);
    }

    public void stop() {
        int ret = mPeerNode.stop();
    }

    private byte[] getAgentAuthHeader() {
        String appid = "org.elastos.debug.didplugin";
        String appkey = "b2gvzUM79yLhCbbGNWCuhSsGdqYhA7sS";
        long timestamp = System.currentTimeMillis();
        String auth = Utils.getMd5Sum(appkey + timestamp);
        String headerValue = "id=" + appid + ";time=" + timestamp + ";auth=" + auth;

        return headerValue.getBytes();
    }

    private byte[] signData(byte[] data) {
        ElastosKeypair.Data originData = new ElastosKeypair.Data();
        originData.buf = data;

        ElastosKeypair.Data signedData = new ElastosKeypair.Data();

        if (StringUtil.isNullOrEmpty(mPrivateKey)) return null;
        int signedSize = ElastosKeypair.sign(mPrivateKey, originData, originData.buf.length, signedData);
        if (signedSize <= 0) {
            return null;
        }

        return signedData.buf;
    }

    private void createConnector(String serviceName) {
        if (mConnector != null) return;
        mConnector = new Connector(serviceName);
        mConnector.setMessageListener(new PeerNodeListener.MessageListener() {
            @Override
            public void onEvent(Contact.Listener.EventArgs event) {
                handleEvent(event);
            }

            @Override
            public void onReceivedMessage(String humanCode, Contact.Channel channelType, Contact.Message message) {
                handleMessage(humanCode, message);
            }
        });
    }

    private void createGroupConnector(String serviceName) {
        if(mGroupConnector != null) return;
        mGroupConnector = new Connector(serviceName);
        mGroupConnector.setMessageListener(new PeerNodeListener.MessageListener() {
            @Override
            public void onEvent(Contact.Listener.EventArgs eventArgs) {
                handleEvent(eventArgs);
//                String humanCode = eventArgs.humanCode;
//                Log.d("xidaokun", "CarrierPeerNode#createGroupConnector#onEvent\n#eventArgs.type:"+ eventArgs.type+"\n#humanCode:"+humanCode);
            }

            @Override
            public void onReceivedMessage(String s, ContactInterface.Channel channel, ContactInterface.Message message) {
                Log.d("xidaokun", "CarrierPeerNode#handleMessage\n#humanCode:"+ s  + "\n#channel"+channel
                        + "\n#message:" + message.data.toString()
                + "\n#nanoTime:" + message.nanoTime);

//                BRDateUtil.stringToLong(message.nanoTime, "yyyy-MM-dd hh:mm:ss");

                //      #humanCode:immMWGMeXsWtvKcTqgYkGEnRbafEvUdX6u
                //      #channelCarrier(1)
                //      #message:{"content":"aaaaa","nickName":"匿名6","serviceName":"ChatGroupService","timeStamp":"2020-1-16 16:27:57","type":"textMsg"}
                //      #nanoTime:1579163278535 091625
                handleGroupMessage(s, message);
            }
        });
    }

    private static class GroupMessage {
        public String content;
    }

    private void handleGroupMessage(String humanCode, Contact.Message message) {
//        GroupMessage groupMessage = new Gson().fromJson(message.data.toString(), GroupMessage.class);
        Log.d("xidaokun", "CarrierPeerNode#handleMessage#\nhumanCode:"+ humanCode + "\ncontent:"+message.data.toString());

        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setContent(message.data.toString());
        messageInfo.setFriendCode(humanCode);
        messageInfo.setTime(message.nanoTime/1000000);
        messageInfo.setMsgId(message.nanoTime/1000000);
        messageInfo.setType(Constants.CHAT_ITEM_TYPE_LEFT);
        messageInfo.setHeader("https://xidaokun.github.io/im_boy.png");
        postMessageEvent(messageInfo);

    }

    private void handleMessage(String humanCode, Contact.Message message) {
        MessageInfo messageInfo = new MessageInfo();

        String content = message.data.toString();
        if(StringUtil.isNullOrEmpty(content)) return;

        Log.d("xidaokun", "CarrierPeerNode#handleMessage#\nhumanCode:"+ humanCode + "\ncontent:"+content);

        messageInfo.setContent(content);
        messageInfo.setFriendCode(humanCode);

        messageInfo.setNickName(null);
        messageInfo.setTime(message.nanoTime/1000000);
        messageInfo.setMsgId(message.nanoTime/1000000);
        messageInfo.setHeader("https://xidaokun.github.io/im_boy.png");
        if(!StringUtil.isNullOrEmpty(getUserInfo().humanCode) && humanCode.equals(getUserInfo().humanCode)) {
            messageInfo.setType(Constants.CHAT_ITEM_TYPE_RIGHT);
        } else {
            messageInfo.setType(Constants.CHAT_ITEM_TYPE_LEFT);
        }
        postMessageEvent(messageInfo);
    }

    private void handleEvent(Contact.Listener.EventArgs event) {
        String text = "";
        switch (event.type) {
            case FriendRequest:
                //replace with IM
                Contact.Listener.RequestEvent requestEvent = (Contact.Listener.RequestEvent) event;
                String summary = requestEvent.summary;
                text = requestEvent.humanCode + " request friend, said: " + summary;
                Log.d("xidaokun", "CarrierPeerNode#handleEvent#FriendRequest#\ntext:"+ text);
                RequestFriendInfo requestFriendInfo = new RequestFriendInfo(requestEvent.humanCode, summary);
                postAddFriendEvent(requestFriendInfo);

//                Contact.Listener.RequestEvent requestEvent = (Contact.Listener.RequestEvent) event;
//                String summary = requestEvent.summary;
//                text = requestEvent.humanCode + " request friend, said: " + summary;
                Log.d("xidaokun", "CarrierPeerNode#handleEvent#FriendRequest#\ntext:"+ text);
                break;
            case StatusChanged:
                Contact.Listener.StatusEvent statusEvent = (Contact.Listener.StatusEvent) event;
                text = statusEvent.humanCode + " status changed " + statusEvent.status;
                Log.d("xidaokun", "CarrierPeerNode#handleEvent#StatusChanged#\ntext:"+ text);
                FriendStatusInfo friendStatusInfo = new FriendStatusInfo(statusEvent.humanCode, statusEvent.status);
                postFriendChangeEvent(friendStatusInfo);
                break;
            case HumanInfoChanged:
                Contact.Listener.InfoEvent infoEvent = (Contact.Listener.InfoEvent) event;
                text = event.humanCode + " info changed: " + infoEvent.toString() + infoEvent.humanInfo.status;
                Log.d("xidaokun", "CarrierPeerNode#handleEvent#HumanInfoChanged#\ntext:"+ text);
                HumanChangeInfo humanChangeInfo = new HumanChangeInfo(event.humanCode, infoEvent.toString());
                postHumanInfoChangeEvent(humanChangeInfo);
                break;
            case MessageAck:
                Contact.Listener.MsgAckEvent ackEvent = (Contact.Listener.MsgAckEvent) event;
                break;
            default:
                return;
        }
    }


    public void postAddFriendEvent(RequestFriendInfo requestFriendInfo) {
        EventBus.getDefault().post(requestFriendInfo);
    }

    public void postFriendChangeEvent(FriendStatusInfo friendStatusInfo) {
        EventBus.getDefault().post(friendStatusInfo);
    }

    public void postHumanInfoChangeEvent(HumanChangeInfo humanChangeInfo) {
        EventBus.getDefault().post(humanChangeInfo);
    }

    public void postMessageEvent(MessageInfo messageInfo) {
        EventBus.getDefault().post(messageInfo);
    }

    public int setMyInfo(Contact.HumanInfo.Item item, String value) {
        int ret = mPeerNode.setUserInfo(item, value);
        Log.d("xidaokun", "CarrierPeerNode#setMyInfo#======ret:"+ ret);
        return ret;
    }

    public void setFriendInfo(String humanCode, Contact.HumanInfo.Item item, String value) {
        int ret = mConnector.setFriendInfo(humanCode, item, value);
        Log.d("xidaokun", "CarrierPeerNode#setFriendInfo#======ret:"+ ret);
    }

    public void setGroupFriendInfo(String humanCode, Contact.HumanInfo.Item item, String value) {
        int ret = mGroupConnector.setFriendInfo(humanCode, item, value);
        Log.d("xidaokun", "CarrierPeerNode#setGroupFriendInfo#======ret:"+ ret);
    }

    public ContactInterface.UserInfo getGroupInfo() {
        return mGroupConnector.getUserInfo();
    }

    public int addFriend(String friendCode) {
        int ret = mPeerNode.addFriend(friendCode, "{\"content\": \"" + BRConstants.CHAT_SINGLE_TYPE + "\"}");
        if(0 == ret) {
            setFriendInfo(friendCode, Contact.HumanInfo.Item.Addition, "{\"type\":\"chat\",\"value\":\"" + BRConstants.CHAT_SINGLE_TYPE + "\"}");
//            String myDid = BRSharedPrefs.getMyDid(mContext);
//            String myCarrierAddr = BRSharedPrefs.getCarrierId(mContext);
//            String nickName = BRSharedPrefs.getNickname(mContext);
//            PushServer.sendNotice(myDid, friendCode, nickName, myCarrierAddr);
//            PushServer.sendIosNotice(myDid, friendCode, nickName, myCarrierAddr);
        }
        Log.d("xidaokun", "CarrierPeerNode#addFriend#======ret:"+ ret);
        return ret;
    }

    public int addGroupFriend(String friendCode) {
        int ret = mGroupConnector.addFriend(friendCode, "{\"content\": \""+ BRConstants.CHAT_GROUP_TYPE  +"\"}");
        if(0 == ret) setFriendInfo(friendCode, Contact.HumanInfo.Item.Addition,  "{\"type\":\"chat\",\"value\":\"" + BRConstants.CHAT_GROUP_TYPE + "\"}");
        Log.d("xidaokun", "CarrierPeerNode#addGroupFriend#======ret:"+ ret);
        return ret;
    }

    public int acceptFriend(String friendCode, String type) {
        if(!StringUtil.isNullOrEmpty(type) && type.equals(BRConstants.CHAT_GROUP_TYPE)) {
            setGroupFriendInfo(friendCode, Contact.HumanInfo.Item.Addition, BRConstants.CHAT_GROUP_TYPE);
        } else {
            setFriendInfo(friendCode, Contact.HumanInfo.Item.Addition, BRConstants.CHAT_SINGLE_TYPE);
        }
        int ret = mPeerNode.addFriend(friendCode, friendCode);
//        int ret = mPeerNode.acceptFriend(friendCode);
        Log.d("xidaokun", "CarrierPeerNode#acceptFriend#ret:"+ ret);
        return ret;
    }

    public ContactInterface.Status getFriendStatus(String friendCode) {
        return mPeerNode.getFriendStatus(friendCode);
    }

    public List<ContactInterface.FriendInfo> getFriends() {
        return mPeerNode.listFriendInfo();
    }

    public ContactInterface.FriendInfo getFriendInfo(String friendCode) {
        List<ContactInterface.FriendInfo> friendInfos =getFriends();
        if(friendInfos == null) return null;
        for(ContactInterface.FriendInfo friendInfo : friendInfos) {
            if(friendInfo.humanCode.equals(friendCode)) {
                return friendInfo;
            }
            List<Contact.HumanInfo.CarrierInfo> boundCarrierArray = friendInfo.boundCarrierArray;
            for(Contact.HumanInfo.CarrierInfo carrierInfo : boundCarrierArray) {
                if(carrierInfo.usrAddr.equals(friendCode)) {
                    return friendInfo;
                }
            }
        }

        return null;
    }

    public ContactInterface.UserInfo getUserInfo() {
        return mPeerNode.getUserInfo();
    }

    public int removeFriend(String friendCode) {
        return mPeerNode.removeFriend(friendCode);
    }

    public long sendMessage(String friendCode, String content) {
        Log.d("xidaokun", "CarrierPeerNode#sendMessage\n#did:"+ friendCode + "\n#content:" + content);
        long ret = mConnector.sendMessage(friendCode, Contact.Channel.Carrier, content);
        Log.d("xidaokun", "CarrierPeerNode#sendMessage#ret:"+ ret);
        return ret;
    }

    public long sendGroupMessage(String friendCode, String content) {
        Log.d("xidaokun", "CarrierPeerNode#sendGroupMessage\n#did:"+ friendCode + "\n#content:" + content);
        long ret = mGroupConnector.sendMessage(friendCode, Contact.Channel.Carrier,content);
        Log.d("xidaokun", "CarrierPeerNode#sendGroupMessage#ret:"+ ret);
        return ret;
    }

    public static class RequestFriendInfo {
        public RequestFriendInfo(String humanCode, String content) {
            this.humanCode = humanCode;
            this.content = content;
        }

        public String humanCode;
        public String content;
    }

    public static class FriendStatusInfo {
        public FriendStatusInfo(String humanCode, ContactInterface.Status status) {
            this.humanCode = humanCode;
            this.status = status;
        }

        public String humanCode;
        public ContactInterface.Status status;
    }

    public static class HumanChangeInfo {
        public HumanChangeInfo(String humanCode, String changeInfo) {
            this.humanCode = humanCode;
            this.changeInfo = changeInfo;
        }

        public String humanCode;
        public String changeInfo;
    }
}
