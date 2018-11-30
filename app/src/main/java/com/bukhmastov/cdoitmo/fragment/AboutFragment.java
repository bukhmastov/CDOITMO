package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.AboutFragmentPresenter;

import javax.inject.Inject;

public class AboutFragment extends ConnectedFragment<AboutFragmentPresenter> {

    @Inject
    AboutFragmentPresenter presenter;

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
    protected AboutFragmentPresenter getPresenter() {
        return presenter;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_about;
    }

    @Override
    protected int getRootId() {
        return 0;
    }
}
