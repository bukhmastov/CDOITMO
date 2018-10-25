package com.bukhmastov.cdoitmo.fragment.presenter;

import android.content.Context;

import com.bukhmastov.cdoitmo.network.handlers.ResponseHandler;

public interface Room101FragmentPresenter extends ConnectedFragmentPresenter {

    void execute(Context context, String scope, ResponseHandler responseHandler);
}
