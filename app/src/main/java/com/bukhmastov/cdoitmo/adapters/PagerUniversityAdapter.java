package com.bukhmastov.cdoitmo.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragments.UniversityBuildingsFragment;
import com.bukhmastov.cdoitmo.fragments.UniversityEventsFragment;
import com.bukhmastov.cdoitmo.fragments.UniversityFacultiesFragment;
import com.bukhmastov.cdoitmo.fragments.UniversityNewsFragment;
import com.bukhmastov.cdoitmo.fragments.UniversityPersonsFragment;
import com.bukhmastov.cdoitmo.fragments.UniversityUnitsFragment;
import com.bukhmastov.cdoitmo.utils.Static;

import java.util.ArrayList;

public class PagerUniversityAdapter extends FragmentStatePagerAdapter {

    private class Element {
        public int id;
        public String title;
        public Class fragment;
        Element(int id, String title, Class fragment){
            this.id = id;
            this.title = title;
            this.fragment = fragment;
        }
    }
    private ArrayList<Element> tabs = new ArrayList<>();

    public PagerUniversityAdapter(FragmentManager fm, Context context) {
        super(fm);
        tabs.add(new Element(0, context.getString(R.string.persons), UniversityPersonsFragment.class));
        tabs.add(new Element(1, context.getString(R.string.faculties), UniversityFacultiesFragment.class));
        tabs.add(new Element(2, context.getString(R.string.units), UniversityUnitsFragment.class));
        tabs.add(new Element(3, context.getString(R.string.buildings), UniversityBuildingsFragment.class));
        tabs.add(new Element(4, context.getString(R.string.news), UniversityNewsFragment.class));
        tabs.add(new Element(5, context.getString(R.string.events), UniversityEventsFragment.class));
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        for (Element element : tabs) {
            if (element.id == position) {
                try {
                    fragment = (Fragment) element.fragment.newInstance();
                } catch (Exception e) {
                    Static.error(e);
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
