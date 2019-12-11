package com.breadwallet.presenter.fragments;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.DividerItemDecoration;
import com.breadwallet.presenter.customviews.IndexBar;
import com.breadwallet.presenter.customviews.SuspensionDecoration;
import com.breadwallet.presenter.entities.ContactEntity;
import com.breadwallet.tools.adapter.FriendsAdapter;
import com.breadwallet.tools.animation.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class FragmentChatFriends extends BaseFragmentChat {
    private static final String TAG = FragmentChatFriends.class.getSimpleName() + "_log";

    private static final String INDEX_STRING_TOP = "\uD83D\uDD0E";

    private RecyclerView mRecyclerView;
    private IndexBar mIndexBar;
    private FriendsAdapter mAdapter;
    private List<ContactEntity> mDatas = new ArrayList<>();
    private SuspensionDecoration mDecoration;
    private LinearLayoutManager mManager;
    private TextView mSideHintTv;

    public static FragmentChatFriends newInstance(String title) {
        FragmentChatFriends f = new FragmentChatFriends();
        f.setTitle(title);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat_friends, container, false);
        initView(rootView);
        //mock data
        initDatas(getResources().getStringArray(R.array.provinces));
        initListener();
        return rootView;
    }

    private void initView(View rootView) {
        mSideHintTv = rootView.findViewById(R.id.side_bar_hint);
        mRecyclerView = rootView.findViewById(R.id.friends_rv);
        mRecyclerView.setLayoutManager(mManager = new LinearLayoutManager(getContext()));
        mIndexBar = rootView.findViewById(R.id.indexBar);
        mIndexBar.setmPressedShowTextView(mSideHintTv)
                .setNeedRealIndex(true)
                .setmLayoutManager(mManager);

        mAdapter = new FriendsAdapter(getContext(), mDatas);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(mDecoration = new SuspensionDecoration(getContext(), mDatas));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));
    }

    private void initListener() {
        mAdapter.setOnItemClickListener(new FriendsAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(View view, int position) {
                UiUtils.startMomentActivity(getContext());
            }
        });
    }

    private void initDatas(final String[] data) {
        getActivity().getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                mDatas = new ArrayList<>();
                mDatas.add((ContactEntity) new ContactEntity("新的朋友").setTop(true).setBaseIndexTag(INDEX_STRING_TOP));
                mDatas.add((ContactEntity) new ContactEntity("群聊").setTop(true).setBaseIndexTag(INDEX_STRING_TOP));
                mDatas.add((ContactEntity) new ContactEntity("标签").setTop(true).setBaseIndexTag(INDEX_STRING_TOP));
                mDatas.add((ContactEntity) new ContactEntity("公众号").setTop(true).setBaseIndexTag(INDEX_STRING_TOP));
                for (int i = 0; i < data.length; i++) {
                    ContactEntity ContactEntity = new ContactEntity();
                    ContactEntity.setContact(data[i]);
                    mDatas.add(ContactEntity);
                }
                mAdapter.setDatas(mDatas);
                mAdapter.notifyDataSetChanged();

                mIndexBar.setmSourceDatas(mDatas)
                        .invalidate();
                mDecoration.setmDatas(mDatas);
            }
        }, 500);
    }

}
