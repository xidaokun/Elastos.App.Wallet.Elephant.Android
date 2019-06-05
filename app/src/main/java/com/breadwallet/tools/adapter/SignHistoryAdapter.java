package com.breadwallet.tools.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.SignHistoryItem;

import java.util.List;

public class SignHistoryAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private List<SignHistoryItem> mData;

    public SignHistoryAdapter(Context context, List<SignHistoryItem> data){
        this.mContext = context;
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
            convertView = mInflater.inflate(R.layout.did_auth_item_layout, parent, false);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.mTime.setText(mData.get(position).time+"");

        return convertView;
    }

    private static class ViewHolder {
        private BaseTextView mTime;
    }
}
