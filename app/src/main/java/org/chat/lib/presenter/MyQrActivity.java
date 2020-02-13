package org.chat.lib.presenter;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.util.StringUtil;

import org.chat.lib.utils.Utils;
import org.elastos.sdk.elephantwallet.contact.internal.ContactInterface;
import org.node.CarrierPeerNode;

public class MyQrActivity extends BRActivity {

    private ImageView mQrImg;
    private TextView mNicknameTv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_qr_layout);

        initView();
        initListener();
    }

    private void initView() {
        mQrImg = findViewById(R.id.my_qr_img);
        mNicknameTv = findViewById(R.id.my_nickname);

        String nickname = BRSharedPrefs.getNickname(this);
        mNicknameTv.setText(nickname);
        ContactInterface.UserInfo userInfo = CarrierPeerNode.getInstance(this).getUserInfo();
        if(userInfo == null) return;
        String carrierAddr = userInfo.getCurrDevCarrierAddr();
        if(!StringUtil.isNullOrEmpty(carrierAddr)) {
            Bitmap bitmap = QRUtils.encodeAsBitmap(carrierAddr, Utils.dp2px(this, 300));
            mQrImg.setImageBitmap(bitmap);
        }
    }

    private void initListener() {
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

}
