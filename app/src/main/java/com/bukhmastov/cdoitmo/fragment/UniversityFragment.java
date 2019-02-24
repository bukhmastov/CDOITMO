package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityFragmentPresenter;

import javax.inject.Inject;

public class UniversityFragment extends ConnectedFragment<UniversityFragmentPresenter> {

    @Inject
    UniversityFragmentPresenter presenter;

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
    protected UniversityFragmentPresenter getPresenter() {
        return presenter;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_pager;
    }

    @Override
    protected int getRootId() {
        return 0;
    }

    @Override
    protected String getLogTag() {
        return "UniversityFragment";
    }
}
