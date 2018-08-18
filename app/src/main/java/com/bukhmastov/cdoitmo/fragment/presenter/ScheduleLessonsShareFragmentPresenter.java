package com.bukhmastov.cdoitmo.fragment.presenter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface ScheduleLessonsShareFragmentPresenter {

    void setFragment(ConnectedFragment fragment);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();

    void onResume();

    void onPause();

    String getAction();

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ADDED, REDUCED})
    @interface TYPE {}
    String ADDED = "added";
    String REDUCED = "reduced";
}
