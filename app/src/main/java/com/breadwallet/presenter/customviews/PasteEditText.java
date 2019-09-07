package com.breadwallet.presenter.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

public class PasteEditText extends BREdit {
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
        Log.i("xidaokun_tag", "id:"+id);
        return super.onTextContextMenuItem(id);
    }


}
