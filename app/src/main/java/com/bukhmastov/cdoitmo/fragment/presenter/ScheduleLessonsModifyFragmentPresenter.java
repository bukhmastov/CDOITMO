package com.bukhmastov.cdoitmo.fragment.presenter;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface ScheduleLessonsModifyFragmentPresenter {

    void setFragment(ConnectedFragment fragment);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();

    void onResume();

    void onPause();

    void onViewCreated();

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({CREATE, EDIT})
    @interface TYPE {}
    String CREATE = "create";
    String EDIT = "edit";
}
