package com.bukhmastov.cdoitmo.activity.presenter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.activity.UniversityPersonCardActivity;

public interface UniversityPersonCardActivityPresenter {

    void setActivity(@NonNull UniversityPersonCardActivity activity);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onResume();

    void onPause();

    void onDestroy();
}