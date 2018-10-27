package com.bukhmastov.cdoitmo.activity.presenter;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.activity.TimeRemainingWidgetActivity;

public interface TimeRemainingWidgetActivityPresenter {

    void setActivity(@NonNull TimeRemainingWidgetActivity activity);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onResume();

    void onPause();

    void onDestroy();
}
