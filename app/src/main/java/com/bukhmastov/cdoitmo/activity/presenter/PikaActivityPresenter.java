package com.bukhmastov.cdoitmo.activity.presenter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.activity.PikaActivity;

public interface PikaActivityPresenter {

    void setActivity(@NonNull PikaActivity activity);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();
}
