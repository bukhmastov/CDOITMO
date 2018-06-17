package com.bukhmastov.cdoitmo.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragment.UniversityBuildingsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityEventsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityFacultiesFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityNewsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityPersonsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityUnitsFragment;
import com.bukhmastov.cdoitmo.util.Log;

import java.util.ArrayList;

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
    private final ArrayList<Element> tabs = new ArrayList<>();

    public PagerUniversityAdapter(FragmentManager fm, Context context) {
        super(fm);
        tabs.add(new Element(0, context.getString(R.string.persons), UniversityPersonsFragment.class));
        tabs.add(new Element(1, context.getString(R.string.faculties), UniversityFacultiesFragment.class));
        tabs.add(new Element(2, context.getString(R.string.units), UniversityUnitsFragment.class));
        tabs.add(new Element(3, context.getString(R.string.news), UniversityNewsFragment.class));
        tabs.add(new Element(4, context.getString(R.string.events), UniversityEventsFragment.class));
        tabs.add(new Element(5, context.getString(R.string.ubuildings), UniversityBuildingsFragment.class));
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        for (Element element : tabs) {
            if (element.id == position) {
                try {
                    fragment = (Fragment) element.fragment.newInstance();
                } catch (Exception e) {
                    Log.exception(e);
                }
                break;
            }
        }
        return fragment;
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
