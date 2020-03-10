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
    private OnItemClickListener mListener;

    public static final int TYPE_FOOTER = 0;
    public static final int TYPE_NORMAL = 1;

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
        this.mListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == TYPE_FOOTER) {
            return new FooterViewHolder(mInflater.inflate(R.layout.chat_friend_contact_footer, parent, false));
        }
        return new NormalViewHolder(mInflater.inflate(R.layout.chat_friend_contact_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if(holder instanceof NormalViewHolder) {
            final NormalViewHolder viewHolder = ((NormalViewHolder)holder);
            final ContactEntity contactEntity = mDatas.get(position);
            viewHolder.name.setText(contactEntity.getContact());
            if(contactEntity.isTop()) {
                viewHolder.alias.setVisibility(View.GONE);
            } else {
                viewHolder.alias.setVisibility(View.VISIBLE);
            }
            viewHolder.alias.setText(contactEntity.getContact().substring(0, 1));
            viewHolder.sendTokenLayout.setVisibility(View.GONE);
            if(contactEntity.isShowBottom()) viewHolder.sendTokenLayout.setVisibility(View.VISIBLE);
            viewHolder.content.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != mListener) mListener.onItemClick(v, position);
                    if(position==0 || viewHolder.sendTokenLayout.getVisibility()==View.VISIBLE) {
                        viewHolder.sendTokenLayout.setVisibility(View.GONE);
                        contactEntity.setShowBottom(false);
                    } else {
                        viewHolder.sendTokenLayout.setVisibility(View.VISIBLE);
                        contactEntity.setShowBottom(true);
                    }
                }
            });

            int count = contactEntity.getWaitAcceptCount();
            if(count != 0) {
                viewHolder.waitForAcceptCount.setVisibility(View.VISIBLE);
                viewHolder.waitForAcceptCount.setText(String.valueOf(count));
            } else {
                viewHolder.waitForAcceptCount.setVisibility(View.GONE);
            }

            viewHolder.content.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mListener.longPress(v, position);
                    return true;
                }
            });
            viewHolder.sendToken.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != mListener) mListener.sendToken(v, position);
                }
            });
            viewHolder.sendMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != mListener) mListener.sendMessage(v, position);
                }
            });
            viewHolder.editNickname.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != mListener) mListener.editNickname(v, position);
                }
            });
            viewHolder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(null != mListener) mListener.deleteFriends(v, position);
                }
            });
            if(contactEntity.isTop()) {
                viewHolder.logo.setImageResource(R.drawable.new_friend);
            } else if(mDatas.get(position).isOnline()) {
                viewHolder.logo.setImageResource(R.drawable.chat_head_online_bg);
            } else {
                viewHolder.logo.setImageResource(R.drawable.chat_head_offline_bg);
            }

        }
    }

    @Override
    public int getItemCount() {
        return (mDatas==null)? 0: mDatas.size()+1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount()-1){
            return TYPE_FOOTER;
        }
        return TYPE_NORMAL;
    }

    public class NormalViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView alias;
        TextView waitForAcceptCount;
        RoundImageView logo;
        View content;
        View sendTokenLayout;
        View sendToken;
        View sendMessage;
        View editNickname;
        View delete;

        public NormalViewHolder(View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.chat_contact_item_logo);
            name = itemView.findViewById(R.id.chat_contact_item_name);
            alias = itemView.findViewById(R.id.alias);
            waitForAcceptCount = itemView.findViewById(R.id.wait_for_accept_count_tv);
            content = itemView.findViewById(R.id.content);
            sendTokenLayout = itemView.findViewById(R.id.chat_contact_send_token_view);
            sendToken = itemView.findViewById(R.id.chat_contact_item_send_token);
            sendMessage = itemView.findViewById(R.id.chat_contact_item_send_message);
            editNickname = itemView.findViewById(R.id.chat_contact_item_edit_nickname);
            delete = itemView.findViewById(R.id.chat_contact_item_delete);
        }
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public interface OnItemClickListener {
        void longPress(View view, int position);
        void onItemClick(View view, int position);
        void sendToken(View view, int position);
        void sendMessage(View view, int position);
        void editNickname(View view, int position);
        void deleteFriends(View view, int position);
    }
}
