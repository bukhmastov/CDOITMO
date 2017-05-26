package com.bukhmastov.cdoitmo.adapters;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsTabFragment;

public class PagerAdapter extends FragmentStatePagerAdapter {
    private Context context;
    public static ConnectedActivity activity;
    public PagerAdapter(FragmentManager fm, Context context, ConnectedActivity activity) {
        super(fm);
        this.context = context;
        PagerAdapter.activity = activity;
    }
    @Override
    public Fragment getItem(int position) {
        Bundle bundle = new Bundle();
        switch (position){
            case 1: bundle.putInt("type", 0); break;
            case 2: bundle.putInt("type", 1); break;
            default: case 0: bundle.putInt("type", 2); break;
        }
        ScheduleLessonsTabFragment fragment = new ScheduleLessonsTabFragment();
        fragment.setArguments(bundle);
        return fragment;
    }
    @Override
    public int getCount() {
        return 3;
    }
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position){
            case 1: return context.getString(R.string.tab_even);
            case 2: return context.getString(R.string.tab_odd);
            default: case 0: return context.getString(R.string.tab_all);
        }
    }
}