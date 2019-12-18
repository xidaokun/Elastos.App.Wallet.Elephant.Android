package app.elaphant.sdk.peernode;

import org.elastos.sdk.elephantwallet.contact.Contact;

import java.nio.ByteBuffer;

public class PeerNodeListener {

    public interface Listener {
        byte[] onAcquire(Contact.Listener.AcquireArgs request);
        void onError(int errCode, String errStr, String ext);
    }

    public interface MessageListener {
        void onEvent(Contact.Listener.EventArgs event);
        void onReceivedMessage(String humanCode, Contact.Channel channelType, Contact.Message message);
    }

    public interface DataListener {
        void onNotify(String humanCode, Contact.Channel channelType,
                                      String dataId, Contact.DataListener.Status status);
        int onReadData(String humanCode, Contact.Channel channelType,
                                       String dataId, long offset, ByteBuffer data);
        int onWriteData(String humanCode, Contact.Channel channelType,
                                        String dataId, long offset, byte[] data);
    }


}
