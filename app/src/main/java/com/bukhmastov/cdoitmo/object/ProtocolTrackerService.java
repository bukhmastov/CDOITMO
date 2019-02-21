package com.bukhmastov.cdoitmo.object;

import android.content.Context;
import androidx.annotation.NonNull;

import com.bukhmastov.cdoitmo.function.Callable;

public interface ProtocolTrackerService {

    void request(@NonNull Context context, Callable callback);

    void shutdown();
}
