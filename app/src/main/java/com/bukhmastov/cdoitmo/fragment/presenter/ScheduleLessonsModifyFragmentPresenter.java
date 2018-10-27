package com.bukhmastov.cdoitmo.fragment.presenter;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface ScheduleLessonsModifyFragmentPresenter extends ConnectedFragmentPresenter {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({CREATE, EDIT})
    @interface TYPE {}
    String CREATE = "create";
    String EDIT = "edit";
}
