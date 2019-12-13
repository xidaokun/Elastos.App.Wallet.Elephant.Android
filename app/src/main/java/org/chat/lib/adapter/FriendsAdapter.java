package org.chat.lib.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.breadwallet.R;
import org.chat.lib.entity.ContactEntity;
import org.chat.lib.widget.RoundImageView;

import java.util.List;


public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder>{

    private Context mContext;
    private List<ContactEntity> mDatas;
    private LayoutInflater mInflater;
    private OnItemClickListener mListener;

    public FriendsAdapter(Context context, List<ContactEntity> datas) {
        this.mContext = context;
        this.mDatas = datas;
        mInflater = LayoutInflater.from(mContext);
    }

    public FriendsAdapter setDatas(List<ContactEntity> datas) {
        mDatas = datas;
        return this;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(R.layout.chat_friend_contact_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final ContactEntity contactEntity = mDatas.get(position);
        holder.name.setText(contactEntity.getContact());
        holder.content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mListener) mListener.OnItemClick(v, position);
            }
        });
        holder.logo.setImageResource(R.drawable.emotion_duoyun);
    }

    @Override
    public int getItemCount() {
        return mDatas != null ? mDatas.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        RoundImageView logo;
        View content;

        public ViewHolder(View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.chat_contact_item_logo);
            name = itemView.findViewById(R.id.chat_contact_item_name);
            content = itemView.findViewById(R.id.content);
        }
    }

    public interface OnItemClickListener {
        void OnItemClick(View view, int position);
    }
}
