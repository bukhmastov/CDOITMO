package com.bukhmastov.cdoitmo.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.fragments.UniversityPersonsFragment;
import com.bukhmastov.cdoitmo.utils.Static;

import java.util.ArrayList;

public class PagerUniversityAdapter extends FragmentStatePagerAdapter {

    private Context context;
    private ConnectedActivity activity;

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

    public PagerUniversityAdapter(FragmentManager fm, Context context, ConnectedActivity activity) {
        super(fm);
        this.context = context;
        this.activity = activity;
        tabs.add(new Element(0, "Персоналии", UniversityPersonsFragment.class));
        //tabs.add(new Element(1, "Факультеты", UniversityPersonsFragment.class));
        //tabs.add(new Element(2, "Подразделения", UniversityPersonsFragment.class));
        //tabs.add(new Element(3, "Корпуса", UniversityPersonsFragment.class));
        //tabs.add(new Element(4, "Новости", UniversityPersonsFragment.class));
        //tabs.add(new Element(5, "События", UniversityPersonsFragment.class));
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
