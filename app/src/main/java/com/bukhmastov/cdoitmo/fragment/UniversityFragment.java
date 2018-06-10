package com.bukhmastov.cdoitmo.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.adapter.PagerUniversityAdapter;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;

public class UniversityFragment extends ConnectedFragment implements ViewPager.OnPageChangeListener {

    private static final String TAG = "UniversityFragment";
    private static int tabSelected = -1;
    private String action_extra = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
        Activity activity = getActivity();
        if (activity != null) {
            Intent intent = activity.getIntent();
            if (intent != null) {
                action_extra = intent.getStringExtra("action_extra");
                if (action_extra != null) {
                    intent.removeExtra("action_extra");
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
        TabLayout scrollable_tabs = activity.findViewById(R.id.scrollable_tabs);
        if (scrollable_tabs != null) {
            scrollable_tabs.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TabLayout main_tabs = activity.findViewById(R.id.scrollable_tabs);
        ViewPager pager = activity.findViewById(R.id.pager);
        FragmentManager fragmentManager = getChildFragmentManager();
        if (pager != null) {
            pager.setAdapter(new PagerUniversityAdapter(fragmentManager, activity));
            pager.addOnPageChangeListener(this);
            main_tabs.setupWithViewPager(pager);
        }
        TabLayout.Tab tab = null;
        try {
            if (action_extra != null) {
                switch (action_extra) {
                    case "persons": tab = main_tabs.getTabAt(0); break;
                    case "faculties": tab = main_tabs.getTabAt(1); break;
                    case "units": tab = main_tabs.getTabAt(2); break;
                    case "ubuildings": tab = main_tabs.getTabAt(3); break;
                    case "news": tab = main_tabs.getTabAt(4); break;
                    case "events": tab = main_tabs.getTabAt(5); break;
                }
                action_extra = null;
            }
            if (tab == null) {
                if (tabSelected == -1) {
                    int pref = Storage.pref.get(activity, "pref_university_tab", -1);
                    tab = main_tabs.getTabAt(pref < 0 ? 0 : pref);
                } else {
                    tab = main_tabs.getTabAt(tabSelected);
                }
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
        TabLayout scrollable_tabs = activity.findViewById(R.id.scrollable_tabs);
        if (scrollable_tabs != null) {
            scrollable_tabs.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
    }

    @Override
    public void onPageSelected(int position) {
        tabSelected = position;
        Storage.pref.put(activity, "pref_university_tab", tabSelected);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageScrollStateChanged(int state) {}
}
