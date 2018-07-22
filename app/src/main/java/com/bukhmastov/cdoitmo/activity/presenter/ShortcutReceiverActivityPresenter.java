package com.bukhmastov.cdoitmo.activity.presenter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bukhmastov.cdoitmo.activity.ShortcutReceiverActivity;

public interface ShortcutReceiverActivityPresenter {

    void setActivity(@NonNull ShortcutReceiverActivity activity);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();
}
