package com.bukhmastov.cdoitmo.activity.presenter;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.activity.MainActivity;

public interface MainActivityPresenter {

    void setActivity(@NonNull MainActivity activity);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onResume();

    void onPause();

    void onDestroy();

    void onSaveInstanceState(Bundle savedInstanceState);

    void onToolbarSetup();

    boolean onToolbarSelected(MenuItem item);

    boolean onBackButtonPressed();

    void authorize(int state);

    void authorized();

    void selectSection(int section);

    boolean shouldListenForDrawerSwipe();

    boolean isInitialized();
}
