package com.bukhmastov.cdoitmo.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapters.PagerUniversityAdapter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Storage;

public class UniversityFragment extends ConnectedFragment implements ViewPager.OnPageChangeListener {

    private static final String TAG = "UniversityFragment";
    private static int tabSelected = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
        activity.findViewById(R.id.scrollable_tabs).setVisibility(View.GONE);
        Storage.pref.put(getContext(), "pref_university_tab", tabSelected);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_university, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TabLayout main_tabs = (TabLayout) activity.findViewById(R.id.scrollable_tabs);
        ViewPager university_pager = (ViewPager) activity.findViewById(R.id.university_pager);
        if (university_pager != null) {
            university_pager.setAdapter(new PagerUniversityAdapter(getFragmentManager(), getContext(), activity));
            university_pager.addOnPageChangeListener(this);
            main_tabs.setupWithViewPager(university_pager);
        }
        TabLayout.Tab tab;
        try {
            if (tabSelected == -1) {
                int pref = Integer.parseInt(Storage.pref.get(getContext(), "pref_university_tab", "-1"));
                tab = main_tabs.getTabAt(pref < 0 ? 0 : pref);
            } else {
                tab = main_tabs.getTabAt(tabSelected);
            }
        } catch (Exception e) {
            tab = null;
        }
        if (tab != null) {
            tab.select();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
        activity.findViewById(R.id.scrollable_tabs).setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
    }

    @Override
    public void onPageSelected(int position) {
        UniversityFragment.tabSelected = position;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageScrollStateChanged(int state) {}

}
