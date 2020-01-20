package org.chat.lib.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.tools.util.BRDateUtil;

import org.chat.lib.entity.ChatMsgEntity;
import org.chat.lib.widget.RoundImageView;

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
        viewHolder.timeTv.setText(BRDateUtil.getFormatDate(mEntities.get(position).getTimeStamp(), "yyyy-MM-dd hh:mm:ss"));
        int count = mEntities.get(position).getCount();
        if(count == 0) {
            viewHolder.countTv.setVisibility(View.GONE);
        } else {
            viewHolder.countTv.setVisibility(View.VISIBLE);
            viewHolder.countTv.setText(String.valueOf(count));
        }
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
