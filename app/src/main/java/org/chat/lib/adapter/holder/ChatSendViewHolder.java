package org.chat.lib.adapter.holder;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.breadwallet.tools.util.BRDateUtil;
import com.bumptech.glide.Glide;

import com.breadwallet.R;
import org.chat.lib.adapter.ChatAdapter;
import org.chat.lib.entity.MessageInfo;
import org.chat.lib.utils.Constants;
import org.chat.lib.utils.Utils;
import org.chat.lib.widget.BubbleImageView;
import org.chat.lib.widget.GifTextView;
import org.easy.recycleview.adapter.BaseViewHolder;

public class ChatSendViewHolder extends BaseViewHolder<MessageInfo> {

    TextView chatItemDate;
    ImageView chatItemHeader;
    GifTextView chatItemContentText;
    BubbleImageView chatItemContentImage;
    TextView chatItemFail;
    ProgressBar chatItemProgress;
    ImageView chatItemVoice;
    LinearLayout chatItemLayoutContent;
    TextView chatItemVoiceTime;
    private ChatAdapter.onItemClickListener onItemClickListener;
    private Handler handler;

    private void initView(View rootView) {
        chatItemDate = rootView.findViewById(R.id.chat_item_date);
        chatItemHeader = rootView.findViewById(R.id.chat_item_header);
        chatItemContentText = rootView.findViewById(R.id.chat_item_content_text);
        chatItemFail = rootView.findViewById(R.id.chat_item_fail);
        chatItemContentImage = rootView.findViewById(R.id.chat_item_content_image);
        chatItemProgress = rootView.findViewById(R.id.chat_item_progress);
        chatItemVoice = rootView.findViewById(R.id.chat_item_voice);
        chatItemLayoutContent = rootView.findViewById(R.id.chat_item_layout_content);
        chatItemVoiceTime = rootView.findViewById(R.id.chat_item_voice_time);
    }

    public ChatSendViewHolder(ViewGroup parent, ChatAdapter.onItemClickListener onItemClickListener, Handler handler) {
        super(parent, R.layout.item_chat_send);
        initView(itemView);
        this.onItemClickListener = onItemClickListener;
        this.handler = handler;
    }

    @Override
    public void setData(MessageInfo data) {
        chatItemDate.setText(BRDateUtil.getFormatDate(data.getTime(), "MM-dd hh:mm:ss a"));
//        Glide.with(getContext()).load(data.getHeader()).into(chatItemHeader);
        chatItemHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.onHeaderClick(getDataPosition());
            }
        });

        chatItemFail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(onItemClickListener != null) {
                    onItemClickListener.onFailedClick(chatItemFail, getDataPosition());
                    chatItemProgress.setVisibility(View.VISIBLE);
                    chatItemFail.setVisibility(View.GONE);
                }
            }
        });

        if (data.getContent() != null) {
            chatItemContentText.setSpanText(handler, data.getContent(), true);
            chatItemVoice.setVisibility(View.GONE);
            chatItemContentText.setVisibility(View.VISIBLE);

            if(data.getSendState()==Constants.CHAT_ITEM_SEND_SUCCESS) {
                Drawable drawable = getContext().getResources().getDrawable(R.drawable.ic_send_success);
                drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                chatItemContentText.setCompoundDrawables(null, null, drawable, null);
            } else {
                chatItemContentText.setCompoundDrawables(null, null, null, null);
            }
            chatItemLayoutContent.setVisibility(View.VISIBLE);
            chatItemVoiceTime.setVisibility(View.GONE);
            chatItemContentImage.setVisibility(View.GONE);
        } else if (data.getImageUrl() != null) {
            chatItemVoice.setVisibility(View.GONE);
            chatItemLayoutContent.setVisibility(View.GONE);
            chatItemVoiceTime.setVisibility(View.GONE);
            chatItemContentText.setVisibility(View.GONE);
            chatItemContentImage.setVisibility(View.VISIBLE);
            Glide.with(getContext()).load(data.getImageUrl()).into(chatItemContentImage);
            chatItemContentImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onImageClick(chatItemContentImage, getDataPosition());
                }
            });
        } else if (data.getFilepath() != null) {
            chatItemVoice.setVisibility(View.VISIBLE);
            chatItemLayoutContent.setVisibility(View.VISIBLE);
            chatItemContentText.setVisibility(View.GONE);
            chatItemVoiceTime.setVisibility(View.VISIBLE);
            chatItemContentImage.setVisibility(View.GONE);
            chatItemVoiceTime.setText(Utils.formatTime(data.getVoiceTime()));
            chatItemLayoutContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onVoiceClick(chatItemVoice, getDataPosition());
                }
            });
        }
        switch (data.getSendState()) {
            case Constants.CHAT_ITEM_SENDING:
                chatItemProgress.setVisibility(View.VISIBLE);
                chatItemFail.setVisibility(View.GONE);
                break;
            case Constants.CHAT_ITEM_SEND_ERROR:
                chatItemProgress.setVisibility(View.GONE);
                chatItemFail.setVisibility(View.VISIBLE);
                break;
            case Constants.CHAT_ITEM_SEND_SUCCESS:
                chatItemProgress.setVisibility(View.GONE);
                chatItemFail.setVisibility(View.GONE);
                break;
        }
    }

}
