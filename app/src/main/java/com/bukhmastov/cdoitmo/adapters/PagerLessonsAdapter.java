package com.bukhmastov.cdoitmo.adapters;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragments.ScheduleLessonsTabFragment;
import com.bukhmastov.cdoitmo.utils.Static;

import java.util.ArrayList;

public class PagerLessonsAdapter extends FragmentStatePagerAdapter {

    private class Element {
        public int id;
        public String title;
        public int type;
        Element(int id, String title, int type){
            this.id = id;
            this.title = title;
            this.type = type;
        }
    }
    private ArrayList<Element> tabs = new ArrayList<>();

    public PagerLessonsAdapter(FragmentManager fm, Context context) {
        super(fm);
        tabs.add(new Element(0, context.getString(R.string.tab_all), 2));
        tabs.add(new Element(1, context.getString(R.string.tab_even), 0));
        tabs.add(new Element(2, context.getString(R.string.tab_odd), 1));
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
            Fragment fragment = ScheduleLessonsTabFragment.class.newInstance();
            Bundle bundle = new Bundle();
            bundle.putInt("type", type);
            fragment.setArguments(bundle);
            return fragment;
        } catch (Exception e) {
            Static.error(e);
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
