package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsShareFragmentPresenter;

import javax.inject.Inject;

public class ScheduleLessonsShareFragment extends ConnectedFragment<ScheduleLessonsShareFragmentPresenter> {

    @Inject
    ScheduleLessonsShareFragmentPresenter presenter;

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
    protected ScheduleLessonsShareFragmentPresenter getPresenter() {
        return presenter;
    }

    @Override
    protected int getLayoutId() {
        return "handle".equals(presenter.getAction()) ? R.layout.fragment_schedule_lessons_share_receive : R.layout.fragment_schedule_lessons_share_send;
    }

    @Override
    protected int getRootId() {
        return 0;
    }

    @Override
    protected String getLogTag() {
        return "ScheduleLessonsShareFragment";
    }
}
