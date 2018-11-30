package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.ERegisterFragmentPresenter;

import javax.inject.Inject;

public class ERegisterFragment extends ConnectedFragment<ERegisterFragmentPresenter> {

    @Inject
    ERegisterFragmentPresenter presenter;

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
    protected ERegisterFragmentPresenter getPresenter() {
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

}
