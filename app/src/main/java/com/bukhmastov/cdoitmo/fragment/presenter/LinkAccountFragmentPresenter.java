package com.bukhmastov.cdoitmo.fragment.presenter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.StringDef;

public interface LinkAccountFragmentPresenter extends ConnectedFragmentPresenter {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ISU})
    @interface Type {}
    String ISU = "isu";
}
