package com.bukhmastov.cdoitmo.activity.presenter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import com.bukhmastov.cdoitmo.activity.FragmentActivity;

public interface FragmentActivityPresenter {

    void setActivity(@NonNull FragmentActivity activity);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onResume();

    void onPause();

    void onToolbarSetup();

    boolean onToolbarSelected(MenuItem item);

    boolean onBackPressed();
}
