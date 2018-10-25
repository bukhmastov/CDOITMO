package com.bukhmastov.cdoitmo.fragment;

import android.os.Bundle;
import android.view.Menu;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.ProtocolFragmentPresenter;

import javax.inject.Inject;

public class ProtocolFragment extends ConnectedFragment {

    @Inject
    ProtocolFragmentPresenter presenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
