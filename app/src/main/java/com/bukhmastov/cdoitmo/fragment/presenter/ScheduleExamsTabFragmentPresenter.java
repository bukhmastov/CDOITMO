package com.bukhmastov.cdoitmo.fragment.presenter;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.View;

import com.bukhmastov.cdoitmo.activity.ConnectedActivity;

public interface ScheduleExamsTabFragmentPresenter {

    void onCreate(Bundle savedInstanceState, ConnectedActivity activity, Fragment fragment);

    void onDestroy();

    void onCreateView(View container);

    void onDestroyView();

    void onResume();

    void onPause();
}
