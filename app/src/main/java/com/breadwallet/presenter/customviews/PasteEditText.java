package com.breadwallet.presenter.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.breadwallet.presenter.interfaces.EditPasteListener;

public class PasteEditText extends BREdit {

    private EditPasteListener mListener;

    public PasteEditText(Context context) {
        super(context);
    }

    public PasteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PasteEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        if(id==android.R.id.paste && mListener!=null) {
            mListener.onPaste(this);
            return true;
        }
        return super.onTextContextMenuItem(id);
    }

    public void setPasteListener(EditPasteListener listener) {
        this.mListener = listener;
    }


}
