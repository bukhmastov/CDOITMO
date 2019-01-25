package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.ERegisterSubjectFragmentPresenter;

import javax.inject.Inject;

public class ERegisterSubjectFragment extends ConnectedFragment<ERegisterSubjectFragmentPresenter> {

    @Inject
    ERegisterSubjectFragmentPresenter presenter;

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
    protected ERegisterSubjectFragmentPresenter getPresenter() {
        return presenter;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_subject_show;
    }

    @Override
    protected int getRootId() {
        return 0;
    }

    @Override
    protected String getLogTag() {
        return "ERegisterSubjectFragment";
    }
}
