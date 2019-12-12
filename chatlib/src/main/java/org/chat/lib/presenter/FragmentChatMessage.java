package org.chat.lib.presenter;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.chat.lib.R;
import org.chat.lib.adapter.ChatMessageAdapter;
import org.chat.lib.entity.ChatMsgEntity;

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

    private void initView(View view) {
        mListView = view.findViewById(R.id.side_delete_listview);
        List<ChatMsgEntity> entities = new ArrayList<>();
        ChatMsgEntity entity1 = new ChatMsgEntity();
        entity1.setName("Chris");
        entity1.setMessage("How are you..........");
        entity1.setTimeStamp("2:10");
        entity1.setCount("999");
        entities.add(entity1);

        ChatMsgEntity entity2 = new ChatMsgEntity();
        entity2.setName("Chris1");
        entity2.setMessage("How are you..........");
        entity2.setTimeStamp("2:10");
        entity2.setCount("9");
        entities.add(entity2);

        ChatMsgEntity entity3 = new ChatMsgEntity();
        entity3.setName("Chris2");
        entity3.setMessage("How are you..........");
        entity3.setTimeStamp("2:10");
        entity3.setCount("9");
        entities.add(entity3);

        ChatMsgEntity entity4 = new ChatMsgEntity();
        entity4.setName("Chris3");
        entity4.setMessage("How are you..........");
        entity4.setTimeStamp("2:10");
        entity4.setCount("9");
        entities.add(entity4);

        ChatMsgEntity entity5 = new ChatMsgEntity();
        entity5.setName("Chris5");
        entity5.setMessage("How are you..........");
        entity5.setTimeStamp("2:10");
        entity5.setCount("666");
        entities.add(entity5);

        ChatMsgEntity entity6 = new ChatMsgEntity();
        entity6.setName("Chris6");
        entity6.setMessage("How are you..........");
        entity6.setTimeStamp("2:10");
        entity6.setCount("9");
        entities.add(entity6);

        ChatMsgEntity entity7 = new ChatMsgEntity();
        entity7.setName("Chris7");
        entity7.setMessage("How are you..........");
        entity7.setTimeStamp("2:10");
        entity7.setCount("9");
        entities.add(entity7);

        ChatMessageAdapter adapter = new ChatMessageAdapter(getContext(), entities);
        mListView.setAdapter(adapter);
    }

    private void initListener() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getContext(), ChatDetailActivity.class);
                getContext().startActivity(intent);
            }
        });
    }
}
