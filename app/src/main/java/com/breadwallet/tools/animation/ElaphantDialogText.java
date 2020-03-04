package com.breadwallet.tools.animation;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.tools.util.StringUtil;

public class ElaphantDialogText extends Dialog {

    private TextView mPositiveBtn;
    private TextView mNegativeBtn;
    private TextView mTitleTv;
    private TextView mMessageTv;

    private OnPositiveClickListener mPositiveListener;
    private OnNegativeClickListener mNegativeListener;

    private String mTitleStr;
    private String mMessageStr;
    private Spanned mMessageSpan;
    private String mPositiveStr;
    private String mNegativeStr;

    public ElaphantDialogText(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.elaphant_dialog_text_layout);

        setCanceledOnTouchOutside(false);
        initView();
        initListener();
        initData();
    }

    private void initView() {
        mTitleTv = findViewById(R.id.title_tv);
        mNegativeBtn = findViewById(R.id.negative_btn);
        mPositiveBtn = findViewById(R.id.positive_btn);
        mMessageTv = findViewById(R.id.message_tv);
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
        mMessageTv.setText(!StringUtil.isNullOrEmpty(mMessageStr)?mMessageStr:mMessageSpan);
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

    public void setMessageSpan(Spanned message) {
        this.mMessageSpan = message;
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
