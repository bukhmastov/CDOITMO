package com.bukhmastov.cdoitmo.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragment.ScheduleExamsTabFragment;
import com.bukhmastov.cdoitmo.util.Log;

import java.util.ArrayList;

public class PagerExamsAdapter extends FragmentPagerAdapter {

    private class Element {
        public final int id;
        public final String title;
        public final int type;
        Element(int id, String title, int type){
            this.id = id;
            this.title = title;
            this.type = type;
        }
    }
    private final ArrayList<Element> tabs = new ArrayList<>();

    public PagerExamsAdapter(FragmentManager fm, Context context) {
        super(fm);
        tabs.add(new Element(0, context.getString(R.string.exams), 0));
        tabs.add(new Element(1, context.getString(R.string.credits), 1));
    }

    @Override
    public Fragment getItem(int position) {
        try {
            int type = 2;
            for (Element element : tabs) {
                if (element.id == position) {
                    type = element.type;
                    break;
                }
            }
            Fragment fragment = ScheduleExamsTabFragment.class.newInstance();
            Bundle bundle = new Bundle();
            bundle.putInt("type", type);
            fragment.setArguments(bundle);
            return fragment;
        } catch (Exception e) {
            Log.exception(e);
            return null;
        }
    }

    @Override
    public int getCount() {
        return tabs.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        for (Element element : tabs) {
            if (element.id == position) {
                return element.title;
            }
        }
        return null;
    }
}
