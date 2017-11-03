package com.bukhmastov.cdoitmo.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.utils.Log;
import com.bukhmastov.cdoitmo.utils.Static;
import com.bukhmastov.cdoitmo.utils.Storage;

public class LinkedAccountsFragment extends ConnectedFragment {

    private static final String TAG = "LinkedAccountsFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "Fragment created");
        FirebaseAnalyticsProvider.logCurrentScreen(activity, this);
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
        FirebaseAnalyticsProvider.setCurrentScreen(activity, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_linked_accounts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        try {
            final View account_cdo_link = view.findViewById(R.id.account_cdo_link);
            final View account_cdo_info = view.findViewById(R.id.account_cdo_info);
            if (account_cdo_link != null) {
                account_cdo_link.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.v(TAG, "account_cdo_link clicked");
                        try {
                            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://de.ifmo.ru")));
                        } catch (Exception e) {
                            Static.snackBar(activity, activity.getString(R.string.something_went_wrong));
                        }
                    }
                });
            }
            Static.T.runThread(new Runnable() {
                @Override
                public void run() {
                    final String cdo_user_info = Storage.file.perm.get(activity, "user#deifmo#login", "").trim() + " (" + Storage.file.perm.get(activity, "user#name", "").trim() + ")";
                    Static.T.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (account_cdo_info != null) {
                                ((TextView) account_cdo_info).setText(cdo_user_info);
                            }
                        }
                    });
                }
            });

        } catch (Exception e) {
            Static.error(e);
        }
    }
}
