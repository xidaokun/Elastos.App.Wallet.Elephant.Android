package com.breadwallet.tools.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.RoundImageView;
import com.breadwallet.presenter.entities.ChatMsgEntity;

import java.util.List;

public class ChatMessageAdapter extends BaseAdapter {

    private Context mContext;
    private List<ChatMsgEntity> mEntities;

    public ChatMessageAdapter(Context context, List<ChatMsgEntity> entities) {
        this.mContext = context;
        this.mEntities = entities;
    }

    @Override
    public int getCount() {
        return (null!=mEntities)?mEntities.size():0;
    }

    @Override
    public Object getItem(int position) {
        return (null!=mEntities)?mEntities.get(position):null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (null == convertView) {
            convertView = View.inflate(mContext, R.layout.chat_friend_message_item, null);
            viewHolder = new ViewHolder();
            viewHolder.nameTv = convertView.findViewById(R.id.chat_item_name);
            viewHolder.msgTv = convertView.findViewById(R.id.chat_item_msg);
            viewHolder.timeTv = convertView.findViewById(R.id.chat_item_time);
            viewHolder.countTv = convertView.findViewById(R.id.chat_item_count);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.nameTv.setText(mEntities.get(position).getName());
        viewHolder.msgTv.setText(mEntities.get(position).getMessage());
        viewHolder.timeTv.setText(mEntities.get(position).getTimeStamp());
        viewHolder.countTv.setText(mEntities.get(position).getCount());
        final int pos = position;
        return convertView;
    }

    class ViewHolder {
        public RoundImageView logoImag;
        public TextView nameTv;
        public TextView msgTv;
        public TextView timeTv;
        public TextView countTv;
    }
}