package com.bukhmastov.cdoitmo.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.View;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.ERegisterSubjectFragmentPresenter;

import javax.inject.Inject;

public class ERegisterSubjectFragment extends ConnectedFragment {

    @Inject
    ERegisterSubjectFragmentPresenter presenter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        presenter.setFragment(this);
        super.onCreate(savedInstanceState);
        presenter.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter.onViewCreated();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.onResume();
        presenter.onToolbarSetup(activity.toolbar);
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.onPause();
        presenter.onToolbarTeardown(activity.toolbar);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        presenter.onToolbarSetup(menu);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_subject_show;
    }

    @Override
    protected int getRootId() {
        return 0;
    }
}
