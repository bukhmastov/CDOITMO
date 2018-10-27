package com.bukhmastov.cdoitmo.fragment.presenter;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface ScheduleLessonsShareFragmentPresenter extends ConnectedFragmentPresenter {

    String getAction();

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ADDED, REDUCED})
    @interface TYPE {}
    String ADDED = "added";
    String REDUCED = "reduced";
}
