package com.bukhmastov.cdoitmo.fragment.presenter;

import android.os.Bundle;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;

public interface ERegisterFragmentPresenter {

    void setFragment(ConnectedFragment fragment);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();

    void onResume();

    void onPause();
}
