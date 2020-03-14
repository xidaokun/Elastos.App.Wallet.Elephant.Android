package org.chat.lib.presenter;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.tools.animation.ElaphantDialogText;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.qrcode.QRUtils;
import com.google.gson.Gson;

import org.chat.lib.push.PushServer;
import org.chat.lib.utils.Utils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.node.CarrierPeerNode;

public class MyQrActivity extends BRActivity {

    private ImageView mQrImg;
    private TextView mNicknameTv;
    private BRButton mSwitchBtn;
    private boolean mIsDidQr;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_qr_layout);

        initView();
        initListener();

//        EventBus.getDefault().register(this);
    }

    private void initView() {
        mQrImg = findViewById(R.id.my_qr_img);
        mNicknameTv = findViewById(R.id.my_nickname);
        mSwitchBtn = findViewById(R.id.my_qr_switch_btn);

        String nickname = BRSharedPrefs.getNickname(this);
        mNicknameTv.setText(nickname);
        showCarrierQr();
    }

    private void showCarrierQr() {
        String carrierAddr = BRSharedPrefs.getCarrierId(this);
        Bitmap bitmap = QRUtils.encodeAsBitmap(carrierAddr==null?"":carrierAddr, Utils.dp2px(this, 300));
        mQrImg.setImageBitmap(bitmap);

        mSwitchBtn.setText(getResources().getString(R.string.My_qr_switch_btn_carrier_addr));
    }

    static class DidBean {
        public String nickname;
        public String did;
    }
    private void showDidQr() {
        String nickname = BRSharedPrefs.getNickname(this);
        String did = BRSharedPrefs.getMyDid(this);

        DidBean didBean = new DidBean();
        didBean.nickname = nickname;
        didBean.did = did;

        String tmp = new Gson().toJson(didBean);

        Bitmap bitmap = QRUtils.encodeAsBitmap(tmp==null?"":tmp, Utils.dp2px(this, 300));
        mQrImg.setImageBitmap(bitmap);

        mSwitchBtn.setText(getResources().getString(R.string.My_qr_switch_btn_did));
    }

    @Subscribe(threadMode = ThreadMode.MAIN, priority = 1)
    public void acceptFriend(final CarrierPeerNode.RequestFriendInfo requestFriendInfo) {
        Log.d("xidaokun", "HomeActivity#acceptFriend#\nhumancode:"+ requestFriendInfo.humanCode + "\ncontent:" + requestFriendInfo.content);
        final ElaphantDialogText elaphantDialog = new ElaphantDialogText(this);
        elaphantDialog.setMessageStr("添加好友请求");
        elaphantDialog.setPositiveStr("接受");
        elaphantDialog.setNegativeStr("拒绝");
        elaphantDialog.setPositiveListener(new ElaphantDialogText.OnPositiveClickListener() {
            @Override
            public void onClick() {
                CarrierPeerNode.getInstance(MyQrActivity.this).acceptFriend(requestFriendInfo.humanCode, requestFriendInfo.content);
                EventBus.getDefault().post(requestFriendInfo.humanCode);
                elaphantDialog.dismiss();
            }
        });
        elaphantDialog.setNegativeListener(new ElaphantDialogText.OnNegativeClickListener() {
            @Override
            public void onClick() {
                elaphantDialog.dismiss();
            }
        });
        elaphantDialog.show();
    }


    private void initListener() {
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsDidQr = !mIsDidQr;
                if(mIsDidQr) {
                    showDidQr();
                } else {
                    showCarrierQr();
                }
                String did = BRSharedPrefs.getMyDid(MyQrActivity.this);
                String carrier = BRSharedPrefs.getCarrierId(MyQrActivity.this);
                String nickname = BRSharedPrefs.getNickname(MyQrActivity.this);

                PushServer.setIosNotice(did, "iYWSBvFruHyN39P19GVNqvikvDigDut2ez", nickname, carrier);
            }
        });
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
//        EventBus.getDefault().unregister(this);
    }
}
