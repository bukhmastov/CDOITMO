package com.bukhmastov.cdoitmo.object;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.interfaces.Callable;
import com.bukhmastov.cdoitmo.network.DeIfmoRestClient;
import com.bukhmastov.cdoitmo.object.impl.ProtocolTrackerImpl;

public interface ProtocolTracker {

    // future: replace with DI factory
    ProtocolTracker instance = new ProtocolTrackerImpl();
    static ProtocolTracker instance() {
        return instance;
    }

    ProtocolTracker check(@NonNull final Context context);

    ProtocolTracker check(@NonNull final Context context, @Nullable final Callable callback);

    ProtocolTracker restart(@NonNull final Context context);

    ProtocolTracker restart(@NonNull final Context context, @Nullable final Callable callback);

    ProtocolTracker start(@NonNull final Context context);

    ProtocolTracker start(@NonNull final Context context, @Nullable final Callable callback);

    ProtocolTracker stop(@NonNull final Context context);

    ProtocolTracker stop(@NonNull final Context context, @Nullable final Callable callback);

    ProtocolTracker reset(@NonNull final Context context);

    ProtocolTracker reset(@NonNull final Context context, @Nullable final Callable callback);

    void setup(@NonNull final Context context, @NonNull final DeIfmoRestClient deIfmoRestClient, final int attempt);
}
