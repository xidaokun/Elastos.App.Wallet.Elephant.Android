package org.chat.lib.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import org.chat.lib.presenter.BaseFragment;

import java.util.List;

public class ChatPagerAdapter extends FragmentPagerAdapter {

    private List<BaseFragment> mFragments;
    private String[] mTitles = new String[]{"Messages", "Friends"};

    public ChatPagerAdapter(FragmentManager fm, List<BaseFragment> fragments) {
        super(fm);
        this.mFragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return (this.mFragments!=null) ? this.mFragments.get(position):null;
    }

    @Override
    public int getCount() {
        return (this.mFragments!=null) ? this.mFragments.size():0;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return (this.mFragments!=null) ? mFragments.get(position).getTitle(): "";
    }

}
