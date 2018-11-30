package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsModifyFragmentPresenter;

import javax.inject.Inject;

public class ScheduleLessonsModifyFragment extends ConnectedFragment<ScheduleLessonsModifyFragmentPresenter> {

    @Inject
    ScheduleLessonsModifyFragmentPresenter presenter;

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
    protected ScheduleLessonsModifyFragmentPresenter getPresenter() {
        return presenter;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_schedule_lessons_modify;
    }

    @Override
    protected int getRootId() {
        return 0;
    }
}
