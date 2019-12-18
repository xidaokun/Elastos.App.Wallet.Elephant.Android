package app.elaphant.sdk.peernode;

import org.elastos.sdk.elephantwallet.contact.Contact;

import java.util.List;

public final class Connector {
    private static final String TAG = Connector.class.getName();

    private PeerNode mPeerNode = null;
    private String mServiceName;

    private PeerNodeListener.MessageListener mMessagListener = null;
    private PeerNodeListener.DataListener mDataListener = null;

    public Connector(String serviceName) {
        mPeerNode = PeerNode.getInstance();
        mServiceName = serviceName;
    }

    public void finalize() {
        removeMessageListener();
        removeDataListener();
    }

    public void setMessageListener(PeerNodeListener.MessageListener listener) {
        if (mPeerNode == null) return;
        if (mMessagListener != null) {
            removeMessageListener();
        }
        mPeerNode.addMessageListener(mServiceName, listener);
        mMessagListener = listener;
    }

    public void removeMessageListener() {
        if (mPeerNode == null) return;
        mPeerNode.removeMessageListener(mServiceName, mMessagListener);
        mMessagListener = null;
    }

    public void setDataListener(PeerNodeListener.DataListener listener) {
        if (mPeerNode == null) return;
        if (mDataListener != null) {
            removeDataListener();
        }
        mPeerNode.addDataListener(mServiceName, listener);
        mDataListener = listener;
    }

    public void removeDataListener() {
        if (mPeerNode == null) return;
        if (mDataListener == null) return;
        mPeerNode.removeDataListener(mServiceName, mDataListener);
        mDataListener = null;
    }

    public int addFriend(String friendCode, String summary) {
        if (mPeerNode == null) return -1;
        return mPeerNode.addFriend(friendCode, summary);
    }

    public int removeFriend(String friendCode) {
        if (mPeerNode == null) return -1;
        return mPeerNode.removeFriend(friendCode);
    }

    public int acceptFriend(String friendCode) {
        if (mPeerNode == null) return -1;
        return mPeerNode.acceptFriend(friendCode);
    }

    public int setFriendInfo(String humanCode, Contact.HumanInfo.Item item, String value) {
        if (mPeerNode == null) return -1;
        return mPeerNode.setFriendInfo(humanCode, item, value);
    }

    public List<Contact.FriendInfo> listFriendInfo() {
        if (mPeerNode == null) return null;
        return mPeerNode.listFriendInfo();
    }

    public List<String> listFriendCode() {
        if (mPeerNode == null) return null;
        return mPeerNode.listFriendCode();
    }

    public Contact.Status getStatus() {
        if (mPeerNode == null) return Contact.Status.Invalid;
        return mPeerNode.getStatus();
    }

    public Contact.Status getFriendStatus(String friendCode) {
        if (mPeerNode == null) return Contact.Status.Invalid;
        return mPeerNode.getFriendStatus(friendCode);
    }

    public int sendMessage(String friendCode, Contact.Message message) {
        if (mPeerNode == null) return -1;
        return mPeerNode.sendMessage(friendCode, message);
    }
}
