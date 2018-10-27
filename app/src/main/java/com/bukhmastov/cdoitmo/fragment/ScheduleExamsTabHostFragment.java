package com.bukhmastov.cdoitmo.fragment;

import android.content.Context;
import androidx.fragment.app.Fragment;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleExamsTabHostFragmentPresenter;

import javax.inject.Inject;

public abstract class ScheduleExamsTabHostFragment extends Fragment {

    @Inject
    ScheduleExamsTabHostFragmentPresenter presenter;

    @Override
    public void onAttach(Context context) {
        AppComponentProvider.getComponent().inject(this);
        super.onAttach(context);
        presenter.onAttach((ConnectedActivity) getActivity());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        presenter.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }
}
