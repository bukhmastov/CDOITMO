package com.bukhmastov.cdoitmo.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.util.BottomBar;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.Thread;

public class LinkedAccountsFragment extends ConnectedFragment {

    private static final String TAG = "LinkedAccountsFragment";

    //@Inject
    private Storage storage = Storage.instance();
    //@Inject
    private FirebaseAnalyticsProvider firebaseAnalyticsProvider = FirebaseAnalyticsProvider.instance();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        firebaseAnalyticsProvider.logCurrentScreen(activity, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "Fragment destroyed");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
        firebaseAnalyticsProvider.setCurrentScreen(activity, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
    }

    @Override
    public void onViewCreated() {
        try {
            final View account_cdo_link = container.findViewById(R.id.account_cdo_link);
            final View account_cdo_info = container.findViewById(R.id.account_cdo_info);
            if (account_cdo_link != null) {
                account_cdo_link.setOnClickListener(v -> {
                    Log.v(TAG, "account_cdo_link clicked");
                    try {
                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://de.ifmo.ru")));
                    } catch (Exception e) {
                        BottomBar.snackBar(activity, activity.getString(R.string.something_went_wrong));
                    }
                });
            }
            Thread.run(() -> {
                final String cdo_user_info = storage.get(activity, Storage.PERMANENT, Storage.USER, "user#deifmo#login", "").trim() + " (" + storage.get(activity, Storage.PERMANENT, Storage.USER, "user#name", "").trim() + ")";
                Thread.runOnUI(() -> {
                    if (account_cdo_info != null) {
                        ((TextView) account_cdo_info).setText(cdo_user_info);
                    }
                });
            });
        } catch (Exception e) {
            Log.exception(e);
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_linked_accounts;
    }

    @Override
    protected int getRootId() {
        return 0;
    }
}
