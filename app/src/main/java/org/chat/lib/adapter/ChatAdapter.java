package org.chat.lib.adapter;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.chat.lib.adapter.holder.ChatAcceptViewHolder;
import org.chat.lib.adapter.holder.ChatSendViewHolder;
import org.chat.lib.entity.MessageInfo;
import org.chat.lib.utils.Constants;
import org.easy.recycleview.adapter.BaseViewHolder;
import org.easy.recycleview.adapter.RecyclerArrayAdapter;


public class ChatAdapter extends RecyclerArrayAdapter<MessageInfo> {

    private onItemClickListener onItemClickListener;
    public Handler handler;

    public ChatAdapter(Context context) {
        super(context);
        handler = new Handler();
    }

    @Override
    public BaseViewHolder OnCreateViewHolder(ViewGroup parent, int viewType) {
        BaseViewHolder viewHolder = null;
        switch (viewType) {
            case Constants.CHAT_ITEM_TYPE_LEFT:
                viewHolder = new ChatAcceptViewHolder(parent, onItemClickListener, handler);
                break;
            case Constants.CHAT_ITEM_TYPE_RIGHT:
                viewHolder = new ChatSendViewHolder(parent, onItemClickListener, handler);
                break;
        }
        return viewHolder;
    }

    @Override
    public int getViewType(int position) {
        return getAllData().get(position).getType();
    }

    public void addItemClickListener(onItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface onItemClickListener {
        void onHeaderClick(int position);

        void onImageClick(View view, int position);

        void onVoiceClick(ImageView imageView, int position);

        void onFailedClick(View view, int position);
    }
}
