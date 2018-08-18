package com.bukhmastov.cdoitmo.activity.presenter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.activity.LoginActivity;

public interface LoginActivityPresenter {

    void setActivity(@NonNull LoginActivity activity);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();

    void onToolbarSetup();
}
