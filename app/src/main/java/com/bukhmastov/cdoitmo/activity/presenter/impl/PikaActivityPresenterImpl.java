package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.PikaActivity;
import com.bukhmastov.cdoitmo.activity.presenter.PikaActivityPresenter;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.Thread;

import java.util.Random;

import javax.inject.Inject;

public class PikaActivityPresenterImpl implements PikaActivityPresenter {

    private static final String TAG = "PikaActivity";
    private PikaActivity activity = null;
    private final Random random = new Random();
    private boolean dimas = false;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    StoragePref storagePref;
    @Inject
    TextUtils textUtils;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public PikaActivityPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setActivity(@NonNull PikaActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.runOnUI(() -> {
            log.v(TAG, "PIKA is no longer hiding");
            firebaseAnalyticsProvider.logCurrentScreen(activity);
            activity.overridePendingTransition(R.anim.zoom_bottom_in, R.anim.zoom_bottom_out);
            if (random.nextInt(6) % 6 == 0) {
                log.v(TAG, "LEGENDARY D1MA$ APPEARS, so pika went away");
                dimas = true;
                ((ImageView) activity.findViewById(R.id.image)).setImageDrawable(activity.getDrawable(R.drawable.wuwari));
            }
            View pika_container = activity.findViewById(R.id.pika_container);
            if (pika_container != null) {
                pika_container.setOnClickListener(v -> {
                    activity.finish();
                    activity.overridePendingTransition(R.anim.zoom_bottom_in, R.anim.zoom_bottom_out);
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        thread.runOnUI(() -> {
            if (dimas) {
                log.v(TAG, "D1MA$ left us to not to be late for some movies");
            } else {
                log.v(TAG, "PIKA left us to stream some games");
            }
        });
    }
}
