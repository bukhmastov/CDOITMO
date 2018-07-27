package com.bukhmastov.cdoitmo.fragment.presenter.impl;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.adapter.PagerUniversityAdapter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityFragmentPresenter;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;

import javax.inject.Inject;

public class UniversityFragmentPresenterImpl implements UniversityFragmentPresenter, ViewPager.OnPageChangeListener {

    private static final String TAG = "UniversityFragment";
    private ConnectedFragment fragment = null;
    private ConnectedActivity activity = null;
    private int tabSelected = -1;
    private String action_extra = null;

    @Inject
    Log log;
    @Inject
    StoragePref storagePref;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public UniversityFragmentPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setFragment(ConnectedFragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.activity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        log.v(TAG, "Fragment created");
        firebaseAnalyticsProvider.logCurrentScreen(activity, fragment);
        Intent intent = activity.getIntent();
        if (intent != null) {
            action_extra = intent.getStringExtra("action_extra");
            if (action_extra != null) {
                intent.removeExtra("action_extra");
            }
        }
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
        TabLayout scrollable_tabs = activity.findViewById(R.id.scrollable_tabs);
        if (scrollable_tabs != null) {
            scrollable_tabs.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        log.v(TAG, "Fragment resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, fragment);
        TabLayout scrollable_tabs = activity.findViewById(R.id.scrollable_tabs);
        if (scrollable_tabs != null) {
            scrollable_tabs.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPause() {
        log.v(TAG, "Fragment paused");
    }

    @Override
    public void onViewCreated() {
        TabLayout main_tabs = activity.findViewById(R.id.scrollable_tabs);
        ViewPager pager = fragment.container().findViewById(R.id.pager);
        FragmentManager fragmentManager = fragment.getChildFragmentManager();
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
                    int pref = storagePref.get(activity, "pref_university_tab", -1);
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
    public void onPageSelected(int position) {
        tabSelected = position;
        storagePref.put(activity, "pref_university_tab", tabSelected);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageScrollStateChanged(int state) {}
}
