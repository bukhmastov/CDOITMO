package com.bukhmastov.cdoitmo.activity.presenter;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.activity.ShortcutReceiverActivity;

public interface ShortcutReceiverActivityPresenter {

    void setActivity(@NonNull ShortcutReceiverActivity activity);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();
}
