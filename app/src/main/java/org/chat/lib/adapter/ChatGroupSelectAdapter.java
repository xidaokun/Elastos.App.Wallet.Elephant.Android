package org.chat.lib.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.breadwallet.R;

import org.chat.lib.entity.ChatGroupSelectEntity;

import java.util.List;

public class ChatGroupSelectAdapter extends BaseAdapter {

    private Context mContext;
    private List<ChatGroupSelectEntity> mDatas;

    public ChatGroupSelectAdapter(Context context, List<ChatGroupSelectEntity> datas) {
        mContext = context;
        this.mDatas = datas;
    }

    @Override
    public int getCount() {
        return (mDatas==null)?0:mDatas.size();
    }

    @Override
    public Object getItem(int position) {
        return (mDatas==null)?null:mDatas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (null == convertView) {
            convertView = View.inflate(mContext, R.layout.chat_group_select_item_layout, null);
            viewHolder = new ViewHolder();
            viewHolder.nameTv = convertView.findViewById(R.id.friend_name);
            viewHolder.selectTag = convertView.findViewById(R.id.select_tag);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.nameTv.setText(mDatas.get(position).getName());
        viewHolder.selectTag.setVisibility(View.GONE);
        if(mDatas.get(position).isSelected()) {
            viewHolder.selectTag.setVisibility(View.VISIBLE);
        }
        return convertView;
    }

    class ViewHolder {
        public TextView nameTv;
        public TextView selectTag;

    }
}
