package com.bukhmastov.cdoitmo.object;

import android.content.Context;
import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.interfaces.Callable;

public interface ProtocolTrackerService {

    void request(@NonNull final Context context, Callable callback);

    void shutdown();
}
