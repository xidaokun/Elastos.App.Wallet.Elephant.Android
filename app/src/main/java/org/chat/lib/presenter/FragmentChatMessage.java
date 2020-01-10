package org.chat.lib.presenter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.breadwallet.R;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;

import org.chat.lib.adapter.ChatMessageAdapter;
import org.chat.lib.entity.ChatMsgEntity;
import org.chat.lib.entity.MessageCacheBean;
import org.chat.lib.entity.MessageItemBean;
import org.chat.lib.source.ChatDataSource;

import java.util.ArrayList;
import java.util.List;

public class FragmentChatMessage extends BaseFragment {

    private static final String TAG = FragmentChatMessage.class.getSimpleName() + "_log";

    private ListView mListView;

    public static FragmentChatMessage newInstance(String title) {
        FragmentChatMessage f = new FragmentChatMessage();
        f.setTitle(title);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat_message, container, false);
        initView(rootView);
        initListener();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    private void refreshData() {
        entities.clear();
        List<MessageItemBean> messageItemBeans = ChatDataSource.getInstance(getContext()).getMessageItemInfos();
        for(MessageItemBean messageCacheBean : messageItemBeans) {
            List<String> friendCode = messageCacheBean.friendCodes;

            if(null == friendCode) continue;

            List<MessageCacheBean> allMessageCacheBeans  = ChatDataSource.getInstance(getContext()).getMessage(BRSQLiteHelper.CHAT_MESSAGE_FRIENDCODE + " = ? ", new String[]{friendCode.toString()});
            List<MessageCacheBean> hasNotReadCacheBeans = ChatDataSource.getInstance(getContext()).getMessage(BRSQLiteHelper.CHAT_MESSAGE_FRIENDCODE + " = ? AND " + BRSQLiteHelper.CHAT_MESSAGE_HAS_READ + " = ? ", new String[]{friendCode.toString(), String.valueOf(0)});

            if(null != allMessageCacheBeans) {
                int count = allMessageCacheBeans.size();
                if(count > 0) {
                    MessageCacheBean lastBean = allMessageCacheBeans.get(count - 1);
                    ChatMsgEntity entity = new ChatMsgEntity();
                    entity.setName(lastBean.MessageNickname);
                    entity.setMessage(lastBean.MessageContent);
                    entity.setTimeStamp(lastBean.MessageTimestamp);
                    entity.setFriendCodes(lastBean.MessageFriendCodes);
                    entity.setCount((null!=hasNotReadCacheBeans && hasNotReadCacheBeans.size()>0)? hasNotReadCacheBeans.size()-1 : 0);

                    entities.add(entity);
                    mAdapter.notifyDataSetChanged();
                }
            }

        }
    }

    List<ChatMsgEntity> entities = new ArrayList<>();
    ChatMessageAdapter mAdapter;
    private void initView(View view) {
        mListView = view.findViewById(R.id.side_delete_listview);
        mAdapter = new ChatMessageAdapter(getContext(), entities);
        mListView.setAdapter(mAdapter);
    }

    private void initListener() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UiUtils.startChatDetailActivity(getContext(), entities.get(position).getFriendCodes());
            }
        });
    }
}
