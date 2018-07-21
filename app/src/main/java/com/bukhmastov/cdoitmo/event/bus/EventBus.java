package com.bukhmastov.cdoitmo.event.bus;

import android.support.annotation.NonNull;

public interface EventBus {

    void register(@NonNull Object object);

    void unregister(@NonNull Object object);

    void fire(@NonNull Object event);

    void fire(@NonNull String tag, @NonNull Object event);
}
