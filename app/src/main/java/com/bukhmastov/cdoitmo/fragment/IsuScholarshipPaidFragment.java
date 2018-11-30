package com.bukhmastov.cdoitmo.fragment;

import android.os.Bundle;
import android.view.Menu;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.IsuScholarshipPaidFragmentPresenter;

import javax.inject.Inject;

public class IsuScholarshipPaidFragment extends ConnectedFragment {

    @Inject
    IsuScholarshipPaidFragmentPresenter presenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        presenter.setFragment(this);
        super.onCreate(savedInstanceState);
        presenter.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        presenter.onToolbarSetup(activity.toolbar);
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        presenter.onToolbarTeardown(activity.toolbar);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        presenter.onToolbarSetup(menu);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onViewCreated() {
        presenter.onViewCreated();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_container;
    }

    @Override
    protected int getRootId() {
        return R.id.container;
    }
}