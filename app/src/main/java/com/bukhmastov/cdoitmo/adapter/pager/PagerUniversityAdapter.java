package com.bukhmastov.cdoitmo.adapter.pager;

import android.content.Context;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.UniversityBuildingsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityEventsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityFacultiesFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityNewsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityPersonsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityUnitsFragment;
import com.bukhmastov.cdoitmo.util.Log;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import dagger.Lazy;

public class PagerUniversityAdapter extends FragmentStatePagerAdapter {

    private class Element {
        public final int id;
        public final String title;
        public final Class fragment;
        Element(int id, String title, Class fragment){
            this.id = id;
            this.title = title;
            this.fragment = fragment;
        }
    }
    private final List<Element> tabs = new LinkedList<>();

    @Inject
    Lazy<Log> log;

    public PagerUniversityAdapter(FragmentManager fm, Context context) {
        super(fm);
        AppComponentProvider.getComponent().inject(this);
        tabs.add(new Element(0, context.getString(R.string.persons), UniversityPersonsFragment.class));
        tabs.add(new Element(1, context.getString(R.string.faculties), UniversityFacultiesFragment.class));
        tabs.add(new Element(2, context.getString(R.string.units), UniversityUnitsFragment.class));
        tabs.add(new Element(3, context.getString(R.string.news), UniversityNewsFragment.class));
        tabs.add(new Element(4, context.getString(R.string.events), UniversityEventsFragment.class));
        tabs.add(new Element(5, context.getString(R.string.ubuildings), UniversityBuildingsFragment.class));
    }

    @Override
    public Fragment getItem(int position) {
        try {
            for (Element element : tabs) {
                if (element.id == position) {
                    return (Fragment) element.fragment.newInstance();
                }
            }
            return null;
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
