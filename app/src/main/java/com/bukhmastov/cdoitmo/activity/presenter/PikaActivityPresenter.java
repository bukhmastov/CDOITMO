package com.bukhmastov.cdoitmo.activity.presenter;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.activity.PikaActivity;

public interface PikaActivityPresenter {

    void setActivity(@NonNull PikaActivity activity);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();
}
