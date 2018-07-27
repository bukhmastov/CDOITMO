package com.bukhmastov.cdoitmo.fragment.presenter;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;

public interface Room101FragmentPresenter {

    void setFragment(ConnectedFragment fragment);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();

    void onResume();

    void onPause();

    void execute(final Context context, final String scope, final ResponseHandler responseHandler);
}
