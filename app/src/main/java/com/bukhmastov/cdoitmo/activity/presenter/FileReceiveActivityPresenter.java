package com.bukhmastov.cdoitmo.activity.presenter;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.MenuItem;

import com.bukhmastov.cdoitmo.activity.FileReceiveActivity;

public interface FileReceiveActivityPresenter {

    void setActivity(@NonNull FileReceiveActivity activity);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();

    boolean onToolbarSelected(MenuItem item);

    boolean onBackPressed();
}
