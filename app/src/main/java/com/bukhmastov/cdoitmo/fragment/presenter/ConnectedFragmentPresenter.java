package com.bukhmastov.cdoitmo.fragment.presenter;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;

public interface ConnectedFragmentPresenter {

    void setFragment(ConnectedFragment fragment);

    void onCreate(@Nullable Bundle savedInstanceState);

    default void onViewCreated() {}

    void onStart();

    default void onToolbarSetup(Menu menu) {}

    void onResume();

    void onPause();

    default void onToolbarTeardown(Menu menu) {
        try {
            if (menu == null) {
                return;
            }
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item == null || item.getItemId() == R.id.offline_mode) {
                    continue;
                }
                item.setVisible(false);
            }
        } catch (Throwable ignore) {
            // ignore
        }
    }

    void onStop();

    void onDestroy();
}
