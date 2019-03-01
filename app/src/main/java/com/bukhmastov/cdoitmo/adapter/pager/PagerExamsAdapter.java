package com.bukhmastov.cdoitmo.adapter.pager;

import android.content.Context;
import android.os.Bundle;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.ScheduleExamsTabFragment;
import com.bukhmastov.cdoitmo.util.Log;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import dagger.Lazy;

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
    private final List<Element> tabs = new LinkedList<>();

    @Inject
    Lazy<Log> log;

    public PagerExamsAdapter(FragmentManager fm, Context context) {
        super(fm);
        AppComponentProvider.getComponent().inject(this);
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
            log.get().exception(e);
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
