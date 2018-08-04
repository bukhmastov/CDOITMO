package com.bukhmastov.cdoitmo.fragment.presenter;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.View;

public interface UniversityEventsFragmentPresenter {

    void setFragment(Fragment fragment);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();

    void onResume();

    void onPause();

    void onCreateView(View container);
}
