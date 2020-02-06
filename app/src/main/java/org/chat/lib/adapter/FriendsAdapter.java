package org.chat.lib.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.breadwallet.R;
import org.chat.lib.entity.ContactEntity;
import org.chat.lib.widget.RoundImageView;

import java.util.List;


public class FriendsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private Context mContext;
    private List<ContactEntity> mDatas;
    private LayoutInflater mInflater;
    private OnItemClickListener mClickListener;

//    public static final int TYPE_HEADER = 0;
//    public static final int TYPE_NORMAL = 1;

    public FriendsAdapter(Context context, List<ContactEntity> datas) {
        this.mContext = context;
        this.mDatas = datas;
        mInflater = LayoutInflater.from(mContext);
    }

    public FriendsAdapter setDatas(List<ContactEntity> datas) {
        mDatas = datas;
        return this;
    }

    public void setOnClickListener(OnItemClickListener listener) {
        this.mClickListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        if(viewType==TYPE_HEADER) return new HeaderViewHolder(mInflater.inflate(R.layout.chat_friend_contact_header_layout, null));
        return new NormalViewHolder(mInflater.inflate(R.layout.chat_friend_contact_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if(holder instanceof NormalViewHolder) {
            final NormalViewHolder viewHolder = ((NormalViewHolder)holder);
            final ContactEntity contactEntity = mDatas.get(position);
            viewHolder.name.setText(contactEntity.getContact());
            viewHolder.sendTokenLayout.setVisibility(View.GONE);
            if(mDatas.get(position).isShowBottom()) viewHolder.sendTokenLayout.setVisibility(View.VISIBLE);
            viewHolder.content.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != mClickListener) mClickListener.onItemClick(v, position);
                    if(viewHolder.sendTokenLayout.getVisibility()==View.VISIBLE) {
                        viewHolder.sendTokenLayout.setVisibility(View.GONE);
                        mDatas.get(position).setShowBottom(false);
                    } else {
                        viewHolder.sendTokenLayout.setVisibility(View.VISIBLE);
                        mDatas.get(position).setShowBottom(true);
                    }
                }
            });
            viewHolder.sendToken.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != mClickListener) mClickListener.sendToken(v, position);
                }
            });
            viewHolder.sendMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != mClickListener) mClickListener.sendMessage(v, position);
                }
            });
            viewHolder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != mClickListener) mClickListener.deleteFriends(v, position);
                }
            });
            viewHolder.logo.setImageResource(R.drawable.emotion_duoyun);
        }
    }

    @Override
    public int getItemCount() {
        return (mDatas==null)? 0: mDatas.size();
    }

//    @Override
//    public int getItemViewType(int position) {
//        if(position == 0) return TYPE_HEADER;
//        return TYPE_NORMAL;
//    }

    public class NormalViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        RoundImageView logo;
        View content;
        View sendTokenLayout;
        View sendToken;
        View sendMessage;
        View delete;

        public NormalViewHolder(View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.chat_contact_item_logo);
            name = itemView.findViewById(R.id.chat_contact_item_name);
            content = itemView.findViewById(R.id.content);
            sendTokenLayout = itemView.findViewById(R.id.chat_contact_send_token_view);
            sendToken = itemView.findViewById(R.id.chat_contact_item_send_token);
            sendMessage = itemView.findViewById(R.id.chat_contact_item_send_message);
            delete = itemView.findViewById(R.id.chat_contact_item_delete);
        }
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
        void sendToken(View view, int position);
        void sendMessage(View view, int position);
        void deleteFriends(View view, int position);

    }
}
