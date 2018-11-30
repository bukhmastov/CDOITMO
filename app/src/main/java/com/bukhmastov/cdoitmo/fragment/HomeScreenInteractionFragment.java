package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.HomeScreenInteractionFragmentPresenter;

import javax.inject.Inject;

public class HomeScreenInteractionFragment extends ConnectedFragment<HomeScreenInteractionFragmentPresenter> {

    @Inject
    HomeScreenInteractionFragmentPresenter presenter;

    @Override
    public void onAttach(Context context) {
        AppComponentProvider.getComponent().inject(this);
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        presenter = null;
        super.onDetach();
    }

    @Override
    protected HomeScreenInteractionFragmentPresenter getPresenter() {
        return presenter;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_homescreen_interaction;
    }

    @Override
    protected int getRootId() {
        return 0;
    }
}
