package com.bukhmastov.cdoitmo.fragment.presenter;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.Menu;

import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;

public interface ConnectedFragmentPresenter {

    void setFragment(ConnectedFragment fragment);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();

    default void onToolbarSetup(Menu menu) {}

    default void onToolbarTeardown(Menu menu) {}

    void onResume();

    void onPause();

    default void onViewCreated() {}
}
