package com.bukhmastov.cdoitmo.object;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.function.Callable;

public interface ProtocolTracker {

    ProtocolTracker check(@NonNull Context context);

    ProtocolTracker check(@NonNull Context context, @Nullable Callable callback);

    ProtocolTracker restart(@NonNull Context context);

    ProtocolTracker restart(@NonNull Context context, @Nullable Callable callback);

    ProtocolTracker start(@NonNull Context context);

    ProtocolTracker start(@NonNull Context context, @Nullable Callable callback);

    ProtocolTracker stop(@NonNull Context context);

    ProtocolTracker stop(@NonNull Context context, @Nullable Callable callback);

    ProtocolTracker reset(@NonNull Context context);

    ProtocolTracker reset(@NonNull Context context, @Nullable Callable callback);

    void setup(@NonNull Context context);
}
