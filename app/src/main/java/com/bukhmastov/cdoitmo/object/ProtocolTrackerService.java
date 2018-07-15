package com.bukhmastov.cdoitmo.object;

import android.content.Context;
import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.interfaces.Callable;
import com.bukhmastov.cdoitmo.object.impl.ProtocolTrackerServiceImpl;

public interface ProtocolTrackerService {

    // future: replace with DI factory
    ProtocolTrackerService instance = new ProtocolTrackerServiceImpl();
    static ProtocolTrackerService instance() {
        return instance;
    }

    void request(@NonNull final Context context, Callable callback);

    void shutdown();
}
