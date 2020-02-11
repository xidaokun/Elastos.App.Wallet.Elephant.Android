package org.chat.lib.adapter;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRDateUtil;

import org.chat.lib.entity.ChatMsgEntity;
import org.chat.lib.widget.RoundImageView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ChatMessageAdapter extends BaseAdapter {

    private Context mContext;
    private List<ChatMsgEntity> mEntities;
    private OnItemListener mListener;

    public ChatMessageAdapter(Context context, List<ChatMsgEntity> entities) {
        this.mContext = context;
        this.mEntities = entities;
    }

    public void setListener(OnItemListener listener) {
        this.mListener = listener;
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

    boolean mLongPress = false;
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
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
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onClick(v, position);
            }
        });

        convertView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View v, final MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN) {
                    mLongPress = true;
                    final float x = event.getX();
                    final float y = event.getY();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(600);
                                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(mLongPress && mListener!=null) mListener.onLongPress(v, position, x, y);
                                        mLongPress = false;
                                    }
                                });
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } else if (event.getAction()==MotionEvent.ACTION_UP) {
                    mLongPress = false;
                }
                return false;
            }
        });
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

    public interface OnItemListener {
        void onLongPress(View view, int position, float x, float y);
        void onClick(View view, int position);
    }
}