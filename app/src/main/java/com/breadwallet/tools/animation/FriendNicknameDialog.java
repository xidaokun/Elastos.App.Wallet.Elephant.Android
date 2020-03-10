package com.breadwallet.tools.animation;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.tools.util.StringUtil;

public class FriendNicknameDialog extends Dialog {

    private TextView mPositiveBtn;
    private TextView mTitleTv;
    private TextView mRequireTv;
    private EditText mNicknameEdt;

    private OnPositiveClickListener mPositiveListener;

    private String mTitleStr;
    private String mMessageStr;
    private String mPositiveStr;

    public FriendNicknameDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friend_nickname_dialog_layout);

        setCanceledOnTouchOutside(false);
        initView();
        initListener();
        refreshUI();
    }

    private void initView() {
        mTitleTv = findViewById(R.id.title_tv);
        mPositiveBtn = findViewById(R.id.positive_btn);
        mNicknameEdt = findViewById(R.id.message_edt);
        mRequireTv = findViewById(R.id.nickname_required_hint);
    }

    private void initListener() {
        mPositiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mPositiveListener) mPositiveListener.onClick();
            }
        });

        mNicknameEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(!StringUtil.isNullOrEmpty(s.toString())) {
                    setRequireTvVisiable(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }



    public void refreshUI() {
        mTitleTv.setText(mTitleStr);
        mNicknameEdt.setHint(mMessageStr);
        mPositiveBtn.setText(mPositiveStr);
        mNicknameEdt.setText(mNicknameStr==null?"":mNicknameStr);
    }

    public void setTitleStr(String resource) {
        this.mTitleStr = resource;
    }

    private String mNicknameStr = null;
    public void setNicknameStr(String nickname) {
        mNicknameStr = nickname;
    }

    public void setRequireTvVisiable(int visiable) {
        this.mRequireTv.setVisibility(visiable);
    }

    public void setMessageStr(String resource) {
        this.mMessageStr = resource;
    }

    public String getEditText() {
        return mNicknameEdt.getText().toString();
    }

    public void setPositiveStr(String mPositiveStr) {
        this.mPositiveStr = mPositiveStr;
    }

    public void setPositiveListener(OnPositiveClickListener listener) {
        this.mPositiveListener = listener;
    }

    public interface OnPositiveClickListener {
        void onClick();
    }
}
