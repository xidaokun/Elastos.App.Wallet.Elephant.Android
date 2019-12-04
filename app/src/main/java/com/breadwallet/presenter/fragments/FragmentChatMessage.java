package com.breadwallet.presenter.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.breadwallet.R;

public class FragmentChatMessage extends BaseFragmentChat {

    private static final String TAG = FragmentChatMessage.class.getSimpleName() + "_log";

    public static FragmentChatMessage newInstance(String title) {
        FragmentChatMessage f = new FragmentChatMessage();
        f.setTitle(title);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chat_message, container, false);
        return rootView;
    }
}
