package com.bukhmastov.cdoitmo.fragment.presenter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;

import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;

public interface ERegisterSubjectFragmentPresenter {

    void setFragment(ConnectedFragment fragment);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();

    void onToolbarSetup(Menu menu);

    void onToolbarTeardown(Menu menu);

    void onResume();

    void onPause();

    void onViewCreated();
}
