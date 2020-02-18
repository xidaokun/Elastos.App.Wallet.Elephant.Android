package com.breadwallet.tools.animation;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.breadwallet.R;

public class ElaphantDialogEdit extends Dialog {

    private TextView mPositiveBtn;
    private TextView mNegativeBtn;
    private TextView mTitleTv;
    private EditText mNicknameEdt;

    private OnPositiveClickListener mPositiveListener;
    private OnNegativeClickListener mNegativeListener;

    private String mTitleStr;
    private String mMessageStr;
    private String mPositiveStr;
    private String mNegativeStr;

    public ElaphantDialogEdit(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.elaphant_dialog_edit_layout);

        setCanceledOnTouchOutside(false);
        initView();
        initListener();
        initData();
    }

    private void initView() {
        mTitleTv = findViewById(R.id.title_tv);
        mNegativeBtn = findViewById(R.id.negative_btn);
        mPositiveBtn = findViewById(R.id.positive_btn);
        mNicknameEdt = findViewById(R.id.message_edt);
    }

    private void initListener() {
        mPositiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mPositiveListener) mPositiveListener.onClick();
            }
        });

        mNegativeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mNegativeListener) mNegativeListener.onClick();
            }
        });
    }

    private void initData() {
        mTitleTv.setText(mTitleStr);
        mNicknameEdt.setHint(mMessageStr);
        mPositiveBtn.setText(mPositiveStr);
        mNegativeBtn.setText(mNegativeStr);
    }

    public void setPositiveListener(OnPositiveClickListener listener) {
        this.mPositiveListener = listener;
    }

    public void setNegativeListener(OnNegativeClickListener listener) {
        this.mNegativeListener = listener;
    }

    public void setTitleStr(String resource) {
        this.mTitleStr = resource;
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

    public void setNegativeStr(String mNegativeStr) {
        this.mNegativeStr = mNegativeStr;
    }

    public interface OnPositiveClickListener {
        void onClick();
    }

    public interface OnNegativeClickListener {
        void onClick();
    }
}
