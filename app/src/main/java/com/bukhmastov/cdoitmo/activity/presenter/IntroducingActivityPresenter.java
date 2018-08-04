package com.bukhmastov.cdoitmo.activity.presenter;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.activity.IntroducingActivity;

public interface IntroducingActivityPresenter {

    void setActivity(@NonNull IntroducingActivity activity);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();
}
