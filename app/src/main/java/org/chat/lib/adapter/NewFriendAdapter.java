package org.chat.lib.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.tools.util.BRConstants;

import org.chat.lib.entity.NewFriendBean;
import org.chat.lib.widget.StateButton;

import java.util.List;

public class NewFriendAdapter extends BaseAdapter {

    private Context mContext;
    private List<NewFriendBean> mData;
    private OnItemListener mListener;

    public NewFriendAdapter(Context context, List<NewFriendBean> datas) {
        this.mContext = context;
        this.mData = datas;
    }

    public void setOnItemListener(OnItemListener listener) {
        this.mListener = listener;
    }

    @Override
    public int getCount() {
        return (mData==null)?0:mData.size();
    }

    @Override
    public Object getItem(int position) {
        return (mData==null)?null:mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (null == convertView) {
            convertView = View.inflate(mContext, R.layout.chat_new_friend_item_layout, null);
            viewHolder = new ViewHolder();
            viewHolder.nameTv = convertView.findViewById(R.id.friend_name);
            viewHolder.sendSb = convertView.findViewById(R.id.accept_btn);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.nameTv.setText(mData.get(position).nickName);
        viewHolder.sendSb.setClickable(false);
        viewHolder.sendSb.setText("");
        if(mData.get(position).acceptStatus == BRConstants.RECEIVE_ACCEPT) {
            viewHolder.sendSb.setText(mContext.getString(R.string.My_chat_new_accept));
            viewHolder.sendSb.setClickable(true);
        } else if(mData.get(position).acceptStatus == BRConstants.REQUEST_ACCEPT) {
            viewHolder.sendSb.setText(mContext.getString(R.string.My_chat_new_waitforaccept));
            viewHolder.sendSb.setClickable(false);
        } else if(mData.get(position).acceptStatus == BRConstants.ACCEPTED) {
            viewHolder.sendSb.setText(mContext.getString(R.string.My_chat_new_accepted));
            viewHolder.sendSb.setClickable(false);
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mListener) mListener.onItemClick(v, position);
            }
        });

        viewHolder.sendSb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int status = mData.get(position).acceptStatus;
                if(status==BRConstants.RECEIVE_ACCEPT && null!=mListener) mListener.accept(v, position);
            }
        });

        return convertView;
    }

    class ViewHolder {
        public TextView nameTv;
        public StateButton sendSb;
    }

    public interface OnItemListener {
        void accept(View view, int position);
        void onItemClick(View view, int position);
    }

}
