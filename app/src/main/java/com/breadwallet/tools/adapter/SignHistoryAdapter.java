package com.breadwallet.tools.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.SignHistoryItem;
import com.breadwallet.tools.util.BRDateUtil;

import java.util.List;

public class SignHistoryAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private List<SignHistoryItem> mData;

    public SignHistoryAdapter(Context context, List<SignHistoryItem> data){
        this.mData = data;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return (mData!=null)? mData.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return (mData!=null) ? mData.get(position) : 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if(null == convertView){
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.signature_history_item_layout, parent, false);

            viewHolder.mTime = convertView.findViewById(R.id.sign_date);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        long time = mData.get(position).time;
        String timeFormat = BRDateUtil.getAuthorDate((time==0) ? System.currentTimeMillis() : time);
        viewHolder.mTime.setText(timeFormat);

        return convertView;
    }

    private static class ViewHolder {
        private BaseTextView mTime;
    }
}
