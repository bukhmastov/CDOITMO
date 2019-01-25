package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.IsuScholarshipAssignedFragmentPresenter;

import javax.inject.Inject;

public class IsuScholarshipAssignedFragment extends ConnectedFragment<IsuScholarshipAssignedFragmentPresenter> {

    @Inject
    IsuScholarshipAssignedFragmentPresenter presenter;

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
    protected IsuScholarshipAssignedFragmentPresenter getPresenter() {
        return presenter;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_container;
    }

    @Override
    protected int getRootId() {
        return R.id.container;
    }

    @Override
    protected String getLogTag() {
        return "IsuScholarshipAssignedFragment";
    }
}
