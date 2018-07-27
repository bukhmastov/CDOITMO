package com.bukhmastov.cdoitmo.fragment.presenter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

public interface UniversityNewsFragmentPresenter {

    void setFragment(Fragment fragment);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();

    void onResume();

    void onPause();

    void onCreateView(View container);
}
