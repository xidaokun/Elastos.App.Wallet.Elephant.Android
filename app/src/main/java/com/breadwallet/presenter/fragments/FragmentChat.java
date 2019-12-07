package com.breadwallet.presenter.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.breadwallet.R;
import com.breadwallet.tools.adapter.ChatPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class FragmentChat extends Fragment {
    private static final String TAG = FragmentChat.class.getSimpleName() + "_log";

    private ViewPager mViewPager;
    private TabLayout mTabLayout;

    public static FragmentChat newInstance(String text) {
        FragmentChat f = new FragmentChat();
        Bundle b = new Bundle();
        b.putString("text", text);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
        initView(rootView);
        return rootView;
    }

    private void initView(View view) {
        mTabLayout = view.findViewById(R.id.tab_layout);
        mViewPager = view.findViewById(R.id.viewpager);
        List<BaseFragmentChat> fragments = new ArrayList<>();
        fragments.add(FragmentChatMessage.newInstance(getContext().getString(R.string.My_chat_tab_message_title)));
        fragments.add(FragmentChatFriends.newInstance(getContext().getString(R.string.My_chat_tab_friends_title)));
        mViewPager.setAdapter(new ChatPagerAdapter(getActivity().getSupportFragmentManager(), fragments));
        mTabLayout.setupWithViewPager(mViewPager);
    }


}
