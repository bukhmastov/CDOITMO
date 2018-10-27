package com.bukhmastov.cdoitmo.activity.presenter;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.activity.LoginActivity;

public interface LoginActivityPresenter {

    void setActivity(@NonNull LoginActivity activity);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();

    void onToolbarSetup();
}
